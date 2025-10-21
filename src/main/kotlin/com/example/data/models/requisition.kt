package com.example.data.models

import com.example.data.database.*
import com.example.logic.lis.getDoctorEmail
import com.example.logic.lis.getEmailCredentials
import com.example.logic.lis.getPatientDetails
import com.example.logic.lis.getRequisitionDetails
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.logic.lis.sendEmail
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.random.Random



@Serializable
data class Requisition(
    val patientRegNo: String,
    val requestFormat: String,
    val physicianId: Int?,
    val testCategoryId: Int,
    val labTestId: Int,
    val sampleTypeId: Int,
    val collectionDateTime: LocalDateTime,
    val priorityLevel: String,
    val clinicalNotes: String?,
    val sampleId: Int?,
    val orderId: Int?,
    val authorizingOfficer: Int
)
@Serializable
data class PendingRequisition(
    val id: Int,
    val patientRegNo: String,
    val testCategory: String,
    val labTest: String,
    val patientName: String,
    val priority: String
)

@Serializable
data class LabParameter(
    val id: Int,
    val name: String,
    val dataType: String,
    val description: String?,
    val units: LabParameterUnit,
    val value: String? = null
)

@Serializable
data class LabParameterUnit(
    val name: String?,
    val base: String,
    val factor: Double?
)

@Serializable
data class LabParameterComment(
    val id: Int,
    val parameterId: Int,
    val comment: String
)

@Serializable
data class FetchParametersResponse(
    val parameters: List<LabParameter?>,
    val error: String? = null,
    val lastUpdated: String? = null
)
@Serializable
sealed class ParameterValue {
    @Serializable
    @SerialName("boolean")
    data class BooleanValue(val value: Boolean) : ParameterValue()

    @Serializable
    @SerialName("string")
    data class StringValue(val value: String) : ParameterValue()

    @Serializable
    @SerialName("float")
    data class FloatValue(val value: Float) : ParameterValue()
}

@Serializable
data class FormSubmissionRequest(
    val status: Int,
    val requisitionId: Int,
    val parameters: List<ParameterValue>, // List of mixed parameter types
    val comments: List<Comment> // List of comment objects
)


@Serializable
data class Comment(
    val id: Int,
    val comment: String,
    val time: String // Date in string format
)

@Serializable
data class CommentResponse(
    val id: Int,
    val comment: String,
    val time: String // Use String to store the ISO timestamp
)
@Serializable
data class LabResultResponse(
    val requisition: Map<String, String>, // Convert all values to String to avoid serialization issues
    val parameters: FetchParametersResponse, // Make sure Parameter is also serializable
    val comments: List<CommentResponse> // Ensure Comment is serializable
)

@Serializable
data class RequisitionDTO(
    val id: Int,
    val patientName: String,
    val regNo: String,
    val labTest: String,
    val status: String
)




