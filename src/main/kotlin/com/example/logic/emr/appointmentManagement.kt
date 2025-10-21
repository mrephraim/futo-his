package com.example.logic.emr

import com.example.data.database.*
import com.example.data.models.*
import com.example.logic.pis.PisLoginSessionData
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.text.insert


@Serializable
data class AppointmentResponse(
    val id: Int,
    val patientRegNo: String,
    val doctorId: String,
    val appointmentDate: String,
    val beginTime: String,
    val finishTime: String,
    val type: String,
    val department: String,
    val bloodPressure: String?,
    val pulseRate: String?,
    val temperature: String?,
    val respiratoryRate: String?,
    val oxygenSaturation: String?,
    val specialNotes: String?,
    val status: Int
)

// Data class for Lab Order Request
@Serializable
data class LabOrderRequest(
    val appointmentId: Int,
    val testCategory: Int,
    val testType: Int,
    val priority: String,
    val comment: String?
)

// Data class for response
@Serializable
data class VitalsResponse(
    val bloodPressure: String?,
    val pulseRate: String?,
    val temperature: String?,
    val respiratoryRate: String?,
    val oxygenSaturation: String?,
    val specialNotes: String?
)

@Serializable
data class LabOrderResponse(
    val id: Int,
    val testCategory: Int,
    val testType: String,
    val priority: String,
    val comment: String?,
    val status: String,
    val requisitionId: Int?
)


