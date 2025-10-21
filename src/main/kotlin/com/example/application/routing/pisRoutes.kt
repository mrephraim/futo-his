package com.example.application.routing

import com.example.logic.pis.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.pisRoutes(){
    routing {
        pisUserManagement()
        pisDashboard()
        pisSetupRoutes()
        pisInventoryRoutes()
        prescriptionRoutes()
    }
}