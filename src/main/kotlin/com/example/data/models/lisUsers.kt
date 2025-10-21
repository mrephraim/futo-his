package com.example.data.models

import com.example.data.database.LabAttendants
import com.example.data.database.LisUsers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class LisUser(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val type: String,
)

@Serializable
data class CreateLisAdminRequest(
    val username: String,
    val password: String
)

@Serializable
data class CreateLabAttendantRequest(
    val username: String,
    val password: String,
    val name: String,
    val specialization: String,
    val email: String
)


fun findLisUser(username: String): LisUser? {
    return transaction {
        // Prepare the SQL query to select the user by username
        val resultSet = exec("""
            SELECT id, username, password_hash, type
            FROM lisusers
            WHERE username = '$username'
        """) { resultSet ->
            // Check if the result set has at least one row
            if (resultSet.next()) {
                // Create and return a User object from the result
                LisUser(
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

fun insertLisUser(username: String, userType: String, passwordHash: String): Int? {
    var generatedId: Int? = null
    transaction {
        // Insert user into LisUsers table
        LisUsers.insert {
            it[LisUsers.username] = username
            it[LisUsers.type] = userType
            it[LisUsers.passwordHash] = passwordHash
        }

        // Retrieve the last inserted ID based on the username
        generatedId = LisUsers
            .selectAll().where { LisUsers.username eq username }
            .map { it[LisUsers.id] }
            .firstOrNull()
    }
    return generatedId
}
fun insertLabAttendant(
    attendantId: Int, // This matches the ID from LisUsers
    name: String,
    specialization: String,
    email: String
) {
    transaction {
        LabAttendants.insert {
            it[LabAttendants.attendantId] = attendantId.toString()
            it[LabAttendants.name] = name
            it[LabAttendants.specialization] = specialization
            it[LabAttendants.email] = email
        }
    }
}


