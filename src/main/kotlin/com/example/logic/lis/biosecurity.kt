package com.example.logic.lis

import com.example.data.database.BioHazardIncidents
import com.example.data.database.LabAttendants
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class BiohazardRequest(
    val requisition: Int,
    val biohazardLevel: String,
    val labOfficer: Int,
    val hazardDescription: String
)


fun Route.biosecurity(){
    route("lis/biohazards") {
        get("/add-report"){
            call.respond(ThymeleafContent("/lis/report-biohazard", emptyMap()))
        }
        post("/add") {
            try {
                val request = call.receive<BiohazardRequest>()
                println(request)

                // Transaction to insert data into the Biohazards table
                transaction {
                    BioHazardIncidents.insert {
                        it[requisition] = request.requisition
                        it[labOfficer] = request.labOfficer
                        it[level] = request.biohazardLevel
                        it[hazardDescription] = request.hazardDescription
                    }
                }

                // Respond with success message
                call.respond(HttpStatusCode.OK, mapOf("message" to "Biohazard logged successfully"))
            } catch (e: Exception) {
                // If an error occurs, respond with an error message and status code
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to log biohazard: ${e.message}"))
            }
        }
        get("/lab-officers") {
            try {
                // Fetch the list of lab attendants from the database
                val attendants = transaction {
                    LabAttendants
                        .selectAll()
                        .map {
                            mapOf(
                                "attendantId" to it[LabAttendants.attendantId],
                                "name" to it[LabAttendants.name]
                            )
                        }
                }

                // Respond with the list of attendants
                call.respond(HttpStatusCode.OK, attendants)
            } catch (e: Exception) {
                // Handle errors and respond with an error message
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to fetch lab officers: ${e.message}"))
            }
        }



    }
}