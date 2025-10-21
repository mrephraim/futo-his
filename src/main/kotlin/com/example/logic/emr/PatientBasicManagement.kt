package com.example.logic.emr

import com.example.data.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class UpdatePatientSession(
    val errors: MutableList<String> = mutableListOf(),
    val updateStatus: String = ""
)
@Serializable
data class AddPatientSessionData(
    val errors: MutableList<String> = mutableListOf(),
)

@Serializable
data class ApiResponse(val success: Boolean, val message: String, val errors: List<String>? = null)



fun Route.patientBasicManagement(){
    get("/emr/add-patient") {
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId = session?.userId // Assuming userId is part of your SessionData

        if (userId == null) {
            // Redirect to log in if userId is not present
            call.respondRedirect("/emr/login")
        } else {
            // Get errors from session if any
            val addPatientSessionData = call.sessions.get<AddPatientSessionData>()
            val errors = addPatientSessionData?.errors ?: emptyList()

            // Clear errors after retrieval
            call.sessions.set(AddPatientSessionData()) // Resetting the errors

            // Pass errors to the view
            call.respond(ThymeleafContent("emr/add-patient", mapOf("errors" to errors)))
        }
    }

    post("/emr/add-patient") {
        val parameters = call.receiveParameters()
        val errors = mutableListOf<String>()

        val regNo = parameters["regNo"]?.trim()?.takeIf {
            it.length == 11 && it.all { char -> char.isDigit() }
        } ?: errors.add("Invalid registration number, must be exactly 11 digits.")

        if (doesPatientExist(regNo.toString())) {
            errors.add("Patient already exists. Please use the edit-info feature on the dashboard.")
        }

        val firstName = parameters["firstName"]?.trim()?.takeIf { it.matches(Regex("[A-Za-z]+")) }
            ?: errors.add("Valid first name is required.")

        val surName = parameters["surName"]?.trim()?.takeIf { it.matches(Regex("[A-Za-z]+")) }
            ?: errors.add("Valid surname is required.")

        val middleName = parameters["middleName"]?.trim()?.takeIf { it.matches(Regex("[A-Za-z]*")) }

        val school = parameters["schools"]?.takeIf { it.isNotBlank() }
            ?: errors.add("School selection is required.")

        val department = parameters["departments"]?.takeIf { it.isNotBlank() }
            ?: errors.add("Department selection is required.")

        val email = parameters["email"]?.takeIf { it.isNotBlank() }
            ?: errors.add("Email is required.")

        val phoneNo = parameters["phoneNo"]?.takeIf { it.matches(Regex("\\d{10,15}")) }
            ?: errors.add("A valid phone number is required.")

        val dobDay = parameters["dob_day"]?.toIntOrNull()?.takeIf { it in 1..31 }
            ?: errors.add("Day of birth is required.")

        val dobMonth = parameters["dob_month"]?.toIntOrNull()?.takeIf { it in 1..12 }
            ?: errors.add("Month of birth is required.")

        val dobYear = parameters["dob_year"]?.toIntOrNull()?.takeIf { it in 1900..(LocalDate.now().year) }
            ?: errors.add("Year of birth is required.")

        val dob = "$dobYear-$dobMonth-$dobDay"

        val sex = parameters["sex"]?.takeIf { it in listOf("Male", "Female", "Other") }
            ?: errors.add("Sex selection is required.")

        val maritalStatus = parameters["maritalStatus"]?.takeIf { it in listOf("Single", "Married", "Divorced", "Widowed") }
            ?: errors.add("Marital status is required.")

        val hostelAddress = parameters["hostelAddress"]?.trim()
        val homeTown = parameters["homeTown"]?.trim()?.takeIf { it.isNotBlank() }
            ?: errors.add("Home town is required.")

        val lga = parameters["lga"]?.trim()?.takeIf { it.isNotBlank() }
            ?: errors.add("Local Government Area is required.")

        val state = parameters["state"]?.trim()?.takeIf { it.isNotBlank() }
            ?: errors.add("State is required.")

        val country = parameters["country"]?.trim()?.takeIf { it.isNotBlank() }
            ?: errors.add("Country is required.")

        // **Return validation errors if any**
        if (errors.isNotEmpty()) {
            call.respond(ApiResponse(success = false, message = "Validation errors occurred.", errors = errors))
            return@post
        }

        // **Create patient object**
        val patient = Patient(
            regNo = regNo.toString(),
            firstName = firstName.toString(),
            surName = surName.toString(),
            middleName = middleName,
            school = school.toString(),
            department = department.toString(),
            phoneNo = phoneNo.toString(),
            email = email.toString(),
            dob = dob,
            sex = sex.toString(),
            maritalStatus = maritalStatus.toString(),
            hostelAddress = hostelAddress,
            homeTown = homeTown.toString(),
            lga = lga.toString(),
            state = state.toString(),
            country = country.toString()
        )

        val success = addPatient(patient)

        if (success) {
            call.respond(ApiResponse(success = true, message = "Patient added successfully!"))
        } else {
            call.respond(ApiResponse(success = false, message = "Failed to add patient. Please try again."))
        }
    }

    get("/emr/edit-patient/{regNo}") {
        val session = call.sessions.get<EmrLoginSessionData>()
        val userId = session?.userId // Assuming userId is part of your SessionData

        if (userId == null) {
            // Redirect to log in if userId is not present
            call.respondRedirect("/emr/login")
        } else {
            val regNo = call.parameters["regNo"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing regNo")

            // Clear any existing session data for PatientDetails
            call.sessions.clear("PatientDetails") // Clear specific session data

            // Fetch the complete patient data
            val completePatientData = fetchCompletePatientData(regNo)

            if (completePatientData != null) {
                // Store the fetched data in the session
                call.sessions.set("PatientDetails", completePatientData)
                call.respondRedirect("/emr/edit-patient") // Redirect to the edit page
            } else {
                call.respond(HttpStatusCode.NotFound, "Patient not found")
            }
        }
    }

    get("/emr/edit-patient") {
        // Retrieve the patient data and update session data from the session
        val sessionData = call.sessions.get<CompletePatientData>()
        val updateSession = call.sessions.get<UpdatePatientSession>() ?: UpdatePatientSession() // Use a default empty session if null

        // Log the session data to verify it is being passed correctly
        println("Patient data: $sessionData")
        println("Update session data: $updateSession")

        if (sessionData != null) {
            // Map both patientData and updateSession to the Thymeleaf template

            call.respond(
                ThymeleafContent(
                    "emr/edit-patient",
                    mapOf("patientData" to sessionData, "updateSession" to updateSession)
                )
            )
            call.sessions.clear<UpdatePatientSession>()


        } else {
            call.respond(HttpStatusCode.NotFound, "No patient data in session")
        }
    }

    post("/emr/edit-patient/{type}") {
        val type = call.parameters["type"]
        val session = call.sessions.get<UpdatePatientSession>() ?: UpdatePatientSession()

        // Get parameters from the form
        val params = call.receiveParameters()
        val errors = mutableListOf<String>()

        // Initialize the status message
        var updateStatus = ""

        when (type) {
            "patient" -> {
                val patient = Patient(
                    regNo = params["regNo"] ?: "",
                    surName = params["surName"] ?: "",
                    firstName = params["firstName"] ?: "",
                    middleName =  params["middleName"] ?: "",
                    phoneNo = params["phoneNo"] ?: "",
                    email = params["email"] ?: "",
                    dob = params["dob"] ?: "",
                    sex = params["sex"] ?: "",
                    maritalStatus = params["maritalStatus"] ?: "",
                    school = params["school"] ?: "",
                    department = params["department"] ?: "",
                    hostelAddress = params["hostelAddress"],
                    homeTown = params["homeTown"] ?: "",
                    lga = params["lga"] ?: "",
                    state = params["state"] ?: "",
                    country = params["country"] ?: ""
                )
                println("Received patient data: $patient")
                // Validate patient

                if (patient.surName.isEmpty()) {
                    errors.add("Surname cannot be empty.")
                }
                if (patient.firstName.isEmpty()) {
                    errors.add("First name cannot be empty.")
                }
                if (patient.phoneNo.isEmpty()) {
                    errors.add("Phone number cannot be empty.")
                }
                if (patient.email.isEmpty()) {
                    errors.add("Email cannot be empty.")
                }

                if (errors.isEmpty()) {
                    val updateResult = updatePatient(patient)
                    if (updateResult) {
                        updateStatus = "Patient ${patient.firstName} ${patient.surName} updated successfully."
                        println("Patient updated successfully")
                    } else {
                        errors.add("Error updating patient.")
                        updateStatus = "Error updating patient."
                        println("Validation failed, errors: $errors")
                    }
                } else {
                    updateStatus = "Validation failed. Please fix the errors."
                    println("Validation Error")
                }
            }
            "kin" -> {
                val kin = EmrPatientsKin(
                    regNo = params["regNo"] ?: "",
                    name = params["name"] ?: "",
                    phoneNo = params["phoneNo"] ?: "",
                    residentialAddress = params["residentialAddress"] ?: "",
                    occupation = params["occupation"] ?: "",
                    relationToStudent = params["relationToStudent"] ?: ""
                )

                // Validate kin
                if (kin.name.isEmpty()) {
                    errors.add("Kin name cannot be empty.")
                }
                if (kin.residentialAddress.isEmpty()) {
                    errors.add("Residential address cannot be empty.")
                }
                if (kin.occupation.isEmpty()) {
                    errors.add("Occupation cannot be empty.")
                }

                if (errors.isEmpty()) {
                    val updateResult = updateKin(kin)
                    if (updateResult) {
                        updateStatus = "Patient kin saved updated successfully."
                    } else {
                        errors.add("Error updating kin.")
                    }
                }
            }
            "guardian" -> {
                val guardian = EmrPatientsGuardian(
                    regNo = params["regNo"] ?: "",
                    name = params["name"] ?: "",
                    phoneNo = params["phoneNo"] ?: "",
                    officeAddress = params["officeAddress"] ?: "",
                    residentialAddress = params["residentialAddress"] ?: "",
                    occupation = params["occupation"] ?: ""
                )


                if (guardian.name.isEmpty()) {
                    errors.add("Guardian name cannot be empty.")
                }
                if (guardian.phoneNo.isEmpty()) {
                    errors.add("Phone number cannot be empty.")
                }

                if (errors.isEmpty()) {
                    val updateResult = updateGuardian(guardian)
                    if (updateResult) {
                        session.errors.add("Guardian ${guardian.name} updated successfully.")
                        updateStatus = "Patient guardian info saved successfully."
                    } else {
                        errors.add("Error updating guardian.")
                    }
                }
            }
            "medical-history" -> {
                val medicalHistory = EmrPatientMedicalHistoryData(
                    regNo = params["regNo"] ?: "",
                    familyHistory = params["familyHistory"] ?: "",
                    previousIllness = params["previousIllness"] ?: "",
                    height = params["height"]?.toDoubleOrNull() ?: 0.0,
                    weight = params["weight"]?.toDoubleOrNull() ?: 0.0,
                    visualAcuity = params["visualAcuity"] ?: "",
                    cardiovascularHealth = params["cardiovascularHealth"] ?: "",
                    hearingCondition = params["hearingCondition"] ?: "",
                    abdominalHealth = params["abdominalHealth"] ?: "",
                    respiratoryHealth = params["respiratoryHealth"] ?: "",
                    extremities = params["extremities"] ?: "",
                    psychiatricState = params["psychiatricState"] ?: "",
                    genotype = params["genotype"] ?: "",
                    bloodGroup = params["bloodGroup"] ?: "",
                    chestXray = params["chestXray"] ?: ""
                )

                // Validate medical history
                if (medicalHistory.height <= 0.0 || medicalHistory.weight <= 0.0) {
                    errors.add("Height and weight must be valid positive values.")
                }

                if (errors.isEmpty()) {
                    val updateResult = updateEmrPatientMedicalHistory(medicalHistory)
                    if (updateResult) {
                        session.errors.add("Medical history updated successfully.")
                        updateStatus = "Patient medical history saved successfully."
                    } else {
                        errors.add("Error updating medical history.")
                    }
                }
            }

            else -> {
                errors.add("Invalid update type.")
                updateStatus = "Invalid update type."
                println("Error: $updateStatus")
            }
        }

        // Set the session data
        val updatedSession = UpdatePatientSession(errors, updateStatus)
        call.sessions.set(updatedSession)  // Set the session without a name

        println("Debug: errors=${updatedSession.errors}, updateStatus=${updatedSession.updateStatus}")
        // Redirect to the Thymeleaf edit-patient page
        val patientRegNo = params["regNo"]
        call.respondRedirect("/emr/edit-patient/$patientRegNo")
    }




}