// Function to insert requisition and fetch its ID
fun saveRequisitionAndGetId(requisition: Requisition): Int? {
    return transaction {
        // Fetch names from the database based on the provided IDs
        val testCategoryName = LabCategories
            .selectAll().where { LabCategories.id eq requisition.testCategoryId }
            .map { it[LabCategories.name] }
            .firstOrNull() ?: throw IllegalArgumentException("Invalid test category ID: ${requisition.testCategoryId}")

        val labTestName = LabTests
            .selectAll().where { LabTests.id eq requisition.labTestId }
            .map { it[LabTests.testName] }
            .firstOrNull() ?: throw IllegalArgumentException("Invalid lab test ID: ${requisition.labTestId}")

        val sampleTypeName = LabSampleTable
            .selectAll().where { LabSampleTable.id eq requisition.sampleTypeId }
            .map { it[LabSampleTable.name] }
            .firstOrNull() ?: throw IllegalArgumentException("Invalid sample type ID: ${requisition.sampleTypeId}")

        // Generate a unique sample ID
        val uniqueSampleId = generateUniqueSampleId()
        val status = 1

        // Insert the requisition with names instead of IDs
        Requisitions.insert {
            it[patientRegNo] = requisition.patientRegNo
            it[requestFormat] = requisition.requestFormat
            it[physicianId] = requisition.physicianId
            it[testCategory] = testCategoryName // Save the category name
            it[labTest] = labTestName          // Save the lab test name
            it[labTestId] = requisition.labTestId
            it[sampleType] = sampleTypeName    // Save the sample type name
            it[sampleId] = uniqueSampleId
            it[collectionDateTime] = requisition.collectionDateTime
            it[priority] = requisition.priorityLevel
            it[clinicalNotes] = requisition.clinicalNotes
            it[rstatus] = status
            it[orderId] = requisition.orderId
            it[authorizingOfficer] = requisition.authorizingOfficer
        }

        // Fetch the last inserted ID for the given registration number
        Requisitions
            .selectAll().where { Requisitions.patientRegNo eq requisition.patientRegNo }
            .orderBy(Requisitions.id, SortOrder.DESC)
            .limit(1)
            .map { it[Requisitions.id] }
            .firstOrNull()
    }
}

fun generateUniqueSampleId(): Int {
    var sampleId = 0

    transaction {
        do {
            // Generate a 6-digit random number
            sampleId = Random.nextInt(100000, 1000000)

            // Check if the generated ID already exists in the database
        } while (Requisitions.selectAll().where { Requisitions.sampleId eq sampleId }.count() > 0)
    }

    return sampleId
}


// Function to get a requisition by ID
fun getRequisitionById(requisitionId: Int): Map<String, Any?>? {
    return transaction {
        // Fetch the requisition row
        Requisitions.selectAll().where { Requisitions.id eq requisitionId }
            .map { row ->
                // Fetch authorizing officer name based on their type
                val authorizingOfficerName = LisUsers
                    .selectAll().where { LisUsers.id eq row[Requisitions.authorizingOfficer] }
                    .firstNotNullOfOrNull { userRow ->
                        when (userRow[LisUsers.type]) {
                            "lab_attendant" -> {
                                LabAttendants
                                    .selectAll().where { LabAttendants.attendantId eq userRow[LisUsers.username] }
                                    .map { it[LabAttendants.name] }
                                    .firstOrNull()
                            }

                            "Admin" -> userRow[LisUsers.username]
                            else -> null
                        }
                    } ?: "Unknown Officer"

                // Fetch physician name
                val physician = row[Requisitions.physicianId].toString()
                val physicianName = EmrDoctors
                    .selectAll().where { EmrDoctors.doctorId eq physician }
                    .map { it[EmrDoctors.name] }
                    .firstOrNull() ?: "N/A"

                // Get full name
                val regno = row[Requisitions.patientRegNo]
                val fullName = EmrPatients
                    .selectAll().where { EmrPatients.regNo eq regno }
                    .map { "${it[EmrPatients.surName]} ${it[EmrPatients.firstName]}" }
                    .firstOrNull() ?: "N/A"

                // Get status
                var status = ""
                if (row[Requisitions.rstatus] == 1) {
                    status = "Pending Results"
                } else if (row[Requisitions.rstatus] == 2) {
                    status = "Results Published"
                }

                // Fetch BSL level from LabTests based on the LabTestId
                val labTestId = row[Requisitions.labTestId]
                val biosecurityLevel = LabTests
                    .selectAll().where { LabTests.id eq labTestId }
                    .map { it[LabTests.bsl] } // Assuming LabTests table has a 'bslLevel' column
                    .firstOrNull() ?: "Unknown Level"

                mapOf(
                    "id" to row[Requisitions.id],
                    "patientRegNo" to row[Requisitions.patientRegNo],
                    "fullName" to fullName,
                    "requestFormat" to row[Requisitions.requestFormat],
                    "physician" to physicianName, // Replace ID with name
                    "testCategoryId" to row[Requisitions.testCategory],
                    "labTestId" to row[Requisitions.labTest],
                    "bsl" to biosecurityLevel, // Add BSL Level here
                    "sampleTypeId" to row[Requisitions.sampleType],
                    "collectionDateTime" to row[Requisitions.collectionDateTime],
                    "priorityLevel" to row[Requisitions.priority],
                    "clinicalNotes" to row[Requisitions.clinicalNotes],
                    "sampleId" to row[Requisitions.sampleId],
                    "status" to status,
                    "authorizingOfficer" to authorizingOfficerName // Replace ID with name
                )
            }.firstOrNull()
    }
}

