package com.example.data.database

import org.jetbrains.exposed.sql.Table

object LisUsers : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val type = varchar("type", 50)
    val passwordHash = varchar("password_hash", 64)
    override val primaryKey = PrimaryKey(id)
}

object LabAttendants : Table("lab_attendants") {
    val attendantId = varchar("attendant_id", 20).uniqueIndex() // Unique Doctor ID
    val name = varchar("name", 100) // Doctor's Name
    val specialization = varchar("specialization", 100) // Area of Expertise
    val email = varchar("email", 100) // Email Address
}