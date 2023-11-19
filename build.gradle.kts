import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.20"
}

group = "io.ariga"
version = "0.1"

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
        create("atlash") {
            description = "Atlas plugin, used as a database schema provider to Atlas."
            id = "io.ariga.atlas"
            implementationClass = "io.ariga.HibernateProvider"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("org.hibernate.orm:hibernate-core:6.1.7.Final")
    implementation(gradleApi())
}
