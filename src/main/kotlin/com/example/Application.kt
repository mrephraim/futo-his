package com.example

import com.example.application.plugins.*
import com.example.application.routing.*
import com.example.data.database.configureDatabases
import com.example.data.hashPassword
import com.example.logic.emr.emrLoginAuthenticationInstallation
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
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

//    val password = "Ephraim"
//    val hashedPassword = hashPassword(password)
//    println("Password Hash: $hashedPassword")

}
