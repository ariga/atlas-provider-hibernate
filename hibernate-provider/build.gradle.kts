import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    signing
    id("com.gradle.plugin-publish") version "1.1.0"
    kotlin("jvm") version "1.9.20"
}

group = "io.atlasgo"
version = System.getenv("PROVIDER_VERSION")

println("DORAV - provider version = $version")

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

dependencies {
    compileOnly("org.hibernate.orm:hibernate-core:6.1.7.Final")
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Implementation-Title" to rootProject.name,
            "Implementation-Version" to project.version))
    }
    archiveBaseName.set(rootProject.name)
}


publishing {
    publications {
        create<MavenPublication>("hibernate-provider") {
            signing {
                sign(publishing.publications["hibernate-provider"])
            }
            pom {
                name = "hibernate-provider"
                description = "A Hibernate schema provider for Atlas"
                url = "https://github.com/ariga/atlas-provider-hibernate"
                groupId = "io.atlasgo"
                artifactId = "hibernate-provider"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "Ariga"
                        name = "Ariga"
                        email = "it@atlasgo.io"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/ariga/atlas-provider-hibernate.git"
                    developerConnection = "scm:git:ssh://github.com/ariga/atlas-provider-hibernate.git"
                    url = "https://github.com/ariga/atlas-provider-hibernate"
                }
            }
        }
    }

    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            if (version.toString().endsWith("SNAPSHOT")) {
                url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
        }
        maven {
            name = "localPluginRepository"
            url = uri("../.local-plugin-repository")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
