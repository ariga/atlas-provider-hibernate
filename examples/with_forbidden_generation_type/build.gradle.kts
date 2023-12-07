import io.atlasgo.gradle.SchemaTask

plugins {
    kotlin("jvm") version "1.9.21"
    id("io.atlasgo.hibernate-provider-gradle-plugin") version "0.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.4.0.Final")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

tasks.create<SchemaTask>("schema-forbidden") {
    packages = listOf("com.example.forbidden_model")
}