fun getPendingRequisitions(): List<PendingRequisition> {
    return transaction {
        Requisitions
            .selectAll().where { Requisitions.rstatus eq 1 }
            .map { it ->
                val regno = it[Requisitions.patientRegNo]
                val fullName = EmrPatients
                    .selectAll().where {EmrPatients.regNo eq regno}
                    .map { "${it[EmrPatients.surName]} ${it[EmrPatients.firstName]}" }
                    .firstOrNull() ?: "N/A"
                PendingRequisition(
                    id = it[Requisitions.id],
                    patientName = fullName,
                    patientRegNo = it[Requisitions.patientRegNo],
                    testCategory = it[Requisitions.testCategory],
                    labTest = it[Requisitions.labTest],
                    priority = it[Requisitions.priority]
                )
            }
    }
}

fun fetchParametersWithValues(requisitionId: Int): FetchParametersResponse {
    return transaction {
        // Step 1: Query the requisition table to get the lab test ID
        val labTestId = Requisitions
            .select(Requisitions.labTestId)
            .where { Requisitions.id eq requisitionId }
            .map { it[Requisitions.labTestId] }
            .firstOrNull()

        if (labTestId == null) {
            return@transaction FetchParametersResponse(
                parameters = emptyList(),
                error = "Requisition ID not found."
            )
        }

        // Step 2: Query the LabTests table to get parameter IDs
        val parameterIds = LabTests
            .select(LabTests.parameters)
            .where { LabTests.id eq labTestId }
            .map { Json.decodeFromString<List<Int>>(it[LabTests.parameters].toString()) } // Decode parameter IDs stored as JSON
            .firstOrNull() ?: emptyList()

        // Step 3: Query the LabParameters and LabParameterUnits tables for parameter details
        val parameters = LabParameters
            .join(LabParameterUnits, JoinType.LEFT, additionalConstraint = {
                LabParameters.id eq LabParameterUnits.parameterId
            })
            .selectAll().where { LabParameters.id inList parameterIds }
            .map {
                LabParameterUnit(
                    name = it[LabParameterUnits.name],
                    base = it[LabParameterUnits.base],
                    factor = it[LabParameterUnits.factor]
                ).let { it1 ->
                    LabParameter(
                        id = it[LabParameters.id],
                        name = it[LabParameters.name],
                        dataType = it[LabParameters.dataType],
                        description = it[LabParameters.description],
                        units = it1
                    )
                }
            }

        // Step 4: Query the LabResults table to check for existing parameter values
        val parameterValuesJson = LabResults
            .select(LabResults.parameters)
            .where { LabResults.requisitionId eq requisitionId }
            .map { it[LabResults.parameters] }
            .firstOrNull()

        val parameterValues: List<ParameterValue>? = parameterValuesJson?.let {
            Json.decodeFromString(it)
        }

        // Step 5: Add parameter values to the parameter details
        val enrichedParameters = parameters.mapIndexed { index, param ->
            param.copy(value = parameterValues?.getOrNull(index)?.toString())
        }
        val getLastUpdated = LabResults
            .select(LabResults.lastUpdated)
            .where { LabResults.requisitionId eq requisitionId }
            .map { it[LabResults.lastUpdated] }
            .firstOrNull()
        // Step 7: Return the structured response
        FetchParametersResponse(
            parameters = enrichedParameters,
            lastUpdated = getLastUpdated.toString()
        )
    }
}


