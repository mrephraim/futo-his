package com.example.application.routing

import com.example.logic.emr.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.emrRoutes(){
    routing{
        emrUsersManagement()
        emrDashboard()
        patientBasicManagement()
        patientFolder()
        appointmentManagement()
    }
}