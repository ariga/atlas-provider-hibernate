package io.atlasgo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import jakarta.persistence.GenerationType
import jakarta.persistence.SequenceGenerator
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.archive.scan.spi.ScanEnvironment
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter
import org.hibernate.boot.model.IdentifierGeneratorDefinition
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService
import org.hibernate.cfg.AvailableSettings
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.IdentifierGenerator
import org.hibernate.service.ServiceRegistry
import org.hibernate.service.spi.ServiceRegistryAwareService
import org.hibernate.service.spi.ServiceRegistryImplementor
import org.hibernate.tool.schema.Action
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool
import org.hibernate.tool.schema.internal.exec.GenerationTarget
import org.hibernate.tool.schema.spi.SchemaManagementTool
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator
import org.hibernate.type.Type
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

private const val linkToGuide = "https://atlasgo.io/guides/orms/hibernate"

class ConsoleGenerationTarget(
        private val writer: OutputStream = System.out,
        private val enableTableGenerators: Boolean = false) : GenerationTarget {
    override fun prepare() {}
    override fun accept(command: String) {
        if (!enableTableGenerators && isUnsupportedCommand(command)) {
            throw UnsupportedGenerationType("unsupported SQL command '$command', data dependent generation is not supported. See $linkToGuide")
        }
        writer.write("$command;\n".toByteArray())
    }

    override fun release() {}

    private fun isUnsupportedCommand(command: String): Boolean {
        return command.startsWith("insert into") || command.startsWith("create sequence")
    }
}

class ConsoleSchemaManagementTool(
        private val tool: HibernateSchemaManagementTool = HibernateSchemaManagementTool(),
        enableTableGenerators: Boolean = false): SchemaManagementTool by tool, ServiceRegistryAwareService {
    init {
        setCustomDatabaseGenerationTarget(ConsoleGenerationTarget(enableTableGenerators = enableTableGenerators))
    }

    override fun injectServices(serviceRegistry: ServiceRegistryImplementor) {
        tool.injectServices(serviceRegistry)
    }
}

class ScanEnvironmentImpl(
    private val packages: List<URL>,
    private val classes: List<String>) : ScanEnvironment {
    override fun getRootUrl(): URL? {
        return null
    }
    override fun getNonRootUrls(): List<URL> {
        return packages.toList()
    }
    override fun getExplicitlyListedClassNames(): List<String> {
        return classes
    }
    override fun getExplicitlyListedMappingFiles(): List<String> {
        return emptyList()
    }
}

class PrintSchemaCommand: CliktCommand() {
    private val packagesOpt by option("--packages", help="list of package URLs, base64 encoded")
        .split(",")
        .default(emptyList())
    private val metadataBuilderClass by option("--metadata-builder", help="class name to build Hibernate Metadata, must be in classpath")
        .default("")
    private val registryBuilderClass by option("--registry-builder", help="class name to build Hibernate ServiceRegistry, must be in classpath")
    private val properties by option("--properties", help = "Resource URL to a properties file, must to be in classpath")
        .default("")
    private val enableTableGenerator by option("--enable-table-generators", help = "Enables some GenerationType (primarily SEQUENCE and TABLE) which are disabled by default")
        .flag("--disable-table-generators")

    var exitOnUnsupportedGenerationType = true

    override fun run() {
        UnsupportedGenerationType.thrownExceptions.clear()
        if (metadataBuilderClass.isNotBlank() && (classes.isNotEmpty() || packages.isNotEmpty())) {
            throw RuntimeException("--metadata-builder is mutually exclusive with --packages and --classes")
        }
        Logger.getLogger("").level = Level.OFF
        Logger.getLogger("org.hibernate").level = Level.OFF
        val settings = Properties()
        if (properties.isNotBlank()) {
            Thread.currentThread().contextClassLoader.getResourceAsStream(properties)?.let {
                settings.load(it)
            } ?: throw RuntimeException("Unable to load properties file '$properties', is it in the classpath?")
        }
        mapOf(
            "hibernate.boot.allow_jdbc_metadata_access" to false,
            "hibernate.temp.use_jdbc_metadata_defaults" to false, // deprecated from hibernate 6.5
            AvailableSettings.SCHEMA_MANAGEMENT_TOOL to ConsoleSchemaManagementTool(HibernateSchemaManagementTool(), enableTableGenerator),
            AvailableSettings.CONNECTION_PROVIDER to UserSuppliedConnectionProviderImpl(),
        ).forEach {
            if (!settings.containsKey(it.key)) {
                settings.put(it.key, it.value)
            }
        }
        val registry = registryBuilderClass.takeUnless { it.isNullOrBlank() }?.let {
            instantiateClass<Function<Properties, ServiceRegistry>>(it).apply(settings)
        } ?: run {
            StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build()
        }
        try {
            val metadata = metadataBuilderClass.takeIf { it.isNotBlank() }?.let {
                val loader = registry.getService(ClassLoaderService::class.java)
                    ?: throw Exception("Missing ClassLoaderService in registry")
                loader.classForName<Function<ServiceRegistry, Metadata>>(it)
                    .getConstructor().newInstance().apply(registry)
            } ?: run {
                MetadataSources(registry)
                    .metadataBuilder
                    .applyScanEnvironment(ScanEnvironmentImpl(packages, classes)).also {
                        try {
                            if (!enableTableGenerator && this::class.java.classLoader.loadClass(
                                    IdGeneratorStrategyInterpreter::class.java.name
                                ) != null
                            ) {
                                it.applyIdGenerationTypeInterpreter(DisabledIdGeneratorStrategyInterpreter())
                            }
                        } catch (e: Throwable) {
                            // ignoring the error, but not disabling generators
                        }
                    }
                    .build()
            }
            val toolSettings = mapOf<String, Any>(
                AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION to Action.CREATE_ONLY,
                AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION to Action.NONE,
                AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS to true
            )
            SchemaManagementToolCoordinator.process(
                metadata,
                registry,
                toolSettings
            ) { }
        }
        finally {
            if (UnsupportedGenerationType.thrownExceptions.isNotEmpty()) {
                UnsupportedGenerationType.thrownExceptions.forEach {
                    System.err.println(it.message)
                }
                // Not exiting in cases when we don't want to kill the JVM, for example - during testing
                if (exitOnUnsupportedGenerationType) {
                    exitProcess(3)
                } else {
                    throw UnsupportedGenerationType.thrownExceptions.first
                }
            }
        }
    }

