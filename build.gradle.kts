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

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

val gradleConf by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

val mavenConf by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

val maven by sourceSets.creating {
    compileClasspath += mavenConf + sourceSets.main.get().output
    runtimeClasspath += mavenConf + sourceSets.main.get().output
}

val gradle by sourceSets.creating {
    compileClasspath += gradleConf + sourceSets.main.get().output
    runtimeClasspath += gradleConf + sourceSets.main.get().output
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = null
    from(gradle.output)
}

gradlePlugin {
    pluginSourceSet(gradle)
    plugins {
        create("hibernate-provider-plugin") {
            website = "https://github.com/ariga/atlas-provider-hibernate"
            vcsUrl = "https://github.com/ariga/atlas-provider-hibernate.git"
            description = "Atlas plugin, used as a database schema provider to Atlas."
            displayName = "Atlas Hibernate Provider"
            tags = listOf("database", "hibernate", "atlas", "migrations", "schema")
            id = "io.atlasgo.hibernate-provider"
            implementationClass = "io.atlasgo.gradle.HibernateProvider"
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
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    runtimeOnly(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    testImplementation("com.h2database:h2:2.2.224")

    mavenConf("org.codehaus.mojo:exec-maven-plugin:3.1.1")
    mavenConf("org.apache.maven:maven-plugin-api:3.6.3")
    mavenConf("org.apache.maven:maven-project:2.2.1")
    mavenConf("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
    gradleConf(gradleApi())
}

tasks.test {
    useJUnitPlatform()
}
