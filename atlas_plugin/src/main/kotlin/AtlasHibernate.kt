package org.ariga

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.initialization.dsl.ScriptHandler.CLASSPATH_CONFIGURATION
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.archive.scan.spi.ScanEnvironment
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl
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
import java.net.URL
import java.net.URLClassLoader

class ConsoleGenerationTarget : GenerationTarget {
    override fun prepare() {}
    override fun accept(command: String) {
        println(command)
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

interface MetadataBuilder {
    fun build(registry: ServiceRegistry): Metadata
}

abstract class SchemaTask : DefaultTask() {

    @Input
    @Option(option = "dialect", description = "")
    open var dialect: String = ""

    @Input
    @Option(option = "metadata-builder", description = "")
    open var metadataBuilderClass: String = ""

    @get:InputFiles
    @get:Classpath
    abstract val classPath: ConfigurableFileCollection?

    @get:InputFiles
    @get:Classpath
    abstract val scannedClasspath: ConfigurableFileCollection?

    @Input
    open var packageFilter: List<String> = emptyList()
        set(newList) {
            field = newList.map { it.replace(".", "/") }
        }

    companion object {
        fun defaultRegistry(classLoader: ClassLoader): BootstrapServiceRegistry {
            return BootstrapServiceRegistryBuilder()
                .applyClassLoaderService(ClassLoaderServiceImpl(classLoader))
                .build()
        }

        fun printSchema(
            bootstrapRegistry: BootstrapServiceRegistry,
            rootPackage: List<URL>,
            metadataBuilderClass: String = "",
            options: Map<String, Any> = emptyMap()
        ) {
            val settings = mapOf(
                "hibernate.temp.use_jdbc_metadata_defaults" to false,
                AvailableSettings.SCHEMA_MANAGEMENT_TOOL to ConsoleSchemaManagementTool(),
                AvailableSettings.CONNECTION_PROVIDER to UserSuppliedConnectionProviderImpl()
            ) + options

            val registry = StandardServiceRegistryBuilder(bootstrapRegistry)
                .applySettings(settings)
                .build()

            val metadata = if (metadataBuilderClass.isNotEmpty()) {
                val metadataBuilderClazz = bootstrapRegistry.getService(ClassLoaderService::class.java)
                    ?.classForName<MetadataBuilder>(metadataBuilderClass)
                metadataBuilderClazz?.getConstructor()?.newInstance()?.build(registry)
            } else {
                MetadataSources(registry)
                    .getMetadataBuilder(registry)
                    .applyScanEnvironment(object : ScanEnvironment {
                        override fun getRootUrl(): URL? {
                            return null
                        }

                        override fun getNonRootUrls(): List<URL> {
                            return rootPackage
                        }

                        override fun getExplicitlyListedClassNames(): List<String> {
                            return emptyList()
                        }

                        override fun getExplicitlyListedMappingFiles(): List<String> {
                            return emptyList()
                        }

                    })
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
    }

    @TaskAction
    fun run() {
        val scannedClasspath = this.scannedClasspath?.filter { f ->
            packageFilter.isEmpty() || packageFilter.any { packageName ->
                f.endsWith(packageName)
            }
        }?.map { it.toURI().toURL() } ?: emptyList()
        val urls = classPath?.asURLs() ?: emptyList()
        val classLoader = URLClassLoader.newInstance(urls.toTypedArray(), this.javaClass.classLoader)
        Thread.currentThread().contextClassLoader = classLoader
        printSchema(
            defaultRegistry(this.javaClass.classLoader),
            scannedClasspath,
            metadataBuilderClass,
            mapOf(
//                "jakarta.persistence.database-product-name" to dialect,
//                "jakarta.persistence.database-major-version" to 8,
//                "jakarta.persistence.database-minor-version" to 0,
            )
        )
    }
}

private fun ConfigurableFileCollection.asURLs(): List<URL> = this.map { it.toURI().toURL() }

class AtlasHibernate : Plugin<Project> {
    override fun apply(project: Project) {
        project.buildscript.apply {
            dependencies.add(CLASSPATH_CONFIGURATION, "org.ariga:atlashibernate")
        }
        project.tasks.register("schema", SchemaTask::class.java) {
            it.dependsOn("compileJava")
            it.group = "Atlas"
            it.description = "This is a description"
            val javaPlugin = project.extensions.getByType(JavaPluginExtension::class.java)
            val mainSourceSet = javaPlugin.sourceSets.named("main").get()
            it.classPath?.setFrom(mainSourceSet.runtimeClasspath + mainSourceSet.resources)
            it.scannedClasspath?.setFrom(mainSourceSet.output.classesDirs.asFileTree
                .map { it.toPath().parent }.toSet()
            )
        }
    }
}