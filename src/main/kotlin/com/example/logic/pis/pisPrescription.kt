package com.example.logic.pis

import com.example.data.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.json.Json

fun Route.prescriptionRoutes() {
    route("pis/prescriptions") {
        get("/view"){
            call.respond(ThymeleafContent("pis/view-prescription", emptyMap()))
        }
        get{
            val session = call.sessions.get<PisLoginSessionData>()
            val userId: Map<String, Any> = mapOf("userId" to (session?.userId ?: ""))
            call.respond(ThymeleafContent("pis/prescription_orders", userId))
        }

        get("/{drugId}/dosage-units") {
            try {
                val drugId = call.parameters["drugId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Invalid drug id")
                    )

                val unitOptions = getDrugBasePackageAndUnits(drugId).map {
                    UnitOptionResponse(
                        id = it.id,
                        name = it.name,
                        type = it.type
                    )
                }

                call.respond(unitOptions)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, message = e.message ?: "Error fetching units")
                )
            }
        }

        post("/patient-info") {
            try {
                val request = call.receive<PatientLookupRequest>()
                val data = fetchPatientPharmacyData(request.regNo)

                if (data == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, message = "Patient not found")
                    )
                } else {
                    call.respond(data)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, message = e.message ?: "Error fetching patient data")
                )
            }
        }
        get("/patient/{regNo}") {
            try {
                val regNo = call.parameters["regNo"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest,
                        PatientResponse(success = false, error = "Missing registration number"))

                val patient = getPatientBasicDetailsByRegNo(regNo)
                if (patient == null) {
                    call.respond(HttpStatusCode.NotFound,
                        PatientResponse(success = false, error = "Patient not found"))
                } else {
                    call.respond(HttpStatusCode.OK,
                        PatientResponse(success = true, data = patient))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    PatientResponse(success = false, error = e.message ?: "Failed to fetch patient details"))
            }
        }


        get ("/fetch"){
            try {
                val status = call.request.queryParameters["status"] ?: "ALL"
                val appointmentId = call.request.queryParameters["appointmentId"]?.toIntOrNull()

                val prescriptions = getPrescriptionsByStatus(status, appointmentId)


                call.respond(
                    PrescriptionResponse(
                        success = true,
                        message = "Prescriptions retrieved successfully",
                        data = prescriptions
                    )
                )
            } catch (e: IllegalArgumentException) {
                // Handles invalid status enums
                call.respond(
                    PrescriptionResponse(
                        success = false,
                        message = "Invalid status provided. Use ALL, PRESCRIBED, ONGOING, or COMPLETED."
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    PrescriptionResponse(
                        success = false,
                        message = "Failed to retrieve prescriptions: ${e.message}"
                    )
                )
            }
        }


        post("/details") {
            try {
                val request = call.receive<PrescriptionRequest>()
                val prescription = getPrescriptionById(request.prescriptionId)

                if (prescription != null) {
                    call.respond(
                        SinglePrescriptionResponse(
                            success = true,
                            message = "Prescription retrieved successfully",
                            data = prescription
                        )
                    )
                } else {
                    call.respond(
                        SinglePrescriptionResponse(
                            success = false,
                            message = "Prescription not found"
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    SinglePrescriptionResponse(
                        success = false,
                        message = "Failed to retrieve prescription: ${e.message}"
                    )
                )
            }
        }

        post("/dispense") {
            try {
                val request = call.receive<DispenseRequest>()

                dispensePrescription(request.prescriptionId)

                call.respond(
                    DispenseResponse(
                        success = true,
                        message = "Prescription ${request.prescriptionId} dispensed successfully"
                    )
                )
            } catch (e: IllegalStateException) {
                // Exposed error("...") throws IllegalStateException
                call.respond(
                    HttpStatusCode.BadRequest,
                    DispenseResponse(
                        success = false,
                        message = e.message ?: "Dispensing failed"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    DispenseResponse(
                        success = false,
                        message = "Unexpected error: ${e.message}"
                    )
                )
            }
        }

        post("/create") {
            try {
                val rawBody = call.receiveText()
                println("Incoming payload: $rawBody")
                val request = Json.decodeFromString<CreatePrescriptionRequest>(rawBody)


                val prescriptionId = createPrescription(request)

                call.respond(
                    CreatePrescriptionResponse(
                        success = true,
                        message = "Prescription created successfully",
                        prescriptionId = prescriptionId
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    CreatePrescriptionResponse(
                        success = false,
                        message = "Failed to create prescription: ${e.message}"
                    )
                )
            }
        }

        get("/count/ongoing") {
            try {
                val count = getOngoingPrescriptionsCount()
                call.respond(HttpStatusCode.OK, mapOf("ongoingCount" to count))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch ongoing prescriptions count"))
            }
        }

        get("/count/prescribed") {
            try {
                val count = getPrescribedPrescriptionsCount()
                call.respond(HttpStatusCode.OK, mapOf("prescribedCount" to count))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch prescribed prescriptions count"))
            }
        }



    }
}
