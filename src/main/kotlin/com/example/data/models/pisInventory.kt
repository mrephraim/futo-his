package com.example.data.models

import com.example.data.database.*
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

@Serializable
data class ReceiveStockRequest(
    val drugId: Int,
    val packageId: Int,
    val quantityOfPackage: Int,
    val batchNumber: String,
    val manufactureDate: String? = null,   // ISO string (yyyy-MM-dd)
    val expiryDate: String? = null,
    val unitCostPerPackage: String,
    val receivedBy: Int? = null            // optional, can be taken from session
)


fun receiveStock(
    drugId: Int,
    packageId: Int,
    batchNumber: String?,
    manufactureDate: LocalDate?,
    expiryDate: LocalDate?,
    quantityOfPackage: Int,
    unitCostPerPackage: BigDecimal?,
    receivedBy: Int?
) {
    transaction {
        // Convert to base units
        val quantityInBase = calculateBaseUnits(packageId, quantityOfPackage)
        val basePackageId = findBasePackageId(packageId)
        val uniCost = unitCostPerPackage?.div(quantityInBase)

        InventoryBatches.insertAndGetId {
            it[InventoryBatches.drugId] = drugId
            it[InventoryBatches.packageId] = basePackageId
            it[InventoryBatches.batchNumber] = batchNumber
            it[InventoryBatches.manufactureDate] = manufactureDate
            it[InventoryBatches.expiryDate] = expiryDate
            it[quantityUom] = quantityInBase
            it[remainingUom] = quantityInBase
            it[unitCost] = uniCost
        }
    }
}

fun findBasePackageId(packageId: Int): Int {
    return transaction {
        var currentId = packageId
        while (true) {
            val pkg = DrugPackages
                .selectAll()
                .where { DrugPackages.id eq currentId }
                .singleOrNull() ?: break

            val subId = pkg[DrugPackages.subPackageId]?.value
            if (subId == null) {
                // This is the base package
                return@transaction currentId
            } else {
                // Move down to sub-package
                currentId = subId
            }
        }
        currentId
    }
}


fun calculateBaseUnits(packageId: Int, quantityOfPackage: Int): BigDecimal {
    var totalUnits = BigDecimal(quantityOfPackage)
    var currentPackageId: Int? = packageId

    transaction {
        while (currentPackageId != null) {
            val pkg = DrugPackages
                .selectAll().where { DrugPackages.id eq currentPackageId!! }
                .singleOrNull()

            if (pkg != null) {
                val subPackageId = pkg[DrugPackages.subPackageId]
                val qtyPerSub = pkg[DrugPackages.quantity]

                if (subPackageId == null) {
                    // Base package reached → multiply once and stop
                    totalUnits *= BigDecimal.ONE
                    currentPackageId = null
                } else {
                    totalUnits *= BigDecimal(qtyPerSub)
                    currentPackageId = subPackageId.value
                }
            } else {
                error("Package with ID $currentPackageId not found")
            }
        }
    }

    return totalUnits
}

fun calculateTotalCost(packageId: Int, quantity: Int): Double {
    return transaction {
        // Get the latest cost entry for this packageId
        val latestCost = InventoryBatches
            .select(InventoryBatches.unitCost)
            .where { InventoryBatches.packageId eq packageId }
            .orderBy(InventoryBatches.receivedAt, SortOrder.DESC)
            .limit(1)
            .map { it[InventoryBatches.unitCost] }
            .firstOrNull()

        if (latestCost != null) {
            (latestCost.toDouble() * quantity)
        } else {
            0.0 // No record found, return 0
        }
    }
}

