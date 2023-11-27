import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.20"
}

group = "io.atlasgo"
version = "0.1"

repositories {
    mavenCentral()
}

tasks.withType<ShadowJar> {
    archiveClassifier = null
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

gradlePlugin {
    plugins {
        create("io.atlasgo.hibernate-provider") {
            website = "https://github.com/ariga/atlas-provider-hibernate"
            vcsUrl = "https://github.com/ariga/atlas-provider-hibernate.git"
            description = "Atlas plugin, used as a database schema provider to Atlas."
            id = "io.atlasgo.hibernate-provider"
            implementationClass = "io.atlasgo.HibernateProvider"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "localPluginRepository"
            url = uri(".local-plugin-repository")
        }
    }
}

dependencies {
    compileOnly("org.hibernate.orm:hibernate-core:6.1.7.Final")
    implementation(gradleApi())
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    runtimeOnly(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}
