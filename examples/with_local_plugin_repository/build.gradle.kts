
plugins {
    application
    id("io.atlasgo.hibernate-provider") version "0.1" apply true
}

group = "org.example"
version = "1.0"

//buildscript {
//    repositories {
//        mavenCentral()
//        mavenLocal {
//            url = uri("../../.local-plugin-repository")
//        }
//    }
//    dependencies {
////        classpath("io.atlasgo:hibernate-provider")
//    }
//}

repositories {
    mavenCentral()
//    mavenLocal {
//        url = uri("../../.local-plugin-repository")
//    }
}

//tasks.withType<SchemaTask> {
//
//}

dependencies {
//    atlas(gradleApi())
    implementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
}

//configurations.findByName("atlas")?.let { conf ->
//    println("DORAV - got atlas config")
//    conf.allDependencies.forEach { dep ->
//        println("Dependency = $dep")
//    }
//} ?: run {
//    println("DORAV - unable to find config")
//}