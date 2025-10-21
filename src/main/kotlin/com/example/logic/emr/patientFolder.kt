package com.example.logic.emr

import com.example.data.models.CompletePatientData
import com.example.data.models.fetchCompletePatientData
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable

@Serializable
data class PatientFolderSessionData(
    val regNo: String? = null
)
fun Route.patientFolder(){
    get("emr/folder/{regNo}"){
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId = session?.userId // Assuming userId is part of your SessionData
        val regNo = call.parameters["regNo"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing regNo")
        if (userId == null) {
            // Redirect to log in if userId is not present
            call.respondRedirect("/emr/login")
        }else{
            call.sessions.set(PatientFolderSessionData(regNo = regNo))
            call.respondRedirect("/emr/folder/")
        }
    }
    get("/emr/folder/"){
        call.respond(ThymeleafContent("emr/folder/index", emptyMap()))
    }
    get("emr/folder/patient-info"){
        val regNo = call.sessions.get<PatientFolderSessionData>()?.regNo
        if (regNo != null) {
            // Fetch the complete patient data only if regNo is not null
            val completePatientData = fetchCompletePatientData(regNo)
            if (completePatientData != null) {
                // Store the fetched data in the session
                call.sessions.set("PatientDetails", completePatientData)
                val sessionData = call.sessions.get<CompletePatientData>()
                if (sessionData != null) {
                    // Map both patientData and updateSession to the Thymeleaf template

                    call.respond(
                        ThymeleafContent(
                            "emr/folder/patient-info",
                            mapOf("patientData" to sessionData)
                        )
                    )
                    call.sessions.clear<UpdatePatientSession>()


                } else {
                    call.respond(HttpStatusCode.NotFound, "No patient data in session")
                }

            } else {
                call.respond(HttpStatusCode.NotFound, "Patient not found")
            }
        } else {
            // Handle the null case, e.g., by returning an error response
            call.respond(HttpStatusCode.BadRequest, "Registration number not found in session")
        }
    }
}