package com.example.logic.emr

import com.example.data.database.EmrPatients
import com.example.data.database.EmrUsers
import com.example.data.database.LabOrders
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/*
For here we do few things:
-routes for dashboard
-logic to get total indexed patients
 */

fun Route.emrDashboard(){
    get("/emr/dashboard/") {
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId = session?.userId // Assuming userId is part of your EmrLoginSessionData
        call.sessions.clear<UpdatePatientSession>()

        if (userId == null) {
            // Redirect to log in if userId is not present
            call.respondRedirect("/emr/login")
        } else {
            // Prepare the model with userName if userId exists
            val userName: Map<String, Any> = mapOf("userName" to (session.userName ?: ""))
            call.respond(ThymeleafContent("emr/index", userName))
        }
    }
    get("/emr/logout") {
        // Clear the session data
        call.sessions.clear<EmrLoginSessionData>()

        // Redirect to the login page or homepage after logging out
        call.respondRedirect("/emr/login")
    }

    get("/emr/total-patients"){
        val count = transaction { EmrPatients
            .selectAll()
            .count()
        }
        call.respond(mapOf("total" to count))
    }

    get("/emr/user-type") {
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId = session?.userId

        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "User not logged in")
            return@get
        }

        val userType = transaction {
            EmrUsers
                .select(EmrUsers.type)
                .where { EmrUsers.id eq userId }
                .map { it[EmrUsers.type] }
                .firstOrNull()
        }

        if (userType == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
        } else {
            call.respond(mapOf("type" to userType))
        }
    }



}