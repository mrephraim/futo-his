package com.example.data.database

import com.example.data.database.LabCategories.default
import com.example.data.database.LabSampleTable.autoIncrement
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

// Define the LabSampleTable object
object DrugBatches : Table("drug_batches") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description")
    override val primaryKey = PrimaryKey(id)
}

object DrugCategories : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val description = text("description")
    val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
    override val primaryKey = PrimaryKey(id)
}

object DrugGenerics : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val description = text("description")
    val createdAt = datetime("created_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
    override val primaryKey = PrimaryKey(id)
}

object Drugs : IntIdTable("drugs") {
    val name = varchar("name", 255)
    val formId = reference("form_id", DosageForms)
    val categoryId = reference("category_id", DrugCategories.id) // new
    val genericId = reference("generic_id", DrugGenerics.id)     // new
    val description = text("description").nullable()
    val active = bool("active").default(true)
}



object DrugParameters : IntIdTable("drug_parameters") {
    val drugId = reference("drug_id", Drugs)
    val name = varchar("name", 255) // e.g., Strength, Volume, Concentration
    val value = decimal("value", 12, 4) // flexible precision for dosages
    val unitId = reference("unit_id", Units)
    val description = text("description").nullable()
}

object DosageForms : IntIdTable("dosage_forms") {
    val name = varchar("name", 100) // Tablet, Syrup, Injection
    val example = varchar("example", 255).nullable()
}

object Units : IntIdTable("units") {
    val name = varchar("name", 50) // mg, g, mL, IU
    val baseUnitId = optReference("base_unit_id", Units) // self-reference
    val factor = decimal("factor", 12, 6) // conversion factor to base unit
    val category = varchar("category", 50) // weight, volume, count, IU
}


object DrugPackages : IntIdTable("drug_packages") {
    val drugId = reference("drug_id", Drugs)                 // Link to drug
    val packageName = varchar("package_name", 100)           // e.g., Tablet, Strip, Box, Bottle
    val subPackageId = reference("sub_package_id", DrugPackages).nullable()                                          // What this package is made of (NULL = base package)
    val formId = reference("form_id", DosageForms)           // Tablet, Capsule, Syrup, etc.
    // Strength of the smallest unit (only required if this is the base unit, e.g., Tablet 500 mg)
    val strengthValue = decimal("strength_value", 10, 2).nullable()
    val strengthUnitId = reference("strength_unit_id", Units).nullable()

    // How many sub-packages or units make up this package
    val quantity = integer("quantity")                       // e.g., 10 tablets per strip, 6 strips per box
}





