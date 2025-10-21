package com.example.data.database

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.math.BigDecimal



object InventoryBatches : IntIdTable("inventory_batches") {
    val drugId = reference("drug_id", Drugs)
    val batchNumber = varchar("batch_number", 128).nullable() // supplier/manufacturer lot
    val manufactureDate = date("manufacture_date").nullable()
    val expiryDate = date("expiry_date").nullable()
    val quantityUom = decimal("quantity_uom", 20, 6) // stored in base unit (e.g., tablets, mg, mL)
    val remainingUom = decimal("remaining_uom", 20, 6)
    val unitCost = decimal("unit_cost", 18, 6).nullable() // cost per base unit
    val currency = varchar("currency", 12).default("NGN")
    val supplierId = integer("supplier_id").nullable()
    val packageId = integer("package_id")
    val storageLocationId = integer("storage_location_id").nullable()
    val receivedAt = datetime("received_at").default(Clock.System.now().toLocalDateTime(TimeZone.UTC))
    val createdBy = integer("created_by").nullable()
    val quarantined = bool("quarantined").default(false)
}

