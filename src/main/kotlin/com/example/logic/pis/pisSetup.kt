package com.example.logic.pis

import com.example.data.database.*
import com.example.data.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.pisSetupRoutes() {
    val json = Json { ignoreUnknownKeys = true }

    get("pis/new-batch"){
        call.respond(ThymeleafContent("pis/pis-setup/new-batch", emptyMap()))
    }


    post("pis/new-batch") {
        val newBatchRequest = call.receive<NewBatchRequest>()

        val isAdded = addNewDrugBatch(newBatchRequest.name, newBatchRequest.description)

        if (isAdded) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "message" to "New Drug Batch '${newBatchRequest.name}' with description '${newBatchRequest.description}' added successfully!"
                )
            )
        } else {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Failed to add Drug Batch '${newBatchRequest.name}' with description '${newBatchRequest.description}'."
                )
            )
        }
    }

    get("pis/new-category"){
        call.respond(ThymeleafContent("pis/pis-setup/new-category", emptyMap()))
    }
    post("pis/new-category") {
        val request = try {
            call.receive<CategoryRequest>()
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request format"))
            return@post
        }

        if (request.name.isBlank() || request.description.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "All fields are required"))
            return@post
        }

        try {
            insertDrugCategory(request.name, request.description)
            call.respond(HttpStatusCode.Created, mapOf("success" to "Category ${request.name} created successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to save category"))
        }
    }

    post("pis/new-generic") {
        val request = try {
            call.receive<CategoryRequest>()
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request format"))
            return@post
        }

        if (request.name.isBlank() || request.description.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "All fields are required"))
            return@post
        }

        try {
            insertDrugGeneric(request.name, request.description)
            call.respond(HttpStatusCode.Created, mapOf("success" to "Generic ${request.name} created successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to save category"))
        }
    }


    get("pis/new-unit"){
        call.respond(ThymeleafContent("pis/pis-setup/new-unit", emptyMap()))
    }

    get("pis/new-drug"){
        call.respond(ThymeleafContent("pis/pis-setup/new-drug", emptyMap()))
    }

    post("pis/drugs") {
        try {
            val request = call.receive<DrugRequest>()
            val id = addDrug(
                name = request.name,
                formId = request.formId,
                categoryId = request.categoryId,
                genericId = request.genericId,
                description = request.description,
                active = request.active
            )

            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    message = "Drug added successfully",
                    data = IdResponse(id)
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<IdResponse>(
                    success = false,
                    message = e.message ?: "Failed to add drug",
                    data = null
                )
            )
        }
    }




    post("pis/drug-parameters") {
        try {
            val request = call.receive<DrugParameterRequest>()
            val id = addDrugParameter(
                drugId = request.drugId,
                name = request.name,
                value = request.value.toBigDecimal(),
                unitId = request.unitId,
                description = request.description
            )
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    message = "Drug parameter added successfully",
                    data = IdResponse(id)
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, message = e.message ?: "Failed to add drug parameter")
            )
        }
    }


    post("pis/dosage-forms") {
        try {
            val request = call.receive<DosageFormRequest>()
            val id = addDosageForm(
                name = request.name,
                example = request.example
            )

            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    message = "Dosage form added successfully",
                    data = IdResponse(id)
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    message = e.message ?: "An error occurred",
                    data = null
                )
            )
        }
    }


    post("pis/units") {
        try {
            val request = call.receive<UnitPisRequest>()
            val id = addUnit(
                name = request.name,
                baseUnitId = request.baseUnitId,
                factor = request.factor.toBigDecimal(),
                category = request.category
            )
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(success = true, message = "Unit added successfully", data = IdResponse(id))
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, message = e.message ?: "Unknown error")
            )
        }
    }


    post("pis/drug-packages") {
        try {
            val request = call.receive<DrugPackageRequest>()

            val id = addDrugPackage(
                drugId = request.drugId,
                packageName = request.packageName,
                quantity = request.quantity,
                subPackageId = request.subPackageId,
                formId = request.formId,
                strengthValue = request.strengthValue?.toBigDecimal(),
                strengthUnitId = request.strengthUnitId
            )

            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    message = "Drug package added successfully",
                    data = IdResponse(id)
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    message = e.message ?: "Failed to add drug package"
                )
            )
        }
    }



    // Get all drugs
    get("pis/drugs") {
        try {
            val drugs = transaction {
                Drugs.selectAll().map {
                    DrugResponse(
                        id = it[Drugs.id].value,
                        name = it[Drugs.name],
                        formId = it[Drugs.formId].value,
                        description = it[Drugs.description],
                        active = it[Drugs.active]
                    )
                }
            }
            call.respond(drugs) // JSON array
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(success = false, message = e.message ?: "Error fetching drugs")
            )
        }
    }


