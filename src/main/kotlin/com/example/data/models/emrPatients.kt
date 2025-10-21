package com.example.data.models

import com.example.data.database.EmrPatientMedicalHistory
import com.example.data.database.EmrPatients
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.data.database.EmrPatientsKins
import com.example.data.database.EmrPatientsGuardians
import org.jetbrains.exposed.sql.*


@Serializable
data class Patient(
    val regNo:  String,
    val surName: String, // Surname
    val firstName: String, // First Name
    val middleName: String? = null, // Middle Name (nullable)
    val school: String, // School
    val department: String, // Department
    val phoneNo: String, // Phone Number
    val email: String, // Phone Number
    val dob: String, // Date of Birth (consider using LocalDate)
    val sex: String, // Sex (e.g., "Male", "Female")
    val maritalStatus: String, // Marital Status (e.g., "Single", "Married")
    val hostelAddress: String? = null, // Hostel Address (nullable)
    val homeTown: String, // Hometown
    val lga: String, // Local Government Area
    val state: String, // State
    val country: String // Country
)

@Serializable
data class EmrPatientsKin(
    val regNo: String, // Registration Number
    val name: String, // Kin's Name
    val phoneNo: String, // Office Address
    val residentialAddress: String, // Residential Address
    val occupation: String, // Occupation
    val relationToStudent: String // Relation to Student
)

@Serializable
data class EmrPatientsGuardian(
    val regNo: String, // Registration Number
    val name: String, // Guardian's Name
    val phoneNo: String, // Phone Number
    val officeAddress: String, // Office Address
    val residentialAddress: String, // Residential Address
    val occupation: String // Occupation
)

@Serializable
data class EmrPatientMedicalHistoryData(
    val regNo: String, // student reg no
    val familyHistory: String, // Family history of illnesses
    val previousIllness: String, // Details of previous illnesses
    val height: Double, // Height in meters or centimeters
    val weight: Double, // Weight in kilograms
    val visualAcuity: String, // Visual acuity description or measurement
    val cardiovascularHealth: String, // Information about heart health
    val hearingCondition: String, // Description of hearing condition
    val abdominalHealth: String, // Description of abdominal health status
    val respiratoryHealth: String, // Description of respiratory condition
    val extremities: String,
    val psychiatricState: String,
    val genotype: String,
    val bloodGroup: String,
    val chestXray: String
    )

@Serializable
data class CompletePatientData(
    val patient: Patient,
    val guardian: EmrPatientsGuardian?,
    val kin: EmrPatientsKin?,
    val medicalHistory: EmrPatientMedicalHistoryData?
)


fun addPatient(patient: Patient): Boolean {
    return transaction {
        try {
            // Insert the new patient record into the EmrPatients table
            EmrPatients.insert {
                it[surName] = patient.surName
                it[firstName] = patient.firstName
                it[middleName] = patient.middleName // nullable
                it[school] = patient.school
                it[email] = patient.email
                it[department] = patient.department
                it[phoneNo] = patient.phoneNo
                it[dob] = patient.dob
                it[sex] = patient.sex
                it[maritalStatus] = patient.maritalStatus
                it[hostelAddress] = patient.hostelAddress // nullable
                it[homeTown] = patient.homeTown
                it[lga] = patient.lga
                it[state] = patient.state
                it[country] = patient.country
                it[regNo] = patient.regNo
            }

            // Add a record in EmrPatientsKins table with reg_no only
            EmrPatientsKins.insert {
                it[regNo] = patient.regNo
                it[name] = "" // Leave other fields empty
                it[phoneNo] = ""
                it[residentialAddress] = ""
                it[occupation] = ""
                it[relationToStudent] = ""
            }

            // Add a record in EmrPatientsGuardians table with reg_no only
            EmrPatientsGuardians.insert {
                it[regNo] = patient.regNo
                it[name] = "" // Leave other fields empty
                it[phoneNo] = ""
                it[officeAddress] = ""
                it[residentialAddress] = ""
                it[occupation] = ""
            }

            // Add a record in EmrPatientMedicalHistory table with reg_no only
            EmrPatientMedicalHistory.insert {
                it[regNo] = patient.regNo // Assuming 'regNo' is passed from the patient object
                it[familyHistory] = "" // Leave empty for now; replace with actual data if available
                it[previousIllness] = "" // Leave empty for now; replace with actual data if available
                it[height] = 0.toBigDecimal() // Default value for height
                it[weight] = 0.toBigDecimal() // Default value for weight
                it[visualAcuity] = "" // Leave empty for now; replace with actual data if available
                it[cardiovascularHealth] = "" // Leave empty for now; replace with actual data if available
                it[hearingCondition] = "" // Leave empty for now; replace with actual data if available
                it[extremities] = "" // Leave empty for now; replace with actual data if available
                it[abdominalHealth] = "" // Leave empty for now; replace with actual data if available
                it[respiratoryHealth] = "" // Leave empty for now; replace with actual data if available
                it[psychiatricState] = "" // Leave empty for now; replace with actual data if available
                it[genotype] = "" // Leave empty for now; replace with actual data if available
                it[bloodGroup] = "" // Leave empty for now; replace with actual data if available
                it[chestXray] = "" // Leave empty for now; replace with actual data if available
            }


            println("Patient ${patient.firstName} ${patient.surName} and associated records added successfully.")
            true // Return true if all insertions were successful
        } catch (e: Exception) {
            // Handle any errors that occur during the insertion
            println("Error adding patient and associated records: ${e.message}")
            false // Return false if there was an error
        }
    }
}

