plugins {
    application
    id("io.atlasgo.hibernate-provider-gradle-plugin") version "0.0.0-SNAPSHOT"
}

group = "org.example"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:7.0.0.Final")
    // Required for classpath scanning (entity discovery) in Hibernate 7
    runtimeOnly("org.hibernate.orm:hibernate-scan-jandex:7.0.0.Final")
    runtimeOnly("com.h2database:h2:2.2.224")
}