// Get all drug parameters
    get("pis/drug-parameters") {
        try {
            val params = transaction {
                DrugParameters.selectAll().map {
                    mapOf(
                        "id" to it[DrugParameters.id].value,
                        "drugId" to it[DrugParameters.drugId].value,
                        "name" to it[DrugParameters.name],
                        "value" to it[DrugParameters.value].toPlainString(),
                        "unitId" to it[DrugParameters.unitId].value,
                        "description" to it[DrugParameters.description]
                    )
                }
            }
            call.respond(params)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
        }
    }

// Get all dosage forms
    get("pis/dosage-forms") {
        try {
            val forms = transaction {
                DosageForms.selectAll().map {
                    DosageFormResponse(
                        id = it[DosageForms.id].value,
                        name = it[DosageForms.name],
                        example = it[DosageForms.example]
                    )
                }
            }

            call.respond(
                ApiResponse(
                    success = true,
                    message = "Dosage forms retrieved successfully",
                    data = forms
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<List<DosageFormResponse>>(
                    success = false,
                    message = e.message ?: "Error retrieving dosage forms",
                    data = null
                )
            )
        }
    }


// Get all units
    get("pis/units") {
        try {
            val units = transaction {
                Units.selectAll().map {
                    UnitResponse(
                        id = it[Units.id].value,
                        name = it[Units.name],
                        baseUnitId = it[Units.baseUnitId]?.value,
                        factor = it[Units.factor].toPlainString(),
                        category = it[Units.category]
                    )
                }
            }
            call.respond(units) // <- now JSON array of objects
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(success = false, message = e.message ?: "Error fetching units")
            )
        }
    }


// Get all drug packages
    get("pis/drug-packages") {
        try {
            val packages = transaction {
                DrugPackages.selectAll().map {
                    mapOf(
                        "id" to it[DrugPackages.id].value,
                        "drugId" to it[DrugPackages.drugId].value,
                        "packageName" to it[DrugPackages.packageName],
                        "quantity" to it[DrugPackages.quantity],
                        "subPackageId" to it[DrugPackages.subPackageId]?.value,
                        "formId" to it[DrugPackages.formId].value,
                        "strengthValue" to it[DrugPackages.strengthValue],
                        "strengthUnitId" to it[DrugPackages.strengthUnitId]?.value
                    )
                }
            }
            call.respond(HttpStatusCode.OK, packages)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Failed to fetch drug packages"))
            )
        }
    }

    get("pis/drugs/{drugId}/packages") {
        try {
            val drugId = call.parameters["drugId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid drugId"))

            val packages = transaction {
                DrugPackages
                    .selectAll().where { DrugPackages.drugId eq drugId }
                    .map {
                        DrugPackageResponse(
                            id = it[DrugPackages.id].value,
                            drugId = it[DrugPackages.drugId].value,
                            packageName = it[DrugPackages.packageName],
                            quantity = it[DrugPackages.quantity],
                            subPackageId = it[DrugPackages.subPackageId]?.value,
                            formId = it[DrugPackages.formId].value,
                            strengthValue = it[DrugPackages.strengthValue]?.toDouble(),
                            strengthUnitId = it[DrugPackages.strengthUnitId]?.value
                        )
                    }
            }

            call.respond(HttpStatusCode.OK, packages)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(false, e.message ?: "Failed to fetch drug packages")
            )
        }
    }





    get("pis/categories") {
        try {
            val categories = transaction {
                DrugCategories.selectAll().map {
                    OptionResponse(
                        id = it[DrugCategories.id],
                        name = it[DrugCategories.name]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, ApiResponse(true, "Categories fetched", categories))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<List<OptionResponse>>(false, e.message ?: "Error fetching categories"))
        }
    }

    get("pis/generics") {
        try {
            val generics = transaction {
                DrugGenerics.selectAll().map {
                    OptionResponse(
                        id = it[DrugGenerics.id],
                        name = it[DrugGenerics.name]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, ApiResponse(true, "Generics fetched", generics))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<List<OptionResponse>>(false, e.message ?: "Error fetching generics"))
        }
    }




}