package com.example.data.models

import com.example.data.database.PisPersonnel
import com.example.data.database.PisUsers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class PisUser(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val type: String,
)

@Serializable
data class CreatePisAdminRequest(
    val username: String,
    val password: String
)

@Serializable
data class CreatePisPersonnelRequest(
    val username: String,
    val password: String,
    val name: String,
    val specialization: String,
    val email: String
)


fun findPisUser(username: String): PisUser? {
    return transaction {
        // Prepare the SQL query to select the user by username
        val resultSet = exec("""
            SELECT id, username, password_hash, type
            FROM pis_users
            WHERE username = '$username'
        """) { resultSet ->
            // Check if the result set has at least one row
            if (resultSet.next()) {
                // Create and return a User object from the result
                PisUser(
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

fun insertPisUser(username: String, userType: String, passwordHash: String): Int? {
    var generatedId: Int? = null
    transaction {
        // Insert user into PisUsers table
        PisUsers.insert {
            it[PisUsers.username] = username
            it[PisUsers.type] = userType
            it[PisUsers.passwordHash] = passwordHash
        }

        // Retrieve the last inserted ID based on the username
        generatedId = PisUsers
            .selectAll().where { PisUsers.username eq username }
            .map { it[PisUsers.id] }
            .firstOrNull()
    }
    return generatedId
}

fun insertPisPersonnel(
    attendantId: Int, // This matches the ID from LisUsers
    name: String,
    specialization: String,
    email: String
) {
    transaction {
        PisPersonnel.insert {
            it[PisPersonnel.attendantId] = attendantId.toString()
            it[PisPersonnel.name] = name
            it[PisPersonnel.specialization] = specialization
            it[PisPersonnel.email] = email
        }
    }
}