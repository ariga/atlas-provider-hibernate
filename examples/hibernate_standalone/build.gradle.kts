plugins {
    kotlin("jvm") version "1.9.20"
    application
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
}


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
}

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources", "src/main/configs")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<ProcessResources>().all { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.withType<Copy>().all { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }

//application {
//    mainClass.set("Main")
//}

//tasks.named<SchemaTask>("schema") {
//    dialect = "MySQL"
//    packageFilter = listOf("org.example.model")
//    metadataBuilderClass = "org.example.MetadataBuilderImpl"
//}
//
//tasks.create("mini-schema", SchemaTask::class.java) {
//    dialect = "MySQL"
//}

//tasks.named<SchemaTask>("schema") {
//
//}