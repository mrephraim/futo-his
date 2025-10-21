
val h2_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val exposed_version: String by project


plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.1"
}


group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("org.jetbrains.exposed:exposed-core:0.53.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.53.0")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("io.ktor:ktor-server-csrf-jvm")
    implementation("at.favre.lib:bcrypt:0.9.0")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-thymeleaf")
    implementation("io.ktor:ktor-server-sessions")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-auth")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("com.sun.activation:jakarta.activation:2.0.1")

//    implementation group: 'com.sun.activation', name: 'javax.activation', version: '1.2.0'



    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
