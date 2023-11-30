package io.atlasgo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.archive.scan.spi.ScanEnvironment
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService
import org.hibernate.cfg.AvailableSettings
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
import org.hibernate.service.ServiceRegistry
import org.hibernate.service.spi.ServiceRegistryAwareService
import org.hibernate.service.spi.ServiceRegistryImplementor
import org.hibernate.tool.schema.Action
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool
import org.hibernate.tool.schema.internal.exec.GenerationTarget
import org.hibernate.tool.schema.spi.SchemaManagementTool
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.util.*
import java.util.function.Function
import java.util.logging.Level

class ConsoleGenerationTarget(private val writer: OutputStream = System.out) : GenerationTarget {
    override fun prepare() {}
    override fun accept(command: String) {
        writer.write("$command;\n".toByteArray())
    }
    override fun release() {}
}

class ConsoleSchemaManagementTool(private val tool: HibernateSchemaManagementTool): SchemaManagementTool by tool,
    ServiceRegistryAwareService {
    constructor(): this(HibernateSchemaManagementTool()) {
        setCustomDatabaseGenerationTarget(ConsoleGenerationTarget())
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

    override fun run() {
        if (metadataBuilderClass.isNotBlank() && (classes.isNotEmpty() || packages.isNotEmpty())) {
            throw RuntimeException("--metadata-builder is mutually exclusive with --packages and --classes")
        }
        java.util.logging.Logger.getLogger("").level = Level.OFF
        java.util.logging.Logger.getLogger("org.hibernate").level = Level.OFF
        val settings = Properties()
        if (properties.isNotBlank()) {
            Thread.currentThread().contextClassLoader.getResourceAsStream(properties)?.let {
                settings.load(it)
            } ?: throw RuntimeException("Unable to load properties file '$properties', is it in the classpath?")
        }
        mapOf(
            "hibernate.temp.use_jdbc_metadata_defaults" to false,
            AvailableSettings.SCHEMA_MANAGEMENT_TOOL to ConsoleSchemaManagementTool(),
            AvailableSettings.CONNECTION_PROVIDER to UserSuppliedConnectionProviderImpl()
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
        val metadata = metadataBuilderClass.takeIf { it.isNotBlank() }?.let {
            val loader = registry.getService(ClassLoaderService::class.java)
                ?: throw Exception("Missing ClassLoaderService in registry")
            loader.classForName<Function<ServiceRegistry, Metadata>>(it)
                .getConstructor().newInstance().apply(registry)
        } ?: run {
            MetadataSources(registry)
                .metadataBuilder
                .applyScanEnvironment(ScanEnvironmentImpl(packages, classes))
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