    private fun <T> instantiateClass(name: String): T {
        @Suppress("UNCHECKED_CAST")
        return Thread.currentThread().contextClassLoader.loadClass(name).getConstructor().newInstance() as T
    }

    private val packages get() = packagesOpt
        .filter { it.isNotEmpty() }
        .map {
            URI(it.decodeBase64()).toURL()
        }
    private val classes by option("--classes", help="list of class names to be included")
        .split(",")
        .default(emptyList())
}

fun String.toBase64(): String = Base64.getEncoder().encodeToString(this.encodeToByteArray())
fun String.decodeBase64(): String = Base64.getDecoder().decode(this).decodeToString()

@Suppress("DEPRECATION")
class DisabledIdGeneratorStrategyInterpreter : IdGeneratorStrategyInterpreter {
    override fun determineGeneratorName(generationType: GenerationType, context: IdGeneratorStrategyInterpreter.GeneratorNameDeterminationContext): String? {
        // The following types are disabled because they rely on additional tables that need to be initialized.
        // This is currently not supported by Atlas.
        return when (generationType) {
            GenerationType.SEQUENCE -> DisabledIdGenerator.DisabledSequenceIdGenerator::class.java.name
            GenerationType.TABLE -> DisabledIdGenerator.DisabledTableIdGenerator::class.java.name
            GenerationType.AUTO -> DisabledIdGenerator.DisabledAutoIdGenerator::class.java.name
            else -> null
        }
    }

    override fun interpretTableGenerator(
        tableGeneratorAnnotation: jakarta.persistence.TableGenerator,
        definitionBuilder: IdentifierGeneratorDefinition.Builder
    ) {
        throw UnsupportedGenerationType("unsupported TableGenerator found for '${definitionBuilder.name}. See $linkToGuide")
    }

    override fun interpretSequenceGenerator(
        sequenceGeneratorAnnotation: SequenceGenerator,
        definitionBuilder: IdentifierGeneratorDefinition.Builder
    ) {
        throw UnsupportedGenerationType("unsupported SequenceGenerator found for '${definitionBuilder.name}. See $linkToGuide")
    }
}

open class DisabledIdGenerator (val generationType: GenerationType): IdentifierGenerator {
    class DisabledSequenceIdGenerator: DisabledIdGenerator(GenerationType.SEQUENCE)
    class DisabledTableIdGenerator: DisabledIdGenerator(GenerationType.TABLE)
    class DisabledAutoIdGenerator: DisabledIdGenerator(GenerationType.AUTO)

    override fun configure(type: Type, params: Properties, serviceRegistry: ServiceRegistry) {
        throw UnsupportedGenerationType("unsupported GenerationType.$generationType found for entity '${params["entity_name"]}. See $linkToGuide")
    }

    override fun generate(session: SharedSessionContractImplementor?, `object`: Any?): Any {
        throw UnsupportedGenerationType("GenerationType.$generationType is unsupported. See $linkToGuide")
    }
}

class UnsupportedGenerationType(message: String): RuntimeException(message) {
    init {
        thrownExceptions.add(this)
    }

    companion object {
        // Hibernate ignores these exceptions when thrown in a specific context, to bypass that, we save them aside
        // and check manually if they were thrown
        val thrownExceptions = ConcurrentLinkedDeque<UnsupportedGenerationType>()
    }
}
