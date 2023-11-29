package io.atlasgo.gradle_plugin

import io.atlasgo.PrintSchemaCommand
import io.atlasgo.toBase64
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
    @Option(option = "packages", description = "Optional list of package names that the scanned will use to detect entities. By default scans the entire classpath")
    open var packages: List<String> = emptyList()
        set(newList) {
            field = newList.map { it.replace(".", "/") }
        }

    @Input
    @Option(option = "classes", description = "Optional list of classnames that will be explicitly added as entities (even if the class does not exist in 'packages'). Class must be available in the classpath.")
    open var classes: List<String> = emptyList()

    @Input
    @Option(option = "properties", description = "Optional properties file name, must be in classpath")
    open var propertiesFile: String = ""

    init {
        project.javaPlugin?.let { javaPlugin ->
            classpath = project.files(
                this::class.java.protectionDomain.codeSource.location,
                javaPlugin.mainSourceSet?.output,
                javaPlugin.mainSourceSet?.output?.resourcesDir,
                project.configurations.named("runtimeClasspath").get().files)
        }
        mainClass.set(HibernateProvider::class.java.canonicalName)
    }

    @TaskAction
    override fun exec() {
        val args = mutableListOf<String>()
        if (registryBuilderClass.isNotEmpty()) {
            args += listOf("--registry-builder", registryBuilderClass)
        }
        if (metadataBuilderClass.isEmpty()) {
            if (classes.isNotEmpty()) {
                args += listOf("--classes", classes.joinToString(","))
            }
            val scannedClasspath = this.scannedClasspath.asFileTree
                .filter { f ->
                    packages.isEmpty() || packages.any { packageName ->
                        f.toPath().parent.endsWith(packageName)
                    }
                }
                .map { it.toPath().parent.toUri() }
                .toSet()
            if (scannedClasspath.isNotEmpty()) {
                args += listOf("--packages", scannedClasspath.map {
                    it.toString().toBase64()
                }.joinToString(","))
            }
        } else {
            args += listOf("--metadata-builder", metadataBuilderClass)
        }
        if (propertiesFile.isNotEmpty()) {
            args += listOf("--properties", propertiesFile)
        }
        this.args = args
        super.exec()
    }
}

class HibernateProvider : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("schema", SchemaTask::class.java) {
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


private val JavaPluginExtension.mainSourceSet: SourceSet?
    get() = sourceSets.named(MAIN_SOURCE_SET_NAME).get()

private val Project.javaPlugin
    get() = extensions.findByType(JavaPluginExtension::class.java)
