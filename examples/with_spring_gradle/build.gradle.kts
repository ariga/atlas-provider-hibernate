plugins {
    id("java")
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal {
        url = uri("../../.local-plugin-repository")
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    runtimeOnly("com.h2database:h2:2.2.224")
    implementation("io.atlasgo:hibernate-provider:0.1")
    implementation("org.hibernate.orm:hibernate-core:6.3.1.Final")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.2.0") {
        exclude(module="logback-classic")
    }
    runtimeOnly("com.mysql:mysql-connector-j")
}