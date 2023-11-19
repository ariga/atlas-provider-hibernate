package io.atlasgo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.archive.scan.spi.ScanEnvironment
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService
import org.hibernate.cfg.AvailableSettings
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
import org.hibernate.internal.util.PropertiesHelper
import org.hibernate.service.ServiceRegistry
import org.hibernate.service.spi.ServiceRegistryAwareService
import org.hibernate.service.spi.ServiceRegistryImplementor
import org.hibernate.tool.schema.Action
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool
import org.hibernate.tool.schema.internal.exec.GenerationTarget
import org.hibernate.tool.schema.spi.SchemaManagementTool
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator
import java.net.URI
import java.net.URL
import java.util.*
import java.util.function.Function
import java.util.logging.Level

class ConsoleGenerationTarget : GenerationTarget {
    override fun prepare() {}
    override fun accept(command: String) {
        println("$command;")
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
        java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);
        val settings = Properties().also { p ->
            p.putAll(mapOf(
                "hibernate.temp.use_jdbc_metadata_defaults" to false,
                AvailableSettings.SCHEMA_MANAGEMENT_TOOL to ConsoleSchemaManagementTool(),
                AvailableSettings.CONNECTION_PROVIDER to UserSuppliedConnectionProviderImpl()
            ))
            Thread.currentThread().contextClassLoader.getResourceAsStream(properties)?.let {
                p.load(it)
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
            PropertiesHelper.map(settings) + toolSettings
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

abstract class SchemaTask : JavaExec() {
    @Input
    @Option(option = "registry-builder", description = "Optional class name that implements Function<Properties, ServiceRegistry>, must have an empty constructor. Used as a provider of the Hibernate ServiceRegistry object if exists")
    open var registryBuilderClass: String = ""

    @Input
    @Option(option = "metadata-builder", description = "Optional class name that implements Function<ServiceRegistry, Metadata>, must have an empty constructor. Used as a provider of the Hibernate Metadata object if exists")
    open var metadataBuilderClass: String = ""

    @get:InputFiles
    @get:Classpath
    open val scannedClasspath: ConfigurableFileCollection = run {
        project.files(project.javaPlugin?.mainSourceSet?.output)
    }

    @Input
    open var packages: List<String> = emptyList()
        set(newList) {
            field = newList.map { it.replace(".", "/") }
        }

    @Input
    open var classes: List<String> = emptyList()

    @TaskAction
    override fun exec() {
        val scannedClasspath = this.scannedClasspath.asFileTree
            .filter { f ->
                packages.isEmpty() || packages.any { packageName ->
                    f.toPath().parent.endsWith(packageName)
                }
            }
            .map { it.toPath().parent.toUri() }
            .toSet()
        val args = mutableListOf<String>()
        if (scannedClasspath.isNotEmpty()) {
            args += listOf("--packages", scannedClasspath.map {
                it.toString().toBase64()
            }.joinToString(","))
        }

        if (classes.isNotEmpty()) {
            args += listOf("--classes", classes.joinToString(","))
        }

        if (registryBuilderClass.isNotEmpty()) {
            args += listOf("--registry-builder", registryBuilderClass)
        }

        if (metadataBuilderClass.isNotEmpty()) {
            args += listOf("--metadata-builder", metadataBuilderClass)
        }
        this.args = args
        super.exec()
    }
}

class HibernateProvider : Plugin<Project> {
    override fun apply(project: Project) {
        val atlasConfig = project.configurations.create("atlas") {
            it.extendsFrom(project.configurations.named("runtimeClasspath").get())
        }
        project.dependencies.add(atlasConfig.name, "io.ariga:atlashibernate")
        project.tasks.register("schema", SchemaTask::class.java) {
            project.javaPlugin?.let { javaPlugin ->
                it.classpath = project.files(
                    javaPlugin.mainSourceSet?.output,
                    javaPlugin.mainSourceSet?.output?.resourcesDir,
                    atlasConfig)
                it.mainClass.set(HibernateProvider::class.java.canonicalName)
            }
            it.dependsOn("compileJava", "processResources")
            it.group = "Atlas"
            it.description = "Prints Hibernate schema to be used as Atlas schema provider"
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = PrintSchemaCommand().main(args)
    }
}

private fun String.toBase64() = Base64.getEncoder().encodeToString(this.encodeToByteArray())
private fun String.decodeBase64(): String = Base64.getDecoder().decode(this).decodeToString()

private val JavaPluginExtension.mainSourceSet: SourceSet?
    get() = sourceSets.named(MAIN_SOURCE_SET_NAME).get()

private val Project.javaPlugin
    get() = extensions.findByType(JavaPluginExtension::class.java)