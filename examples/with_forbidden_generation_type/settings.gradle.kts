pluginManagement {
    repositories {
        mavenLocal {
            url = uri("../../.local-plugin-repository")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "with_forbidden_generation_type"