fun doesPatientExist(regNo: String): Boolean {
    return transaction {
        // Check if a record exists with the given regNo
        EmrPatients
            .selectAll().where { EmrPatients.regNo eq regNo }
            .limit(1)
            .any() // Returns true if at least one matching record is found, false otherwise
    }
}

fun updatePatient(patient: Patient): Boolean {
    return transaction {
        try {
            EmrPatients.update({ EmrPatients.regNo eq patient.regNo }) {
                it[surName] = patient.surName
                it[firstName] = patient.firstName
                it[middleName] = patient.middleName // nullable
                it[school] = patient.school
                it[department] = patient.department
                it[phoneNo] = patient.phoneNo
                it[email] = patient.email
                it[sex] = patient.sex
                it[maritalStatus] = patient.maritalStatus
                it[hostelAddress] = patient.hostelAddress // nullable
                it[homeTown] = patient.homeTown
                it[lga] = patient.lga
                it[state] = patient.state
                it[country] = patient.country
            }
            println("Patient ${patient.firstName} ${patient.surName} updated successfully.")
            true
        } catch (e: Exception) {
            println("Error updating patient: ${e.message}")
            false
        }
    }
}

fun updateKin(kin: EmrPatientsKin): Boolean {
    return transaction {
        try {
            EmrPatientsKins.update({ EmrPatientsKins.regNo eq kin.regNo }) {
                it[regNo] = kin.regNo
                it[name] = kin.name
                it[phoneNo] = kin.phoneNo
                it[residentialAddress] = kin.residentialAddress
                it[occupation] = kin.occupation
                it[relationToStudent] = kin.relationToStudent
            }
            println("Kin ${kin.name} updated successfully.")
            true
        } catch (e: Exception) {
            println("Error updating kin: ${e.message}")
            false
        }
    }
}

fun updateGuardian(guardian: EmrPatientsGuardian): Boolean {
    return transaction {
        try {
            EmrPatientsGuardians.update({ EmrPatientsGuardians.regNo eq guardian.regNo }) {
                it[regNo] = guardian.regNo
                it[name] = guardian.name
                it[phoneNo] = guardian.phoneNo
                it[officeAddress] = guardian.officeAddress
                it[residentialAddress] = guardian.residentialAddress
                it[occupation] = guardian.occupation
            }
            println("Guardian ${guardian.name} updated successfully.")
            true
        } catch (e: Exception) {
            println("Error updating guardian: ${e.message}")
            false
        }
    }
}

fun updateEmrPatientMedicalHistory(data: EmrPatientMedicalHistoryData): Boolean {
    return transaction {
        try {
            EmrPatientMedicalHistory.update({ EmrPatientMedicalHistory.regNo eq data.regNo }) {
                it[familyHistory] = data.familyHistory
                it[previousIllness] = data.previousIllness
                it[height] = data.height.toBigDecimal()
                it[weight] = data.weight.toBigDecimal()
                it[visualAcuity] = data.visualAcuity
                it[cardiovascularHealth] = data.cardiovascularHealth
                it[hearingCondition] = data.hearingCondition
                it[abdominalHealth] = data.abdominalHealth
                it[respiratoryHealth] = data.respiratoryHealth
                it[bloodGroup] = data.bloodGroup
                it[genotype] = data.genotype
                it[extremities] = data.extremities
                it[chestXray] = data.chestXray
                it[psychiatricState] = data.psychiatricState
            }
            println("Patient medical history updated successfully.")
            true
        } catch (e: Exception) {
            println("Error updating patient medical history: ${e.message}")
            false
        }
    }
}

