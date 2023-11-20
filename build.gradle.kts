import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    `java-gradle-plugin`
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

gradlePlugin {
    plugins {
        create("io.atlasgo.hibernate-provider") {
            description = "Atlas plugin, used as a database schema provider to Atlas."
            id = "io.atlasgo.hibernate-provider"
            implementationClass = "io.atlasgo.HibernateProvider"
        }
    }
}

dependencies {
    compileOnly("org.hibernate.orm:hibernate-core:6.1.7.Final")
    compileOnly(gradleApi())
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