fun Route.appointmentManagement(){
    get("emr/book-appointment/{regNo}"){
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId = session?.userId // Assuming userId is part of your SessionData
        val regNo = call.parameters["regNo"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing regNo")
        if(userId == null){
            call.respondRedirect("/emr/login")
        }else{
            
        }
    }

    get("emr/new-appointment/{regNo}") {
        val regNo = call.parameters["regNo"] ?: return@get call.respondText(
            "Registration number is required",
            status = HttpStatusCode.BadRequest
        )

        // Query the database for the last appointment's status
        val appointmentStatus: Int? = transaction {
            exec("SELECT status FROM emrappointments WHERE patient_reg_no = '$regNo' ORDER BY id DESC LIMIT 1") { resultSet ->
                if (resultSet.next()) {
                    resultSet.getInt("status") // Assuming status is an integer
                } else {
                    null // No appointment found
                }
            }
        }

        println(appointmentStatus)

        // If appointmentStatus is null or not equal to 1, set it to 0
        val finalAppointmentStatus = appointmentStatus?.takeIf { it == 1 } ?: 0
        println(finalAppointmentStatus)

       // Prepare the context, ensuring appointmentStatus is valid
        val model: Map<String, Any> = mapOf(
            "regNo" to regNo,
            "appointmentStatus" to finalAppointmentStatus
        )

       // Pass the context to Thymeleaf
        call.respond(ThymeleafContent("/emr/new-appointment", model))

    }

    post("/emr/new-appointment") {
        val params = call.receiveParameters()

        try {
            // Extract parameters
            val patientRegNo = params["patientRegNo"] ?: throw IllegalArgumentException("PatientRegNo is required")
            val doctorId = params["doctorId"] ?: throw IllegalArgumentException("DoctorId is required")
            val appointmentDate = params["appointmentDate"] ?: throw IllegalArgumentException("AppointmentDate is required")
            val startTime = params["startTime"] ?: throw IllegalArgumentException("StartTime is required")
            val duration = params["duration"] ?: throw IllegalArgumentException("Duration is required")
            val department = params["department"] ?: throw IllegalArgumentException("Department is required")
            val type = params["type"] ?: throw IllegalArgumentException("Type is required")

            // Validate the startTime format (HH:mm)
            val timeRegex = Regex("^([01]?[0-9]|2[0-3]):([0-5][0-9])$")
            if (!startTime.matches(timeRegex)) {
                throw IllegalArgumentException("StartTime must be in the format HH:mm")
            }

            // If startTime is valid, create the request
            val createRequest = CreateAppointmentRequest(
                patientRegNo = patientRegNo,
                doctorId = doctorId,
                appointmentDate = appointmentDate,
                startTime = startTime, // Already in valid HH:mm format
                duration = duration, // Assuming it's in minutes (15, 30, etc.)
                department = department,
                type = type,
                status = 1 // Default status for a new appointment
            )

            // Call the function to create the appointment
            val isSuccess = createAppointment(createRequest)
            if (isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to "true",
                        "message" to "Appointment created successfully"
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to "false",
                        "error" to "Failed to create appointment"
                    )
                )
            }
        } catch (e: Exception) {
            // Handle unexpected errors
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("success" to "false", "message" to e.localizedMessage)
            )
        }
    }



    post("/emr/new-vital") {
        val params = call.receiveParameters()

        try {
            val updateRequest = UpdateAppointmentRequest(
                appointmentId = params["appointmentId"]?.toInt(),
                status = 1, // Default status for vitals entry
                bloodPressure = params["bloodPressure"],
                pulseRate = params["pulseRate"],
                temperature = params["temperature"],
                respiratoryRate = params["respiratoryRate"],
                oxygenSaturation = params["oxygenSaturation"],
                specialNotes = params["specialNotes"]
            )
            val regNo = params["regNo"].orEmpty()

            val isUpdated = addPatientVitals(regNo, updateRequest)
            if (isUpdated) {
                // Ensuring the response is in the format you want (success as string)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("success" to "true") // 'true' as a string
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to "false", "error" to "Failed to update appointment")
                )
            }
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("success" to "false", "error" to e.message)
            )
        }
    }

    get("/emr/api/appointments") {
        val regNo = call.sessions.get<PatientFolderSessionData>()?.regNo

        if (regNo == null) {
            call.respond(emptyList<AppointmentResponse>()) // Return empty if regNo is null
            return@get
        }

        val appointments = transaction {
            EmrAppointments.selectAll().where { EmrAppointments.patientRegNo eq regNo }
                .map {
                    AppointmentResponse(
                        id = it[EmrAppointments.id],
                        patientRegNo = it[EmrAppointments.patientRegNo],
                        doctorId = it[EmrAppointments.doctorId],
                        appointmentDate = it[EmrAppointments.appointmentDate],
                        beginTime = it[EmrAppointments.beginTime],
                        finishTime = it[EmrAppointments.finishTime],
                        type = it[EmrAppointments.type],
                        department = it[EmrAppointments.department],
                        bloodPressure = it[EmrAppointments.bloodPressure],
                        pulseRate = it[EmrAppointments.pulseRate],
                        temperature = it[EmrAppointments.temperature],
                        respiratoryRate = it[EmrAppointments.respiratoryRate],
                        oxygenSaturation = it[EmrAppointments.oxygenSaturation],
                        specialNotes = it[EmrAppointments.specialNotes],
                        status = it[EmrAppointments.status]
                    )
                }
        }
        call.respond(appointments)
    }

    get("emr/folder/appointments"){
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId: Map<String, Any> = mapOf("userId" to (session?.userId ?: ""))
        call.respond(ThymeleafContent("/emr/folder/appointments", userId))
    }
    get("emr/folder/view-appointment"){
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId: Map<String, Any> = mapOf("userId" to (session?.userId ?: ""))
        call.respond(ThymeleafContent("/emr/folder/view-appointment", userId))
    }

    // Fetch all lab categories
    get("/emr/api/lab-categories") {
        val categories = fetchCategories() // Using the provided function
        call.respond(categories)
    }

    // Fetch lab tests under a specific category
    get("/emr/api/lab-tests/{categoryId}") {
        val categoryId = call.parameters["categoryId"]?.toIntOrNull()
        if (categoryId == null) {
            call.respond(mapOf("error" to "Invalid category ID"))
            return@get
        }

        val tests = fetchTestsUnderCategory(categoryId) // Using the provided function
        call.respond(tests)
    }

    // Fetch lab orders for a specific appointment
    get("/emr/api/lab-orders/{appointmentId}") {
        val appointmentId = call.parameters["appointmentId"]?.toIntOrNull()
        if (appointmentId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid appointment ID"))
            return@get
        }

        val labOrders = transaction {
            LabOrders.selectAll()
                .where { LabOrders.appointmentId eq appointmentId }
                .map { labOrderRow ->
                    val testTypeId = labOrderRow[LabOrders.testType] // Get testType (ID)

                    // Query LabTests table to get the test name
                    val testName = LabTests
                        .select(LabTests.testName)
                        .where { LabTests.id eq testTypeId }
                        .map { it[LabTests.testName] }
                        .firstOrNull() ?: "Unknown Test" // Default to "Unknown Test" if not found

                    // Check if there is a requisition with rstatus = 2
                    val requisitionId = Requisitions
                        .select(Requisitions.id)
                        .where { (Requisitions.orderId eq labOrderRow[LabOrders.id]) and (Requisitions.rstatus eq 2) }
                        .map { it[Requisitions.id] }
                        .firstOrNull() // Get the first matching requisition ID or null

                    LabOrderResponse(
                        id = labOrderRow[LabOrders.id],
                        testCategory = labOrderRow[LabOrders.testCategory],
                        testType = testName,  // Now using the retrieved test name
                        priority = labOrderRow[LabOrders.priority],
                        comment = labOrderRow[LabOrders.comment],
                        status = labOrderRow[LabOrders.status],
                        requisitionId = requisitionId // Include requisition ID if found
                    )
                }
        }

        call.respond(labOrders)  // Respond with properly formatted data
    }




    // Submit a new lab test order
    post("/emr/api/lab-orders") {
        val request = call.receive<LabOrderRequest>()

        transaction {
            LabOrders.insert {
                it[appointmentId] = request.appointmentId
                it[testCategory] = request.testCategory
                it[testType] = request.testType
                it[priority] = request.priority
                it[comment] = request.comment
                it[status] = "pending"
            }
        }

        call.respond(mapOf("message" to "Lab order placed successfully"))
    }

    get("/emr/api/vitals/{appointmentId}") {
        val appointmentId = call.parameters["appointmentId"]?.toIntOrNull()

        if (appointmentId == null) {
            call.respond(mapOf("error" to "Invalid appointment ID"))
            return@get
        }

        val vitals = transaction {
            EmrAppointments
                .selectAll().where { EmrAppointments.id eq appointmentId }.firstNotNullOfOrNull {
                    VitalsResponse(
                        bloodPressure = it[EmrAppointments.bloodPressure],
                        pulseRate = it[EmrAppointments.pulseRate],
                        temperature = it[EmrAppointments.temperature],
                        respiratoryRate = it[EmrAppointments.respiratoryRate],
                        oxygenSaturation = it[EmrAppointments.oxygenSaturation],
                        specialNotes = it[EmrAppointments.specialNotes]
                    )
                }
        }

        if (vitals == null) {
            call.respond(mapOf("error" to "Vitals not found for appointment ID $appointmentId"))
        } else {
            call.respond(vitals)
        }
    }

    get("/emr/api/doctors") {
        val doctors = fetchAllDoctors() // Using provided function
        call.respond(doctors)
    }











}
