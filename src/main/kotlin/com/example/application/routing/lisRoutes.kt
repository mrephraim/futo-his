package com.example.application.routing

import com.example.logic.lis.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.lisRoutes(){
    routing{
        lisUserManagement()
        lisDashboard()
        setupRoutes()
        requisition()
        result()
        biosecurity()
    }
}