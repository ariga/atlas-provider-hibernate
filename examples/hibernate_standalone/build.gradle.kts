import org.ariga.SchemaTask

plugins {
    kotlin("jvm") version "1.9.20"
    application
    id("org.ariga.atlashibernate") apply true
}

kotlin {
    jvmToolchain(11)
}

group = "org.example"
version = "1.0-SNAPSHOT"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ariga:atlashibernate")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    implementation("org.ariga:atlashibernate")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
}

kotlin {
    jvmToolchain(17)
}

tasks.create("other-schema", SchemaTask::class.java) {
    dialect = "MySQL"
    packageFilter = listOf("org.example.model")
    metadataBuilderClass = "org.example.MetadataBuilderImpl"
}