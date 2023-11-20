plugins {
    application
}

group = "org.example"
version = "1.0"

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
