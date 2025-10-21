package com.example.data.models

import com.example.data.database.EmrAppointments
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class CreateAppointmentRequest(
    val patientRegNo: String,
    val doctorId: String,
    val appointmentDate: String,
    val startTime: String,
    val duration: String,
    val department: String,
    val type: String,
    val status: Int
)

@Serializable
data class UpdateAppointmentRequest(
    val appointmentId: Int? = null, // Optional, will be retrieved using patientRegNo if null
    val status: Int? = null,
    val bloodPressure: String? = null,
    val pulseRate: String? = null,
    val temperature: String? = null,
    val respiratoryRate: String? = null,
    val oxygenSaturation: String? = null,
    val specialNotes: String? = null
)


fun createAppointment(request: CreateAppointmentRequest): Boolean {
    val startTime = request.startTime  // "HH:mm" format
    val duration = request.duration.toInt()  // Duration in minutes, e.g., 15, 30, etc.

    // Split the start time into hours and minutes
    val startParts = startTime.split(":")
    val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()

    // Calculate the end time by adding the duration in minutes
    val endMinutes = startMinutes + duration
    val endHours = endMinutes / 60
    val endMinutesOnly = endMinutes % 60

    // Format the end time in "HH:mm" format
    val endTime = String.format("%02d:%02d", endHours, endMinutesOnly)

    // Perform database transaction
    return transaction {
        val rowsInserted = EmrAppointments.insert {
            it[patientRegNo] = request.patientRegNo
            it[doctorId] = request.doctorId
            it[appointmentDate] = request.appointmentDate
            it[beginTime] = startTime  // Already in "HH:mm"
            it[finishTime] = endTime   // Calculated end time in "HH:mm"
            it[type] = request.type
            it[department] = request.department
            it[status] = 1  // Initial status
        }
        rowsInserted.insertedCount > 0
    }
}

fun addPatientVitals(patientRegNo: String, vitals: UpdateAppointmentRequest): Boolean {
    return transaction {
        val appointmentId = vitals.appointmentId ?: EmrAppointments
            .selectAll().where { EmrAppointments.patientRegNo eq patientRegNo }
            .orderBy(EmrAppointments.id, SortOrder.DESC)
            .limit(1)
            .map { it[EmrAppointments.id] }
            .firstOrNull()

        if (appointmentId != null) {
            val rowsUpdated = EmrAppointments.update({ EmrAppointments.id eq appointmentId }) {
                vitals.bloodPressure?.let { bp -> it[bloodPressure] = bp }
                vitals.pulseRate?.let { pr -> it[pulseRate] = pr }
                vitals.temperature?.let { temp -> it[temperature] = temp }
                vitals.respiratoryRate?.let { rr -> it[respiratoryRate] = rr }
                vitals.oxygenSaturation?.let { os -> it[oxygenSaturation] = os }
                vitals.specialNotes?.let { notes -> it[specialNotes] = notes }
                it[status] = 2 // Update status to 2 when vitals are added
            }
            rowsUpdated > 0
        } else {
            false // No matching appointment found
        }
    }
}

