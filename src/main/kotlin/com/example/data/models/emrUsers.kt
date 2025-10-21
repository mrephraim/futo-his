package com.example.data.models

import com.example.data.database.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val type: String,
)
data class Doctor(
    val doctorId: String, // Unique doctor identifier
    val name: String, // Doctor's name
    val specialization: String, // Area of expertise
    val phoneNo: String, // Contact number
    val email: String, // Email address
    val officeLocation: String // Office location
)



fun addUser(username: String, passwordHash: String, type: String): User? {
    return transaction {
        EmrUsers.insert {
            it[EmrUsers.username] = username
            it[EmrUsers.passwordHash] = passwordHash
            it[EmrUsers.type] = type
        }.resultedValues?.singleOrNull()?.let {
            User(
                id = it[EmrUsers.id],
                username = it[EmrUsers.username],
                passwordHash = it[EmrUsers.passwordHash],
                type = it[EmrUsers.type],
            )
        }
    }
}

fun findUserByUsername(username: String): User? {
    return transaction {
        // Prepare the SQL query to select the user by username
        val resultSet = exec("""
            SELECT id, username, password_hash, type
            FROM emrusers
            WHERE username = '$username'
        """) { resultSet ->
            // Check if the result set has at least one row
            if (resultSet.next()) {
                // Create and return a User object from the result
                User(
                    id = resultSet.getInt("id"),
                    username = resultSet.getString("username"),
                    passwordHash = resultSet.getString("password_hash"),
                    type = resultSet.getString("type"),
                )
            } else {
                null // Return null if no user was found
            }
        }
        resultSet // This will either be a User object or null
    }
}

fun addDoctor(createDoctorRequest: Doctor): Doctor? {
    return transaction {
        EmrDoctors.insert {
            it[doctorId] = createDoctorRequest.doctorId
            it[name] = createDoctorRequest.name
            it[specialization] = createDoctorRequest.specialization
            it[phoneNo] = createDoctorRequest.phoneNo
            it[email] = createDoctorRequest.email
            it[officeLocation] = createDoctorRequest.officeLocation
        }.resultedValues?.singleOrNull()?.let {
            Doctor(
                doctorId = it[EmrDoctors.doctorId],
                name = it[EmrDoctors.name],
                specialization = it[EmrDoctors.specialization],
                phoneNo = it[EmrDoctors.phoneNo],
                email = it[EmrDoctors.email],
                officeLocation = it[EmrDoctors.officeLocation],
            )
        }
    }
}


@Serializable
data class PatientBasicInfo(val firstName: String, val surName: String)

fun fetchPatientBasicInfo(regNo: String): PatientBasicInfo? {
    return transaction {
        EmrPatients
            .selectAll().where { EmrPatients.regNo eq regNo } // Correctly apply the filter here
            .map {
                PatientBasicInfo(
                    firstName = it[EmrPatients.firstName],
                    surName = it[EmrPatients.surName]
                )
            }
            .singleOrNull() // Return a single result or null if no match
    }
}


// Function to fetch all doctors
fun fetchAllDoctors(): List<Map<String, String>> {
    return transaction {
        EmrDoctors.selectAll()
            .map {
                mapOf(
                    "id" to it[EmrDoctors.doctorId],
                    "name" to it[EmrDoctors.name],
                     "specialization" to it[EmrDoctors.specialization]
                )
            }
    }
}
