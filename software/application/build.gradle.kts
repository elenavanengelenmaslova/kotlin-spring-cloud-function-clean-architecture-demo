plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

springBoot {
    mainClass.set("com.example.clean.architecture.Application")
}

dependencies {
    implementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
}