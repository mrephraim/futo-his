package com.example.logic.pis

import com.example.data.database.*
import com.example.data.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.pisInventoryRoutes() {
    val json = Json { ignoreUnknownKeys = true }

    get("pis/inventory"){
        call.respond(ThymeleafContent("pis/pis-inventory/index", emptyMap()))
    }
    get("pis/inventory/all") {
        try {
            val filterParam = call.request.queryParameters["status"]
            val filter = when (filterParam) {
                "OUT_OF_STOCK" -> InventoryStatus.OUT_OF_STOCK
                "LOW_STOCK" -> InventoryStatus.LOW_STOCK
                "OK" -> InventoryStatus.OK
                else -> null
            }

            val inventory = getInventoryDrugDetails(filter)
            call.respond(
                HttpStatusCode.OK,
                InventoryResponse(success = true, data = inventory)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                InventoryResponse(success = false, error = e.message ?: "Failed to fetch inventory details")
            )
        }
    }


    get("pis/receive-stock"){
        call.respond(ThymeleafContent("pis/pis-inventory/receive-stock", emptyMap()))
    }


    post("pis/receive-stock") {
        try {
            val request = call.receive<ReceiveStockRequest>()

            transaction {
                // Step 1: compute quantity in base unit
                val packageRow = DrugPackages
                    .selectAll().where { DrugPackages.id eq request.packageId }
                    .singleOrNull()
                    ?: throw IllegalArgumentException("Invalid packageId")

                receiveStock(
                    drugId = request.drugId,
                    packageId = request.packageId,
                    batchNumber = request.batchNumber,
                    manufactureDate = request.manufactureDate?.let { LocalDate.parse(it) },
                    expiryDate = request.expiryDate?.let { LocalDate.parse(it) },
                    quantityOfPackage = request.quantityOfPackage,
                    unitCostPerPackage = request.unitCostPerPackage.toBigDecimal(),
                    receivedBy = request.receivedBy
                )
            }

            call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "Stock received successfully"))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(false, e.message ?: "Failed to receive stock")
            )
        }
    }



}