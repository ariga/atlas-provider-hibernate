import io.atlasgo.gradle.SchemaTask

plugins {
    application
    id("io.atlasgo.hibernate-provider-gradle-plugin") version "0.0.0-SNAPSHOT"
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.1.7.Final")
    runtimeOnly("com.h2database:h2:2.2.224")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
}

val testTasks = mapOf(
    "expected_minimal_schema" to
            tasks.create<SchemaTask>("minimal-schema") {
                packages = listOf("org.example.minimodel")
            },
    "h2_schema" to
            tasks.create<SchemaTask>("custom-registry-schema") {
                registryBuilderClass = "org.example.H2ServiceRegistryBuilder"
            },
    "event_only_schema" to
            tasks.create<SchemaTask>("custom-metadata-schema") {
                metadataBuilderClass = "org.example.EventOnlyMetadataBuilder"
            },
    "event2_with_location_schema" to
            tasks.create<SchemaTask>("with-explicit-class-schema") {
                packages = listOf("io.matilda.model")
                classes = listOf("org.example.minimodel.Location")
            },
    "h2_schema" to
            tasks.create<SchemaTask>("schema-with-h2-properties") {
                propertiesFile = "h2-hibernate.properties"
            },
    "expected_default_schema" to
            tasks.getByName<SchemaTask>("schema"))

val integrationTest = tasks.register("integrationTests") {
    dependsOn += "build"
    doLast {
        testTasks.forEach { (testFile, schemaTask) ->
            println("Running task ${schemaTask.name}")
            var output = ""
            schemaTask.logging.addStandardOutputListener {
                output += it
            }
            schemaTask.logging.captureStandardOutput(null)
            schemaTask.standardOutputCapture.start()
            schemaTask.exec()
            schemaTask.standardOutputCapture.stop()
            
            // Normalize the output by converting absolute paths to relative paths
            val normalizedOutput = normalizePathsInOutput(output)
            
            val expectedOutput = File("src/test/$testFile").readText()
            if (normalizedOutput != expectedOutput) {
                println("""
                    output for task ${schemaTask.name} was not as expected (file: $testFile):
                    ----
                    $expectedOutput
                    ----
                    actual output:
                    ----
                    $normalizedOutput
                    ----
                """.trimIndent())
                throw RuntimeException("Unexpected output for task ${schemaTask.name}")
            }
        }
    }
}

fun normalizePathsInOutput(output: String): String {
    val projectDir = project.projectDir.absolutePath
    return output.replace(projectDir + File.separator, "")
}

