import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    `java-gradle-plugin`
    application
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
        create("atlashibernate") {
            description = "Atlas Hibernate plugin is used as an Atlas schema provider for Hibernate."
            id = "io.ariga.atlashibernate"
            implementationClass = "io.ariga.AtlasHibernate"
        }
    }
}

//publishing {
//    repositories {
//        maven {
//            name = "localPluginRepository"
//
//            url = uri("../local-plugin-repository")
//        }
//    }
//}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("org.hibernate.orm:hibernate-core:6.1.7.Final")
    implementation(gradleApi())
}

tasks.withType<ProcessResources>().all { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.withType<Copy>().all { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }

//application {
//    mainClass = "io.ariga.MainKt"
//}
//
//tasks.jar {
//    isZip64 = true
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    from(configurations.runtimeClasspath.get().files.map {
//        if (it.isDirectory()) it else zipTree(it)
//    })

//    manifest {
//        attributes["Main-Class"] = "io.ariga.MainKt"
//}
//}