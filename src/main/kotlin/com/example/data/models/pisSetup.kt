package com.example.data.models

import com.example.data.database.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

@Serializable
data class NewBatchRequest(
    val name: String,
    val description: String
)


fun addNewDrugBatch(name: String, description: String): Boolean {
    return try {
        transaction{
            DrugBatches.insert {
                it[DrugBatches.name] = name
                it[DrugBatches.description] = description
            }
        }
        true
    } catch (e: Exception) {
        println("Error adding sample type: ${e.message}")
        false
    }
}

fun insertDrugCategory(name: String, description: String): Int {
    return transaction {
        DrugCategories.insert {
            it[DrugCategories.name] = name
            it[DrugCategories.description] = description
        } get DrugCategories.id
    }
}

fun insertDrugGeneric(name: String, description: String): Int {
    return transaction {
        DrugGenerics.insert {
            it[DrugGenerics.name] = name
            it[DrugGenerics.description] = description
        } get DrugGenerics.id
    }
}

@Serializable
data class DrugRequest(
    val name: String,
    val formId: Int,
    val categoryId: Int,
    val genericId: Int,
    val description: String? = null,
    val active: Boolean = true
)

@Serializable
data class DrugParameterRequest(
    val drugId: Int,
    val name: String,
    val value: String,
    val unitId: Int,
    val description: String? = null
)

@Serializable
data class DosageFormRequest(
    val name: String,
    val example: String? = null
)
@Serializable
data class UnitPisRequest(
    val name: String,
    val baseUnitId: Int? = null,
    val factor: String,
    val category: String
)

@Serializable
data class DrugPackageRequest(
    val drugId: Int,
    val packageName: String,
    val quantity: Int,
    val subPackageId: Int? = null,        // optional: if this package is made of another package
    val formId: Int,                      // dosage form (tablet, capsule, injection, etc.)
    val strengthValue: Double? = null,    // only for base units
    val strengthUnitId: Int? = null       // only for base units
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class IdResponse(val id: Int)

@Serializable
data class UnitResponse(
    val id: Int,
    val name: String,
    val baseUnitId: Int? = null,
    val factor: String,
    val category: String
)
@Serializable
data class DosageFormResponse(
    val id: Int,
    val name: String,
    val example: String? = null
)

@Serializable
data class OptionResponse(
    val id: Int,
    val name: String
)

@Serializable
data class DrugResponse(
    val id: Int,
    val name: String,
    val formId: Int,
    val description: String? = null,
    val active: Boolean
)

@Serializable
data class DrugParameterResponse(
    val id: Int,
    val drugId: Int,
    val name: String,
    val value: String,
    val unitId: Int,
    val description: String? = null
)


@Serializable
data class DrugPackageResponse(
    val id: Int,
    val drugId: Int,
    val packageName: String,
    val quantity: Int,
    val subPackageId: Int? = null,
    val formId: Int,
    val strengthValue: Double? = null,
    val strengthUnitId: Int? = null
)







//FOR CONVERSION
//val decimalValue = request.value.toBigDecimal()


fun addDrug(
    name: String,
    formId: Int,
    categoryId: Int,
    genericId: Int,
    description: String? = null,
    active: Boolean = true
): Int {
    return transaction {
        Drugs.insertAndGetId {
            it[Drugs.name] = name
            it[Drugs.formId] = EntityID(formId, DosageForms)
            it[Drugs.categoryId] = categoryId
            it[Drugs.genericId] = genericId
            it[Drugs.description] = description
            it[Drugs.active] = active
        }.value
    }
}


fun addDrugParameter(
    drugId: Int,
    name: String,
    value: BigDecimal,
    unitId: Int,
    description: String? = null
): Int {
    return transaction {
        DrugParameters.insertAndGetId {
            it[DrugParameters.drugId] = EntityID(drugId, Drugs)
            it[DrugParameters.name] = name
            it[DrugParameters.value] = value
            it[DrugParameters.unitId] = EntityID(unitId, Units)
            it[DrugParameters.description] = description
        }.value
    }
}

fun addDosageForm(
    name: String,
    example: String? = null
): Int {
    return transaction {
        DosageForms.insertAndGetId {
            it[DosageForms.name] = name
            it[DosageForms.example] = example
        }.value
    }
}

fun addUnit(
    name: String,
    baseUnitId: Int? = null,
    factor: BigDecimal = BigDecimal.ONE,
    category: String
): Int {
    return transaction {
        Units.insertAndGetId {
            it[Units.name] = name
            it[Units.baseUnitId] = baseUnitId?.let { id -> EntityID(id, Units) }
            it[Units.factor] = factor
            it[Units.category] = category
        }.value
    }
}


fun addDrugPackage(
    drugId: Int,
    packageName: String,
    quantity: Int,
    subPackageId: Int? = null,       // if null → base package
    formId: Int,
    strengthValue: BigDecimal? = null,
    strengthUnitId: Int? = null
): Int {
    return transaction {
        DrugPackages.insertAndGetId {
            it[DrugPackages.drugId] = EntityID(drugId, Drugs)
            it[DrugPackages.packageName] = packageName
            it[DrugPackages.quantity] = quantity
            it[DrugPackages.subPackageId] = subPackageId?.let { id -> EntityID(id, DrugPackages) }
            it[DrugPackages.formId] = EntityID(formId, DosageForms)
            it[DrugPackages.strengthValue] = strengthValue
            it[DrugPackages.strengthUnitId] = strengthUnitId?.let { id -> EntityID(id, Units) }
        }.value
    }
}


