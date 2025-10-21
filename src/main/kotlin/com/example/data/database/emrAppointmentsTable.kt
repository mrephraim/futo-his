package com.example.data.database


import org.jetbrains.exposed.sql.Table


object EmrAppointments : Table() {
    val id = integer("id").autoIncrement()
    val patientRegNo = varchar("patient_reg_no", 11) // Patient Registration Number
    val doctorId = varchar("doctor_id", 20) // Doctor's ID
    val appointmentDate = varchar("appointment_date", 20) // Appointment Date
    val beginTime = varchar("start_time", 20) // Start Time (HH:mm)
    val finishTime = varchar("end_time", 20) // End Time (HH:mm)
    val type = varchar("type", 20) // End Time (HH:mm)
    val department = varchar("department", 20)
    val bloodPressure = varchar("blood_pressure", 10).nullable() // Blood Pressure (e.g., "120/80")
    val pulseRate = varchar("pulse_rate", 10).nullable() // Pulse Rate
    val temperature = varchar("temperature", 10).nullable() // Temperature in Celsius
    val respiratoryRate = varchar("respiratory_rate", 10).nullable() // Respiratory Rate
    val oxygenSaturation = varchar("oxygen_saturation", 10).nullable() // Oxygen Saturation (SpO2)
    val specialNotes = text("special_notes").nullable() // Space for Additional Notes
    val status = integer("status") // Appointment Status

    override val primaryKey = PrimaryKey(id)
}

object LabOrders : Table() {
    val id = integer("id").autoIncrement()
    val appointmentId = integer("appointment_id").references(EmrAppointments.id) // Link to appointment
    val testCategory = integer("test_category") // Category of the test
    val testType = integer("test_type") // Specific test ID
    val priority = varchar("priority", 10) // Priority level (e.g., "normal", "urgent")
    val comment = text("comment").nullable() // Additional comments
    val status = varchar("status", 20).default("pending") // Status of the test (pending, completed, etc.)

    override val primaryKey = PrimaryKey(id)
}



