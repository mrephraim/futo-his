package com.example.data.database

import com.example.data.database.LabTests.default
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Requisitions : Table("requisitions") {
    val id = integer("id").autoIncrement()
    val patientRegNo = varchar("patient_reg_no", 11) // Patient Registration Number
    val requestFormat = varchar("request_format", 50) // Request Format: "physician" or "direct"
    val physicianId = integer("physician_id").nullable() // Nullable for direct entry
    val authorizingOfficer = integer("authorizing_officer") // Person logging the requisition
    val testCategory = varchar("test_category", 100) // Lab Test Category Name
    val labTest = varchar("lab_test", 255) // Specific Lab Test Name
    val labTestId = integer("lab_test_id") // Specific Lab Test
    val sampleType = varchar("sample_type", 100) // Sample Type
    val sampleId = integer("sample_id")
    val collectionDateTime = datetime("collection_date_time") // Sample Collection Date & Time
    val priority = varchar("priority", 50) // Priority: "routine", "urgent", or "stat"
    val clinicalNotes = text("clinical_notes").nullable() // Additional notes (optional)
    val rstatus = integer("status") // Additional notes (optional)
    val orderId = integer("order_id").nullable() // order_id
    override val primaryKey = PrimaryKey(id)
}

object LabResults : Table("lab_results") {
    val id = integer("id").autoIncrement() // Primary key
    val requisitionId = integer("r_id")
    val parameters = text("parameters") // Store parameters as a JSON string
    val comments = text("comments") // Store comments as a JSON string
    val lastUpdated = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))

    override val primaryKey = PrimaryKey(id)
}
