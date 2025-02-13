val exposed_version: String by project
val h2_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val prometheus_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.0.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
}

group = "com.hris.employees"
version = "0.0.1"

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-status-pages")

    // DB
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("org.postgresql:postgresql:42.7.4")

    //Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    //Test
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    //DI
    implementation("org.kodein.di:kodein-di-framework-ktor-server-jvm:7.25.0")

    //Metrics
    implementation("io.ktor:ktor-server-call-id")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.13")
    implementation("io.ktor:ktor-server-metrics-micrometer")

    //Docs
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.swagger.codegen.v3:swagger-codegen-generators:1.0.56")

}
