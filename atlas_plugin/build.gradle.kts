import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    `java-library`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.20"
}

group = "org.ariga"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_14
    targetCompatibility = JavaVersion.VERSION_14
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_14
    }
}

gradlePlugin {
    plugins {
        create("atlashibernate") {
            id = "org.ariga.atlashibernate"
            implementationClass = "org.ariga.AtlasHibernate"
        }
    }
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
//    implementation("org.hibernate.orm:hibernate-core:6.1.7.Final")
    implementation(gradleApi())
}