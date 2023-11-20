pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal {
            url = uri("../../.local-plugin-repository")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "sample_with_local_plugin_repository"
