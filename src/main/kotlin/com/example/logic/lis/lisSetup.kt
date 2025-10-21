package com.example.logic.lis

import com.example.data.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.json.Json

fun Route.setupRoutes() {
    val json = Json { ignoreUnknownKeys = true }
    // Add a new category
    get("lis/new-category"){
        call.respond(ThymeleafContent("lis/lab-setup/new-category", emptyMap()))
    }
    post("lis/new-category") {
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
            val id = insertCategory(request.name, request.description)
            call.respond(HttpStatusCode.Created, mapOf("success" to "Category created successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to save category"))
        }
    }

    // Fetch all categories
    get("lis/categories") {
        try {
            val categories = fetchCategories()
            call.respond(HttpStatusCode.OK, categories)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch categories"))
        }
    }
    get("lis/all-categories") {
        try {
            val categories = fetchCategories()
            call.respond(HttpStatusCode.OK, mapOf("categories" to categories))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch categories"))
        }
    }

    get("lis/new-parameter"){
        call.respond(ThymeleafContent("lis/lab-setup/new-parameter", emptyMap()))
    }


    post("lis/add-parameter") {
        try {
            // Read the body once as a string
            val rawPayload = call.receiveText()
            println("Raw Payload: $rawPayload")

            // Deserialize the payload from the raw JSON string
            val parameterRequest = json.decodeFromString<ParameterRequest>(rawPayload)
            val parameterId = addParameter(parameterRequest)
            addParameterDetails(parameterId, parameterRequest)

            call.respond(HttpStatusCode.Created, mapOf("message" to "Parameter added successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }




    // Edit a category
    put("lis/edit-category/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid category ID"))
            return@put
        }

        val request = try {
            call.receive<CategoryRequest>()
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request format"))
            return@put
        }

        if (request.name.isBlank() || request.description.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "All fields are required"))
            return@put
        }

        try {
            val updated = updateCategory(id, request.name, request.description)
            if (updated) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Category updated successfully"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Category not found"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update category"))
        }
    }


    get("lis/new-sample-type"){
        call.respond(ThymeleafContent("lis/lab-setup/new-sample", emptyMap()))
    }


    post("lis/new-sample-type") {
        val sampleTypeRequest = call.receive<SampleTypeRequest>()

        val isAdded = addSampleType(sampleTypeRequest.name, sampleTypeRequest.description)

        if (isAdded) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Sample type added successfully!"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to add sample type."))
        }
    }

    get("lis/new-test"){
        call.respond(ThymeleafContent("lis/lab-setup/new-test", emptyMap()))
    }
    get("lis/api/lab-parameters") {
        try {
            val labParameters = fetchLabParameters()
            call.respond(HttpStatusCode.OK, labParameters)
        } catch (e: Exception) {
            println("Error in /lis/api/lab-parameters: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch lab parameters"))
        }
    }

    get("lis/api/lab-samples") {
        try {
            val labSamples = fetchLabSamples()
            call.respond(HttpStatusCode.OK, labSamples)
        } catch (e: Exception) {
            println("Error in /lis/api/lab-samples: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch lab samples"))
        }
    }

    post("lis/submit-lab-test") {
        val labTest = call.receive<LabTest>()

        if (labTest.testName.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Invalid input data.")
            return@post
        }

        val labTestId = insertLabTest(labTest.testName, labTest.testCategory, labTest.bsl, labTest.parameters, labTest.sampleTypes)
        if (labTestId) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Test created successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Failed to insert lab test.")
        }
    }






}
