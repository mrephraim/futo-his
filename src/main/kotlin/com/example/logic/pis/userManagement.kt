package com.example.logic.pis

import com.example.data.hashPassword
import com.example.data.models.*
import com.example.data.verifyPassword
import com.example.logic.capitalizeFirstLetter
import com.example.logic.emr.CreateAdminRequest
import com.example.logic.emr.EmrLoginSessionData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable


@Serializable
data class PisLoginSessionData(
    val errorMessage: String? = null,
    val userName: String? = null,
    val userId: Int? = null
)

fun Route.pisUserManagement(){
    get("/pis/login"){
        // Retrieve the session data
        val session = call.sessions.get<PisLoginSessionData>()

        // Check if userId exists and is not null
        val userId = session?.userId // Assuming you have userId in your SessionData class
        if (userId != null) {
            // Redirect to the dashboard if userId is present
            call.respondRedirect("/pis")
        } else {
            // Prepare the model for the login view
            val model: Map<String, Any> = mapOf("errorMessage" to (session?.errorMessage ?: ""))
            call.respond(ThymeleafContent("pis/login", model))

            // Clear the session after displaying the message
            call.sessions.clear<PisLoginSessionData>()
        }
    }
    // POST request for login
    post("/pis/login") { // No authentication required for login
        // Extract username and password from the request
        val parameters = call.receiveParameters()
        val username = parameters["username"] ?: ""
        val password = parameters["password"] ?: ""


        // Find user by username
        val user = findPisUser(username)

        // Authenticate the user
        if (user != null && verifyPassword(password, user.passwordHash)) {
            // Successful login, create a session or redirect to the dashboard
            val userName = user.username.capitalizeFirstLetter()
            val userId = user.id
            call.sessions.set(PisLoginSessionData(userName = userName, userId = userId, errorMessage = null))
            call.respondRedirect("/pis") // Redirect to a dashboard or home page
        } else {
            // Login failed, set the error message and redirect back to the login page
            val errorMessage = "Invalid username or password."
            call.sessions.set(PisLoginSessionData(errorMessage = errorMessage))
            call.respondRedirect("/pis/login")
        }
    }

    get("pis/new-user"){
        call.respond(ThymeleafContent("pis/new-user", emptyMap()))
    }

    post("pis/create-admin") {
        val request = call.receive<CreatePisAdminRequest>()
        val hashedPassword = hashPassword(request.password) // Replace with your hashing logic

        val userId = insertPisUser(request.username, "Admin", hashedPassword)
        println("THE USER ID IS:")
        println(userId)
        if (userId != null) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Admin created successfully"))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to create Admin"))
        }
    }

    post("pis/create-pis-personnel") {
        val request = call.receive<CreatePisPersonnelRequest>()
        val hashedPassword = hashPassword(request.password) // Replace with your hashing logic

        val userId = insertPisUser(request.username, "pis_personnel", hashedPassword)
        if (userId != null) {
            insertPisPersonnel(
                userId,
                request.name,
                request.specialization,
                request.email
            )
            call.respond(HttpStatusCode.OK, mapOf("message" to "Lab Attendant created successfully"))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to create Lab Attendant"))
        }
    }

}