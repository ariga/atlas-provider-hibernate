plugins {
    kotlin("jvm") version "1.9.20"
    application
    id("io.atlasgo.hibernate") version "0.1" apply true
}

kotlin {
    jvmToolchain(11)
}

group = "org.example"
version = "1.0"

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
}

kotlin {
    jvmToolchain(14)
}