fun processFormSubmission(request: FormSubmissionRequest): Boolean {
    return try {
        transaction {
            println("Starting form submission processing for requisitionId: ${request.requisitionId}")

            val existingRecord = LabResults
                .select { LabResults.requisitionId eq request.requisitionId }
                .firstOrNull()

            val serializedParameters = Json.encodeToString(request.parameters)
            val newComments = request.comments

            if (existingRecord != null) {
                println("Existing record found. Updating record for requisitionId: ${request.requisitionId}")

                val existingCommentsJson = existingRecord[LabResults.comments]
                val existingComments = try {
                    if (existingCommentsJson.isNotEmpty()) {
                        Json.decodeFromString<List<Comment>>(existingCommentsJson)
                    } else emptyList()
                } catch (e: Exception) {
                    println("Error decoding existing comments: ${e.message}")
                    emptyList()
                }

                val combinedComments = existingComments + newComments
                val serializedCombinedComments = Json.encodeToString(combinedComments)

                val updatedRows = LabResults.update({ LabResults.requisitionId eq request.requisitionId }) {
                    it[LabResults.parameters] = serializedParameters
                    it[LabResults.comments] = serializedCombinedComments
                }

                println("Updated rows in LabResults: $updatedRows")
            } else {
                println("No existing record found. Inserting new record for requisitionId: ${request.requisitionId}")

                val serializedComments = Json.encodeToString(newComments)
                LabResults.insert {
                    it[LabResults.requisitionId] = request.requisitionId
                    it[LabResults.parameters] = serializedParameters
                    it[LabResults.comments] = serializedComments
                }
            }

            // Fetch orderId from the Requisition table
            val orderId = Requisitions
                .select(Requisitions.orderId)
                .where { Requisitions.id eq request.requisitionId }
                .map { it[Requisitions.orderId] }
                .firstOrNull()

            println("Fetched orderId: $orderId for requisitionId: ${request.requisitionId}")

            if (request.status == 2) {
                println("Updating requisition status to 'Published'")

                val updatedRows = Requisitions.update({ Requisitions.id eq request.requisitionId }) {
                    it[Requisitions.rstatus] = 2
                }

                println("Updated requisition status rows: $updatedRows")

                if (updatedRows == 0) {
                    throw IllegalStateException("Failed to update requisition status for ID: ${request.requisitionId}")
                }

                if (orderId != null) {
                    val orderUpdatedRows = LabOrders.update({ LabOrders.id eq orderId }) {
                        it[LabOrders.status] = "Published"
                    }
                    println("Updated LabOrders to 'Published' rows: $orderUpdatedRows")
                }
            } else if (orderId != null) {
                println("Checking current status of lab order for orderId: $orderId")

                val currentStatus = LabOrders
                    .select(LabOrders.status)
                    .where { LabOrders.id eq orderId }
                    .map { it[LabOrders.status] }
                    .firstOrNull()

                if (currentStatus != "Processing") {
                    val statusUpdatedRows = LabOrders.update({ LabOrders.id eq orderId }) {
                        it[LabOrders.status] = "Processing"
                    }

                    println("Updated LabOrders to 'Processing' rows: $statusUpdatedRows")
                }
            }
        } // End of transaction

        // Only send an email if status == 2
        if (request.status == 2) {
            println("Preparing to send email for published result...")

            val (regNo, physicianId, labTest) = getRequisitionDetails(request.requisitionId) ?: return false
            val (firstName, surname, patientEmail) = getPatientDetails(regNo) ?: Triple(null, null, null)
            val doctorEmail = physicianId?.let { getDoctorEmail(it.toString()) }
            val patientName = "$firstName $surname"

            // Prepare recipient list
            val recipients = mutableListOf<String>()
            if (!patientEmail.isNullOrEmpty()) recipients.add(patientEmail)
            if (!doctorEmail.isNullOrEmpty()) recipients.add(doctorEmail)

            println("Email recipients: $recipients")

            // If there are no valid recipients, don't send the email
            if (recipients.isEmpty()) {
                println("No valid recipients found. Skipping email.")
                return false
            }

            // Craft email message
            val subject = "Lab Results Available - Requisition ID: ${request.requisitionId}"
            val message = """
                Hello,

                Lab result for $patientName are now available.
                You can pick it up at the health center any time.
                Please copy out the info below, it is used to sort out the lab result faster.

                Patient Name: $patientName
                Registration No: $regNo
                Requisition ID: ${request.requisitionId}
                Test: $labTest

                Regards,
                FUTO Health Services, Laboratory Team
            """.trimIndent()

            val (email, password) = getEmailCredentials()
            val emailSent = sendEmail(subject, message, recipients, email, password)

            println("Email sent status: $emailSent")

            return emailSent
        }

        println("Form submission processed successfully.")
        true // If not sending an email, return true
    } catch (e: Exception) {
        println("Error in processFormSubmission: ${e.localizedMessage}")
        e.printStackTrace()
        false
    }
}


