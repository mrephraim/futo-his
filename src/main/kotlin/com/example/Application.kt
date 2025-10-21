package com.example

import com.example.application.plugins.*
import com.example.application.routing.*
import com.example.data.database.configureDatabases
import com.example.data.hashPassword
import com.example.logic.emr.emrLoginAuthenticationInstallation
import io.ktor.server.application.*

import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}


fun Application.module() {
    configureSerialization()
    emrLoginAuthenticationInstallation()
    configureRouting()
    installSessions()
    thymeLeaf()
    configureDatabases()
    emrRoutes()
    lisRoutes()
    pisRoutes()

    val password = "Ephraim"
    val hashedPassword = hashPassword(password)
    println("Password Hash: $hashedPassword")

}
