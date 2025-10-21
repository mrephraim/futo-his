package com.example.logic.pis

import com.example.data.database.LisUsers
import com.example.data.database.PisUsers
import com.example.logic.lis.LisLoginSessionData
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.pisDashboard() {
    get("/pis") {
        val session = call.sessions.get<PisLoginSessionData>()
        val userId = session?.userId // Assuming userId is part of your EmrLoginSessionData

        if (userId == null) {
            // Redirect to log in if userId is not present
            call.respondRedirect("/pis/login")
        } else {
            // Prepare the model with userName if userId exists
            val data = mapOf(
                "userName" to (session.userName ?: ""),
                "userId" to (session.userId ?: "")
            )
            call.respond(ThymeleafContent("pis/index", data))
        }
    }
    get("/pis/user-type") {
        val session = call.sessions.get<PisLoginSessionData>()
        val userId = session?.userId

        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "User not logged in")
            return@get
        }

        val userType = transaction {
            PisUsers
                .select(PisUsers.type)
                .where { PisUsers.id eq userId }
                .map { it[PisUsers.type] }
                .firstOrNull()
        }

        if (userType == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
        } else {
            call.respond(mapOf("type" to userType))
        }
    }

    get("/pis/logout") {
        // Clear the session data
        call.sessions.clear<PisLoginSessionData>()

        // Redirect to the login page or homepage after logging out
        call.respondRedirect("/pis/login")
    }


}