fun fetchCompletePatientData(regNo: String): CompletePatientData? {
    return transaction {
        // Fetch main patient data
        val patientData = EmrPatients.selectAll().where { EmrPatients.regNo eq regNo }
            .singleOrNull()?.let {
                Patient(
                    regNo = it[EmrPatients.regNo],
                    surName = it[EmrPatients.surName],
                    firstName = it[EmrPatients.firstName],
                    middleName = it[EmrPatients.middleName],
                    school = it[EmrPatients.school],
                    department = it[EmrPatients.department],
                    phoneNo = it[EmrPatients.phoneNo],
                    email = it[EmrPatients.email],
                    dob = it[EmrPatients.dob],
                    sex = it[EmrPatients.sex],
                    maritalStatus = it[EmrPatients.maritalStatus],
                    hostelAddress = it[EmrPatients.hostelAddress],
                    homeTown = it[EmrPatients.homeTown],
                    lga = it[EmrPatients.lga],
                    state = it[EmrPatients.state],
                    country = it[EmrPatients.country]
                )
            } ?: return@transaction null // If no patient is found, return null

        // Fetch guardian data
        val guardianData = EmrPatientsGuardians.selectAll().where { EmrPatientsGuardians.regNo eq regNo }
            .singleOrNull()?.let {
                EmrPatientsGuardian(
                    regNo = it[EmrPatientsGuardians.regNo],
                    name = it[EmrPatientsGuardians.name],
                    phoneNo = it[EmrPatientsGuardians.phoneNo],
                    officeAddress = it[EmrPatientsGuardians.officeAddress],
                    residentialAddress = it[EmrPatientsGuardians.residentialAddress],
                    occupation = it[EmrPatientsGuardians.occupation]
                )
            }

        // Fetch kin data
        val kinData = EmrPatientsKins.selectAll().where { EmrPatientsKins.regNo eq regNo }
            .singleOrNull()?.let {
                EmrPatientsKin(
                    regNo = it[EmrPatientsKins.regNo],
                    name = it[EmrPatientsKins.name],
                    phoneNo= it[EmrPatientsKins.phoneNo],
                    residentialAddress = it[EmrPatientsKins.residentialAddress],
                    occupation = it[EmrPatientsKins.occupation],
                    relationToStudent = it[EmrPatientsKins.relationToStudent]
                )
            }

        // Fetch medical history data
        val medicalHistoryData = EmrPatientMedicalHistory.selectAll().where { EmrPatientMedicalHistory.regNo eq regNo }
            .singleOrNull()?.let {
                EmrPatientMedicalHistoryData(
                    regNo = it[EmrPatientMedicalHistory.regNo],
                    familyHistory = it[EmrPatientMedicalHistory.familyHistory],
                    previousIllness = it[EmrPatientMedicalHistory.previousIllness],
                    height = it[EmrPatientMedicalHistory.height].toDouble(),
                    weight = it[EmrPatientMedicalHistory.weight].toDouble(),
                    visualAcuity = it[EmrPatientMedicalHistory.visualAcuity],
                    cardiovascularHealth = it[EmrPatientMedicalHistory.cardiovascularHealth],
                    hearingCondition = it[EmrPatientMedicalHistory.hearingCondition],
                    abdominalHealth = it[EmrPatientMedicalHistory.abdominalHealth],
                    respiratoryHealth = it[EmrPatientMedicalHistory.respiratoryHealth],
                    extremities = it[EmrPatientMedicalHistory.extremities],
                    psychiatricState = it[EmrPatientMedicalHistory.psychiatricState],
                    genotype = it[EmrPatientMedicalHistory.genotype],
                    bloodGroup = it[EmrPatientMedicalHistory.bloodGroup],
                    chestXray = it[EmrPatientMedicalHistory.chestXray]
                )
            }

        // Combine all data into CompletePatientData
        CompletePatientData(
            patient = patientData,
            guardian = guardianData,
            kin = kinData,
            medicalHistory = medicalHistoryData
        )
    }
}

