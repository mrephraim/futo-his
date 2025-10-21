package com.example.data.database

import com.example.data.database.LabParameters.default
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object LabCategories : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val description = text("description")
    val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
    override val primaryKey = PrimaryKey(id)
}


    object LabParameters : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 100)
        val dataType = varchar("data_type", 50)
        val description = text("description")
        val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
        override val primaryKey = PrimaryKey(id)
    }

object LabParameterUnits : Table() {
    val id = integer("id").autoIncrement()
    val parameterId = integer("parameter_id").references(LabParameters.id)
    val name = varchar("unit_name", 50)
    val base = varchar("base_unit", 50)
    val factor = double("conversion_factor")
    override val primaryKey = PrimaryKey(id)
}

object LabParameterComments : Table() {
    val id = integer("id").autoIncrement()
    val parameterId = integer("parameter_id").references(LabParameters.id)
    val comment = text("comment")
    override val primaryKey = PrimaryKey(id)
}

// Define the LabSampleTable object
object LabSampleTable : Table("lab_sample") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description")
    override val primaryKey = PrimaryKey(id)
}

//finally the labtests
object LabTests : Table() {
    val id = integer("id").autoIncrement()
    val testName = varchar("test_name", 255)
    val testCategory = integer("test_category").references(LabCategories.id)
    val bsl = integer( "biosecurity_level")
    val parameters = array<Int>("parameters") // Requires PostgreSQL extension for array
    val sampleTypes = array<Int>("sample_types") // Requires PostgreSQL extension for array
    val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
    override val primaryKey = PrimaryKey(id)
}
