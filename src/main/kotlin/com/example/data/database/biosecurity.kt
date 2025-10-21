package com.example.data.database

import org.jetbrains.exposed.dao.id.IntIdTable

object BioHazardIncidents : IntIdTable() {
    val requisition = integer("requisition")
    val labOfficer = integer("officer_involved")
    val level = varchar("level", 10)
    val hazardDescription = text("hazard_description")
}