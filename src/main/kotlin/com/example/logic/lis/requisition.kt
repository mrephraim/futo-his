package com.example.logic.lis

import com.example.data.database.*
import com.example.data.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.response.*
import kotlinx.datetime.LocalDateTime
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class RequisitionPayload(
    val regno: String,
    val requestFormat: String,
    val physician: String? = null,
    val testCategory: String,
    val labTest: String,
    val sampleType: String,
    val collectionDateTime: String,
    val priorityLevel: String,
    val orderId: String? = null,
    val clinicalNotes: String? = null
)


@Serializable
data class LabOrderResponse(
    val labOrderId: Int,
    val labTestName: String,
    val labTestCategory: String,
    val doctorName: String,
    val status: String,
    val comment: String,
    val regNo: String
)


fun Route.requisition(){
    get("lis/new-requisition"){
        call.respond(ThymeleafContent("lis/new-requisition", emptyMap()))
    }
    get ("lis/identify-patient"){
        val regNo = call.request.queryParameters["regno"]
        println("Received regNo: $regNo") // Debugging line
        if (regNo.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Registration number is required.")
            return@get
        }

        val patientInfo = fetchPatientBasicInfo(regNo)
        if (patientInfo != null) {
            call.respond(HttpStatusCode.OK, patientInfo)
        } else {
            call.respond(HttpStatusCode.NotFound, "Patient not found.")
        }
    }
    // Route to get all physicians
    get("lis/get-physicians") {
        try {
            val physicians = fetchAllDoctors()
            call.respond(HttpStatusCode.OK, mapOf("physicians" to physicians))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching physicians.")
        }
    }

    get ("lis/get-lab-tests"){
        val cid = call.request.queryParameters["categoryId"]?.toInt()
        println("Received CATEGORY ID: $cid") // Debugging line
        if (cid == null) {
            call.respond(HttpStatusCode.BadRequest, "Select Category First")
            return@get
        }

        val labTests = fetchTestsUnderCategory(cid)
        call.respond(HttpStatusCode.OK, mapOf("labTests" to labTests))
    }
    // Route to get samples for a lab test
    get("lis/get-samples-for-test") {
        val testId = call.request.queryParameters["testId"]?.toIntOrNull()
        if (testId == null) {
            call.respond(HttpStatusCode.BadRequest, "Test ID is required and must be an integer.")
            return@get
        }

        try {
            val samples = fetchSamplesForTest(testId)
            if (samples.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No samples found for the provided test ID.")
            } else {
                call.respond(HttpStatusCode.OK, mapOf("samples" to samples))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching samples.")
        }
    }


    post("lis/add-requisition") {
        val session = call.sessions.get<LisLoginSessionData>()

        // Ensure the session and userId exist
        val userId = session?.userId
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, "User is not logged in.")
            return@post
        }

        try {
            // Receive JSON payload
            val payload = call.receive<RequisitionPayload>()

            // Map JSON payload to your Requisition entity
            val requisition = Requisition(
                patientRegNo = payload.regno,
                requestFormat = payload.requestFormat,
                physicianId = if (payload.requestFormat == "direct") null else payload.physician?.toIntOrNull(),
                testCategoryId = payload.testCategory.toInt(),
                labTestId = payload.labTest.toInt(),
                sampleTypeId = payload.sampleType.toInt(),
                collectionDateTime = LocalDateTime.parse(payload.collectionDateTime),
                priorityLevel = payload.priorityLevel,
                clinicalNotes = payload.clinicalNotes,
                orderId = payload.orderId?.toInt(),
                authorizingOfficer = userId,
                sampleId = null
            )

            // Save the requisition
            val requisitionId = saveRequisitionAndGetId(requisition)

            if (requisitionId != null) {
                call.respond(HttpStatusCode.OK, mapOf("id" to requisitionId))
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch requisition ID.")
            }
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request data.")
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "An error occurred while processing the requisition.")
        }
    }
    get("lis/view-requisition"){
        call.respond(ThymeleafContent("lis/view-requisition", emptyMap()))
    }
    get("lis/api/requisition/{id}") {
        val requisitionId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Requisition ID is required")
        val requisition = getRequisitionById(requisitionId) // Replace with your database query
        println(requisition)
        if (requisition != null) {
            val requisitionAsStringMap = requisition.mapValues { it.value?.toString() ?: "" }
            call.respond(requisitionAsStringMap)
        } else {
            call.respond(HttpStatusCode.NotFound, "Requisition not found")
        }
    }

    get("lis/api/requisitions/pending") {
        call.respond(getPendingRequisitions())
    }

    get("lis/api/requisitions") {
        val viewType = call.request.queryParameters["type"] // Example: ?type=pending, published, all
        val requisitions = fetchRequisitions(viewType)
        call.respond(requisitions)
    }

    get("lis/requisitions") {
        call.respond(ThymeleafContent("lis/requisitions", emptyMap()))
    }

    post("lis/api/requisitions/archive/{id}") {
        val requisitionId = call.parameters["id"]?.toIntOrNull()

        if (requisitionId == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid requisition ID")
            return@post
        }

        try {
            transaction {
                Requisitions.update({ Requisitions.id eq requisitionId }) {
                    it[rstatus] = 0 // Set status to archived
                }
            }
            call.respond(HttpStatusCode.OK, "Requisition archived successfully")
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Failed to archive requisition")
        }
    }

    post("lis/api/requisitions/unarchive/{id}") {
        val requisitionId = call.parameters["id"]?.toIntOrNull()

        if (requisitionId == null) {
            call.respond(mapOf("error" to "Invalid requisition ID"))
            return@post
        }

        val rowsUpdated = transaction {
            Requisitions.update({ Requisitions.id eq requisitionId }) {
                it[rstatus] = 1 // Set status to Pending
            }
        }

        if (rowsUpdated > 0) {
            call.respond(mapOf("message" to "Requisition successfully unarchived"))
        } else {
            call.respond(mapOf("error" to "Requisition not found or update failed"))
        }
    }

    // Route to fetch the total number of requisitions with status = 1
    get("/lis/api/requisitions/pending-count") {
        // Fetch the count of requisitions where the status is 1 (pending)
        val pendingRequisitionsCount = transaction {
            Requisitions
                .selectAll().where { Requisitions.rstatus eq 1 }  // Assuming `status` column holds the status value
                .count()  // Count the number of rows where status = 1
        }

        // Respond with the count as a plain text
        call.respondText(pendingRequisitionsCount.toString())
    }

    post("/lis/update-lab-orders") {
        try {
            transaction {
                LabOrders.update({ LabOrders.status eq "pending" }) {
                    it[status] = "received"
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Lab orders updated"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.localizedMessage))
        }
    }

    get("/lis/fetch-lab-orders") {
        try {
            val labOrders = transaction {
                LabOrders
                    .selectAll()
                    .where { LabOrders.status inList listOf("pending", "received") }
                    .orderBy(LabOrders.id to SortOrder.DESC)
                    .map { row ->
                        val testTypeId = row[LabOrders.testType]
                        val testCategoryId = row[LabOrders.testCategory]
                        val appointmentId = row[LabOrders.appointmentId]

                        val labTestName = LabTests
                            .selectAll().where { LabTests.id eq testTypeId }
                            .map { it[LabTests.testName] }
                            .firstOrNull() ?: "Unknown"

                        val labTestCategory = LabCategories
                            .selectAll().where { LabCategories.id eq testCategoryId }
                            .map { it[LabCategories.name] }
                            .firstOrNull() ?: "Unknown"

                        // Get doctor ID from appointment table
                        val appointment = EmrAppointments
                            .selectAll().where { EmrAppointments.id eq appointmentId }
                            .firstOrNull()

                        val doctorId = appointment?.get(EmrAppointments.doctorId)
                        val regNo = appointment?.get(EmrAppointments.patientRegNo) ?: "Unknown" // Fetch the regNo

                        val doctorName = doctorId?.let {
                            EmrDoctors
                                .selectAll().where { EmrDoctors.doctorId eq it }
                                .map { it[EmrDoctors.name] }
                                .firstOrNull()
                        } ?: "Unknown"

                        LabOrderResponse(
                            labOrderId = row[LabOrders.id],
                            labTestName = labTestName,
                            labTestCategory = labTestCategory,
                            doctorName = doctorName,
                            status = row[LabOrders.status],
                            comment = row[LabOrders.comment] ?: "No comment",
                            regNo = regNo
                        )
                    }
            }

            call.respond(labOrders)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.localizedMessage))
        }
    }




    get("/lis/view-orders"){
        call.respond(ThymeleafContent("/lis/view-orders", emptyMap()))
    }









}