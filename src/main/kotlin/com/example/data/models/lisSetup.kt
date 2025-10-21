package com.example.data.models

import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction


@Serializable
data class CategoryRequest(val name: String, val description: String)

@Serializable
data class UnitInput(val name: String, val base: String, val factor: Double)

@Serializable
data class ParameterRequest(
    val parameterName: String,
    val dataType: String,
    val description: String,
    val units: List<UnitRequest>,
    val comments: List<String>
)
@Serializable
data class UnitRequest(
    var name: String,
    var base: String,
    var factor: Double?
)
@Serializable
data class SampleTypeRequest(
    val name: String,
    val description: String
)
@Serializable
data class SampleTypes(
    val id: Int,
    val name: String
)

@Serializable
data class LabItem(
    val id: Int,
    val name: String
)
@Serializable
data class CategoryItem(
    val id: Int,
    val name: String
)

@Serializable
data class LabTest(
    val testName: String,
    val testCategory: Int,
    val bsl: Int,
    val parameters: List<Int>,
    val sampleTypes: List<Int>
)

@Serializable
data class LabTestsPerCategory(
    val id: Int,
    val name: String
)

@Serializable
data class SampleInfo(
    val id: Int,
    val name: String,
    val description: String
)





// Function to insert a category
fun insertCategory(name: String, description: String): Int {
    return transaction {
        LabCategories.insert {
            it[LabCategories.name] = name
            it[LabCategories.description] = description
        } get LabCategories.id
    }
}

// Function to fetch all categories

// Function to update a category
fun updateCategory(id: Int, name: String, description: String): Boolean {
    return transaction {
        LabCategories.update({ LabCategories.id eq id }) {
            it[LabCategories.name] = name
            it[LabCategories.description] = description
        } > 0
    }
}


fun addParameter(request: ParameterRequest): Int {
    return transaction {
        LabParameters.insert {
            it[name] = request.parameterName
            it[dataType] = request.dataType
            it[description] = request.description
        } get LabParameters.id
    }
}


fun addParameterDetails(parameterId: Int, request: ParameterRequest) {
    transaction {
        request.units.forEach { unit ->
            LabParameterUnits.insert {
                it[LabParameterUnits.parameterId] = parameterId
                it[name] = unit.name
                it[base] = unit.base
                it[factor] = unit.factor?: 0.0
            }
        }

        request.comments.forEach { comment ->
            LabParameterComments.insert {
                it[LabParameterComments.parameterId] = parameterId
                it[LabParameterComments.comment] = comment
            }
        }
    }
}

fun addSampleType(name: String, description: String): Boolean {
    return try {
        transaction{
            LabSampleTable.insert {
                it[LabSampleTable.name] = name
                it[LabSampleTable.description] = description
            }
        }
        true
    } catch (e: Exception) {
        println("Error adding sample type: ${e.message}")
        false
    }
}

fun fetchLabParameters(): List<LabItem> {
    return transaction {
        LabParameters
            .selectAll()
            .map { LabItem(it[LabParameters.id], it[LabParameters.name]) }
    }
}

fun fetchLabSamples(): List<LabItem> {
    return transaction {
        LabSampleTable
            .selectAll()
            .map { LabItem(it[LabSampleTable.id], it[LabSampleTable.name]) }
    }
}

fun fetchCategories(): List<CategoryItem> {
    return transaction {
        LabCategories
            .selectAll()
            .map { CategoryItem(it[LabCategories.id], it[LabCategories.name]) }
    }
}
fun insertLabTest(
    testName: String,
    testCategory: Int,
    bsl: Int,
    parameters: List<Int>,
    sampleTypes: List<Int>
): Boolean {
    return try {
        transaction {
            LabTests.insert {
                it[LabTests.testName] = testName
                it[LabTests.testCategory] = testCategory
                it[LabTests.bsl] = bsl
                it[LabTests.parameters] = parameters
                it[LabTests.sampleTypes] = sampleTypes
            }
        }
        true // Return true if insertion succeeds
    } catch (e: Exception) {
        e.printStackTrace() // Log the exception for debugging
        false // Return false if insertion fails
    }
}

fun fetchTestsUnderCategory (cid: Int): List<LabTestsPerCategory> {
    return transaction {
        LabTests
            .selectAll().where {LabTests.testCategory eq cid}
            .map {
                LabTestsPerCategory(
                    id = it[LabTests.id],
                    name = it[LabTests.testName]
                )
            }
    }
}

// Function to fetch samples for a particular test
fun fetchSamplesForTest(testId: Int): List<SampleInfo> {
    return transaction {
        try {
            // Log test ID
            println("Fetching samples for test ID: $testId")

            // Fetch sample IDs from LabTests
            val sampleIds: List<Int> = LabTests
                .selectAll().where { LabTests.id eq testId }.firstNotNullOfOrNull { it[LabTests.sampleTypes].toList() }
                ?: emptyList()

            println("Sample IDs retrieved: $sampleIds")

            // Fetch sample details
            LabSampleTable
                .selectAll().where { LabSampleTable.id inList sampleIds }
                .map {
                    SampleInfo(
                        id = it[LabSampleTable.id],
                        name = it[LabSampleTable.name],
                        description = it[LabSampleTable.description]
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Rethrow the exception for the route to catch
        }
    }
}



