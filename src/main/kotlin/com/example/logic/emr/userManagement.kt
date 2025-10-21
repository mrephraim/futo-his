package com.example.logic.emr

import com.example.data.database.EmrUsers
import com.example.data.hashPassword
import com.example.data.models.Doctor
import com.example.data.models.addDoctor
import com.example.data.models.addUser
import com.example.data.models.findUserByUsername
import com.example.data.verifyPassword
import com.example.logic.capitalizeFirstLetter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.emrLoginAuthenticationInstallation() {
    install(Authentication) {
        basic("auth-basic") {
            realm = "Ktor Server"
            validate { credentials ->
                val user = findUserByUsername(credentials.name)
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
data class EmrLoginSessionData(
    val errorMessage: String? = null,
    val userName: String? = null,
    val userId: Int? = null
)
@Serializable
data class CreateAdminRequest(
    val username: String, // Admin username
    val passwordHash: String, // Hashed password (sent as a base64 string in the example)
    val type: String = "Admin" // User type, default is "Admin"
)

@Serializable
data class CreateDoctorRequest(
    val username: String,
    val passwordHash: String,
    val name: String,
    val specialization: String,
    val phoneNo: String,
    val email: String,
    val officeLocation: String
)


fun Route.emrUsersManagement(){
    get("/emr/login") {
        // Retrieve the session data
        val session = call.sessions.get<EmrLoginSessionData>()

        // Check if userId exists and is not null
        val userId = session?.userId // Assuming you have userId in your SessionData class
        if (userId != null) {
            // Redirect to the dashboard if userId is present
            call.respondRedirect("/emr/dashboard/")
        } else {
            // Prepare the model for the login view
            val model: Map<String, Any> = mapOf("errorMessage" to (session?.errorMessage ?: ""))
            call.respond(ThymeleafContent("emr/login", model))

            // Clear the session after displaying the message
            call.sessions.clear<EmrLoginSessionData>()
        }
    }


    // POST request for login
    post("/emr/login/auth") { // No authentication required for login
        // Extract username and password from the request
        val parameters = call.receiveParameters()
        val username = parameters["username"] ?: ""
        val password = parameters["password"] ?: ""


        // Find user by username
        val user = findUserByUsername(username)

        // Authenticate the user
        if (user != null && verifyPassword(password, user.passwordHash)) {
            // Successful login, create a session or redirect to the dashboard
            val userName = user.username.capitalizeFirstLetter()
            val userId = user.id
            call.sessions.set(EmrLoginSessionData(userName = userName, userId = userId))
            call.respondRedirect("/emr/dashboard/") // Redirect to a dashboard or home page
        } else {
            // Login failed, set the error message and redirect back to the login page
            val errorMessage = "Invalid username or password."
            call.sessions.set(EmrLoginSessionData(errorMessage = errorMessage))
            call.respondRedirect("/emr/login")
        }
    }

    get("/emr/new-user"){

        call.respond(ThymeleafContent("emr/new-user", emptyMap()))
    }
    post("/emr/new-user") {
        val adminRequest = call.receive<CreateAdminRequest>()
        // Validate and save admin details
        val user = addUser(adminRequest.username, hashPassword(adminRequest.passwordHash), adminRequest.type)
        if (user != null) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Admin created successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create admin"))
        }
    }

    post("/emr/add-doctor") {
        try {
            // Step 1: Receive the request
            val request = call.receive<CreateDoctorRequest>()

            // Step 2: Check if the username already exists
            val existingUser = withContext(Dispatchers.IO) {
                transaction {
                    EmrUsers.selectAll().where { EmrUsers.username eq request.username }.singleOrNull()
                }
            }

            if (existingUser != null) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("success" to "false", "message" to "Username already exists.")
                )
                return@post
            }

            // Step 3: Hash the password
            val hashedPassword = hashPassword(request.passwordHash) // Replace with your hashing function

            // Step 4: Add the user
            val newUser = withContext(Dispatchers.IO) {
                addUser(request.username, hashedPassword, "Doctor")
            }

            if (newUser == null) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("success" to "false", "message" to "Failed to create user.")
                )
                return@post
            }

            // Step 5: Add the doctor
            withContext(Dispatchers.IO) {
                transaction {
                    addDoctor(
                        Doctor(
                            doctorId = newUser.id.toString(),
                            name = request.name,
                            specialization = request.specialization,
                            phoneNo = request.phoneNo,
                            email = request.email,
                            officeLocation = request.officeLocation
                        )
                    )
                }
            }

            // Step 6: Respond with success
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "success" to "true",
                    "message" to "Account created successfully"
                )
            )
        } catch (e: Exception) {
            // Handle unexpected errors
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("success" to "false", "message" to e.localizedMessage)
            )
        }
    }




}