package com.example.logic.lis

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

fun Application.lisLoginAuthenticationInstallation() {
    install(Authentication) {
        basic("auth-basic") {
            realm = "Ktor Server"
            validate { credentials ->
                val user = findLisUser(credentials.name)
                if (user != null && verifyPassword(credentials.password, user.passwordHash)) {
                    UserIdPrincipal(user.username)
                } else {
                    null
                }
            }
        }
    }

}

@Serializable
data class LisLoginSessionData(
    val errorMessage: String? = null,
    val userName: String? = null,
    val userId: Int? = null
)

fun Route.lisUserManagement(){
    get("/lis/login"){
        // Retrieve the session data
        val session = call.sessions.get<LisLoginSessionData>()

        // Check if userId exists and is not null
        val userId = session?.userId // Assuming you have userId in your SessionData class
        if (userId != null) {
            // Redirect to the dashboard if userId is present
            call.respondRedirect("/lis")
        } else {
            // Prepare the model for the login view
            val model: Map<String, Any> = mapOf("errorMessage" to (session?.errorMessage ?: ""))
            call.respond(ThymeleafContent("lis/login", model))

            // Clear the session after displaying the message
            call.sessions.clear<LisLoginSessionData>()
        }
    }
    // POST request for login
    post("/lis/login") { // No authentication required for login
        // Extract username and password from the request
        val parameters = call.receiveParameters()
        val username = parameters["username"] ?: ""
        val password = parameters["password"] ?: ""


        // Find user by username
        val user = findLisUser(username)

        // Authenticate the user
        if (user != null && verifyPassword(password, user.passwordHash)) {
            // Successful login, create a session or redirect to the dashboard
            val userName = user.username.capitalizeFirstLetter()
            val userId = user.id
            call.sessions.set(LisLoginSessionData(userName = userName, userId = userId, errorMessage = null))
            call.respondRedirect("/lis") // Redirect to a dashboard or home page
        } else {
            // Login failed, set the error message and redirect back to the login page
            val errorMessage = "Invalid username or password."
            call.sessions.set(LisLoginSessionData(errorMessage = errorMessage))
            call.respondRedirect("/lis/login")
        }
    }

    get("lis/new-user"){
        call.respond(ThymeleafContent("lis/new-user", emptyMap()))
    }

    post("lis/create-admin") {
        val request = call.receive<CreateLisAdminRequest>()
        val hashedPassword = hashPassword(request.password) // Replace with your hashing logic

        val userId = insertLisUser(request.username, "Admin", hashedPassword)
        println("THE USER ID IS:")
        println(userId)
        if (userId != null) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Admin created successfully"))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Failed to create Admin"))
        }
    }

    post("lis/create-lab-attendant") {
        val request = call.receive<CreateLabAttendantRequest>()
        val hashedPassword = hashPassword(request.password) // Replace with your hashing logic

        val userId = insertLisUser(request.username, "lab_attendant", hashedPassword)
        if (userId != null) {
            insertLabAttendant(
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