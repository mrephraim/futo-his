package com.example.data.database

import com.example.data.database.PrescriptionMedications.nullable
import com.example.data.database.PrescriptionMedications.references
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

// Main prescription table
object Prescriptions : IntIdTable("prescriptions") {
    val patientId = varchar("patient_id", 50)
    val prescriberId = integer("prescriber_id")
    val appointmentId = integer("appointment_id").references(EmrAppointments.id).nullable()
    val prescriberDept = enumerationByName("prescriber_dept", 20, PrescriberDept::class)
    val status = enumerationByName("status", 20, PrescriptionStatus::class)
    val prescribedAt = datetime("prescribed_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
}

// Medications linked to prescription
object PrescriptionMedications : IntIdTable("prescription_medications") {
    val prescriptionId = reference("prescription_id", Prescriptions)
    val drugId = integer("drug_id") // links to Drug table
    val dosageQuantity = double("dosage_quantity")
    val packageId = integer("package_id").references(DrugPackages.id)
    val intakeInterval = integer("intake_interval")
    val durationValue = integer("duration_value")
    val durationUnit = integer("duration_unit")
    val instruction = varchar("instruction", 255).nullable()
}

enum class PrescriberDept {
    PIS, // Pharmacy Information System
    LIS, // Lab Information System
    EMR, // Electronic Medical Record
    CAD  // Cardiology, or any other dept
}

enum class PrescriptionStatus {
    PRESCRIBED,
    ONGOING,
    COMPLETED
}