fun isStockAvailable(drugId: Int, packageId: Int, requiredQty: BigDecimal): Boolean {
    return transaction {
        val totalRemaining = InventoryBatches
            .select(InventoryBatches.remainingUom.sum())
            .where {
                (InventoryBatches.drugId eq drugId) and
                        (InventoryBatches.packageId eq packageId) and
                        (InventoryBatches.quarantined eq false) // exclude quarantined stock
            }
            .firstOrNull()?.getOrNull(InventoryBatches.remainingUom.sum()) ?: BigDecimal.ZERO

        totalRemaining >= requiredQty
    }
}


@Serializable
enum class InventoryStatus {
    OUT_OF_STOCK,
    LOW_STOCK,
    OK
}

@Serializable
data class PackageInventoryInfo(
    val packageId: Int,
    val packageName: String,
    val totalQuantityEntered: String,
    val totalQuantityRemaining: String,
    val latestCost: String?,
    val status: InventoryStatus
)

@Serializable
data class InventoryDrugInfo(
    val drugId: Int,
    val drugName: String,
    val category: String,
    val generic: String,
    val packages: List<PackageInventoryInfo>
)

@Serializable
data class InventoryResponse(
    val success: Boolean,
    val data: List<InventoryDrugInfo>? = null,
    val error: String? = null
)




fun getInventoryDrugDetails(filter: InventoryStatus? = null): List<InventoryDrugInfo> {
    return transaction {
        Drugs.selectAll().mapNotNull { drugRow ->
            val drugId = drugRow[Drugs.id].value
            val drugName = drugRow[Drugs.name]

            val categoryName = DrugCategories
                .selectAll().where { DrugCategories.id eq drugRow[Drugs.categoryId] }
                .singleOrNull()?.get(DrugCategories.name) ?: ""

            val genericName = DrugGenerics
                .selectAll().where { DrugGenerics.id eq drugRow[Drugs.genericId] }
                .singleOrNull()?.get(DrugGenerics.name) ?: ""

            // Get base packages (no subpackage)
            val basePackages = DrugPackages
                .selectAll().where { (DrugPackages.drugId eq drugId) and (DrugPackages.subPackageId.isNull()) }
                .mapNotNull { pkgRow ->
                    val packageId = pkgRow[DrugPackages.id].value
                    val packageName = pkgRow[DrugPackages.packageName]

                    // Inventory batches for this package
                    val batches = InventoryBatches
                        .selectAll().where { InventoryBatches.packageId eq packageId }
                        .orderBy(InventoryBatches.receivedAt to SortOrder.DESC)
                        .toList()

                    if (batches.isEmpty()) return@mapNotNull null

                    val totalQuantityEntered = batches.fold(BigDecimal.ZERO) { acc, row -> acc + row[InventoryBatches.quantityUom] }
                    val totalQuantityRemaining = batches.fold(BigDecimal.ZERO) { acc, row -> acc + row[InventoryBatches.remainingUom] }
                    val latestCost = batches.firstOrNull()?.get(InventoryBatches.unitCost)

                    // 🔎 Benchmark rules
                    val status = when {
                        totalQuantityRemaining <= BigDecimal.ZERO -> InventoryStatus.OUT_OF_STOCK
                        totalQuantityRemaining < (totalQuantityEntered.multiply(BigDecimal("0.2"))) -> InventoryStatus.LOW_STOCK // <20% left
                        else -> InventoryStatus.OK
                    }

                    PackageInventoryInfo(
                        packageId = packageId,
                        packageName = packageName,
                        totalQuantityEntered = totalQuantityEntered.toString(),
                        totalQuantityRemaining = totalQuantityRemaining.toString(),
                        latestCost = latestCost.toString(),
                        status = status
                    )
                }

            // If filter is applied, keep only matching packages
            val filteredPackages = filter?.let { basePackages.filter { it.status == filter } } ?: basePackages

            if (filteredPackages.isEmpty()) {
                null // skip this drug if no package matches filter
            } else {
                InventoryDrugInfo(
                    drugId = drugId,
                    drugName = drugName,
                    category = categoryName,
                    generic = genericName,
                    packages = filteredPackages
                )
            }
        }
    }
}





