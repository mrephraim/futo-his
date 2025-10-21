package com.example.data.database

import org.jetbrains.exposed.sql.Table

object EmrUsers : Table() {
        val id = integer("id").autoIncrement()
        val username = varchar("username", 50).uniqueIndex()
        val type = varchar("type", 50)
        val passwordHash = varchar("password_hash", 64)
        override val primaryKey = PrimaryKey(id)
}

object EmrDoctors : Table() {
    val doctorId = varchar("doctor_id", 20).uniqueIndex() // Unique Doctor ID
    val name = varchar("name", 100) // Doctor's Name
    val specialization = varchar("specialization", 100) // Area of Expertise
    val phoneNo = varchar("phone_no", 15) // Contact Number
    val email = varchar("email", 100) // Email Address
    val officeLocation = varchar("office_location", 255) // Office Address/Location
}