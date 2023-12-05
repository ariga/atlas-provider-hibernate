package io.atlasgo

import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

@Mojo(name = "schema", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
internal class ExportSchemaMojo : AbstractMojo() {
    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    private lateinit var project: MavenProject

    @Parameter(property = "classpathScope", defaultValue = "runtime")
    protected var classpathScope: String? = null

    @Parameter(property = "properties")
    private val properties = ""

    @Parameter(property = "packages")
    private var packages = emptyList<String>()

    @Parameter(property = "classes")
    private var classes = emptyList<String>()

    @Parameter(property = "registry-builder")
    private var registryBuilderClass = ""

    @Parameter(property = "metadata-builder")
    private var metadataBuilderClass = ""

    @Parameter(property = "debug")
    private var debugEnabled = false

    override fun execute() {
        val args = mutableListOf("java")
        if (debugEnabled) {
            args += listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        }
        args += listOf("-cp", buildClasspath())
        args += ExportSchemaMain::class.java.name
        if (registryBuilderClass.isNotBlank()) {
            args += listOf("--registry-builder", registryBuilderClass)
        }
        if (properties.isNotBlank()) {
            args += listOf("--properties", properties)
        }
        if (metadataBuilderClass.isEmpty()) {
            if (classes.isNotEmpty()) {
                args += listOf("--classes", classes.joinToString(","))
            }
            if (scannedClasspath.isNotEmpty()) {
                args += listOf("--packages", scannedClasspath.joinToString(","))
            }
        } else {
            args += listOf("--metadata-builder", metadataBuilderClass)
        }
        val exitCode = ProcessBuilder()
            .command(args)
            .redirectOutput(Redirect.INHERIT)
            .redirectError(Redirect.INHERIT)
            .start()
            .waitFor()
        if (exitCode != 0) {
            exitProcess(exitCode)
        }
    }

    private val scannedClasspath: List<String> by lazy {
        val scanBase = File(project.build.outputDirectory)
        val packageDirs = packages.map { it.replace(".", "/" )}
        scanBase.walkBottomUp().filter {
            it.isDirectory
        }.filter { dir ->
            packageDirs.isEmpty() || packageDirs.any { pack ->
                dir.relativeTo(scanBase).startsWith(pack)
            }
        }.map {
            it.toURI().toString().toBase64()
        }.toList()
    }

    private fun buildClasspath(): String {
        val path = mutableListOf<Path>()
        path.add(File(this::class.java.protectionDomain.codeSource.location.file).toPath())
        path += project.build.resources.map {
            Paths.get(it.directory)
        }
        val artifacts = mutableListOf<Artifact>()
        when (classpathScope) {
            "compile" -> {
                artifacts += project.compileArtifacts as List<Artifact>
                path.add(Paths.get(project.build.outputDirectory))
            }
            "test" -> {
                artifacts += project.testArtifacts as List<Artifact>
                path.add(Paths.get(project.build.testOutputDirectory))
                path.add(Paths.get(project.build.outputDirectory))
            }
            "runtime" -> {
                artifacts += project.runtimeArtifacts as List<Artifact>
                path.add(Paths.get(project.build.outputDirectory))
            }
            "system" -> {
                artifacts += project.systemArtifacts as List<Artifact>
            }
            else -> {
                throw IllegalStateException("Invalid classpath scope: $classpathScope")
            }
        }
        path += artifacts.map {
            it.file.toPath()
        }
        return path.joinToString(":")
    }
}

// Defining the main in a different class helps us avoid adding maven dependencies to the classpath
class ExportSchemaMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = PrintSchemaCommand().main(args)
    }
}
