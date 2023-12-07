import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm")
}

group = "io.atlasgo"
version = "0.1"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation(project(":provider"))

    implementation("org.codehaus.mojo:exec-maven-plugin:3.1.1")
    implementation("org.apache.maven:maven-plugin-api:3.6.3")
    implementation("org.apache.maven:maven-project:2.2.1")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
}