fun fetchCommentsFromDatabase(requisitionId: Int): List<CommentResponse> {
    return transaction {
        LabResults
            .selectAll().where { LabResults.requisitionId eq requisitionId }
            .mapNotNull { row ->
                val commentJson = row[LabResults.comments]
                try {
                    // Decode JSON string into a list of CommentResponse
                    Json.decodeFromString<List<CommentResponse>>(commentJson)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            .flatten() // Flatten the list if multiple rows are fetched
    }
}

fun deleteComment(commentId: Int, requisitionId: Int): Boolean {
    return try {
        transaction {
            // Fetch the existing comments for the given requisitionId
            val existingRecord = LabResults.selectAll()
                .where { LabResults.requisitionId eq requisitionId }
                .firstOrNull()

            if (existingRecord != null) {
                // Deserialize existing comments
                val existingCommentsJson = existingRecord[LabResults.comments]
                val existingComments = if (existingCommentsJson.isNotEmpty()) {
                    Json.decodeFromString<List<Comment>>(existingCommentsJson)
                } else {
                    emptyList()
                }

                // Filter out the comment by id
                val filteredComments = existingComments.filterNot { it.id == commentId }

                // Serialize the updated comments
                val updatedCommentsJson = Json.encodeToString(filteredComments)

                // Update the record with the filtered comments
                LabResults.update({ LabResults.requisitionId eq requisitionId }) {
                    it[LabResults.comments] = updatedCommentsJson
                }
                true
            } else {
                false // No record found for the requisitionId
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun fetchRequisitions(viewType: String?): List<RequisitionDTO> {
    return transaction {
        // Exclude archived
        (Requisitions.leftJoin(EmrPatients, { Requisitions.patientRegNo }, { EmrPatients.regNo }))
            .leftJoin(LabTests, { Requisitions.labTestId }, { LabTests.id })
            .selectAll().where { // Exclude archived
                when (viewType) {
                    "pending" -> Requisitions.rstatus eq 1
                    "processed" -> Requisitions.rstatus eq 2
                    "archived" -> Requisitions.rstatus eq 0
                    "all" -> (Requisitions.rstatus neq 0) // Exclude archived only when "all" is selected
                    else -> Op.FALSE
                }
            }
            .map {
                RequisitionDTO(
                    id = it[Requisitions.id],
                    patientName = "${it[EmrPatients.firstName]} ${it[EmrPatients.surName]}", // Handle null cases
                    regNo = it[Requisitions.patientRegNo], // Patient reg number
                    labTest = it[Requisitions.labTest], // Lab test name
                    status = mapStatus(it[Requisitions.rstatus]) // Convert status code
                )
            }
    }
}

fun mapStatus(status: Int): String {
    return when (status) {
        1 -> "Pending"
        2 -> "Published"
        0 -> "Archived" // This won't appear since we exclude archived in the main query
        else -> "Unknown"
    }
}



