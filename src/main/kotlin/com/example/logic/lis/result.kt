package com.example.logic.lis

import com.example.data.database.Requisitions
import com.example.data.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction



fun Route.result(){
    get("lis/pending-results"){
        call.respond(ThymeleafContent("lis/pending-results", emptyMap()))
    }
    get("lis/process-result") {
        // Extract requisitionId from query parameter "id"
        val requisitionId = call.request.queryParameters["id"]?.toIntOrNull()

        if (requisitionId != null) {
            // Query the requisitions table using the requisitionId
            val requisitionStatus = transaction {
                Requisitions.selectAll().where { Requisitions.id eq requisitionId }
                    .map { it[Requisitions.rstatus] }
                    .singleOrNull()
            }

            // Check if the requisition exists and its status
            if (requisitionStatus != null && requisitionStatus == 1) {
                // Status is 2, render the process-result page
                call.respond(ThymeleafContent("lis/process-result", emptyMap()))
            } else if(requisitionStatus == 2){
                // If status is not 1, redirect to the pending results page
                call.respondRedirect("/lis/result-printout/${requisitionId}")
            }else{
                call.respondRedirect("/lis/pending-results")
            }
        } else {
            // If the requisitionId is invalid or missing
            call.respond(HttpStatusCode.BadRequest, "Invalid or missing requisition ID.")
        }
    }

    get("lis/api/get-parameters/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid ID")
            return@get
        }

        val parameterDetails = fetchParametersWithValues(id) // Call your previously defined function

        call.respond(parameterDetails)
    }
    post("lis/api/submit-result-form") {
        try {
            // Parse the request body as JSON
            val request = call.receive<FormSubmissionRequest>()

            val isSuccess = processFormSubmission(request)

            if (isSuccess) {
                call.respond(mapOf("message" to "Form submission successful"))
            } else {
                call.respond(mapOf("message" to "Form submission failed"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(mapOf("error" to "An error occurred: ${e.localizedMessage}"))
        }
    }

    get ("lis/api/requisitions/comments/{requisitionId}"){
        val requisitionId = call.parameters["requisitionId"]?.toIntOrNull()
        if (requisitionId == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid requisition ID.")
            return@get
        }

        try {
            // Fetch comments for the given requisition ID
            val comments = fetchCommentsFromDatabase(requisitionId)

            // Respond with comments as JSON
            call.respond(HttpStatusCode.OK, comments)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "Failed to fetch comments.")
        }
    }

    delete("/api/comments/{requisitionId}/{commentId}") {
        val requisitionId = call.parameters["requisitionId"]?.toIntOrNull()
        val commentId = call.parameters["commentId"]?.toIntOrNull()

        if (requisitionId == null || commentId == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid requisitionId or commentId")
            return@delete
        }

        val isDeleted = deleteComment(commentId, requisitionId)

        if (isDeleted) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Comment deleted successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("message" to "Comment or requisition not found"))
        }
    }

    get("lis/result-printout/{id}"){
        call.respond(ThymeleafContent("lis/result-printout", emptyMap()))
    }
    get("lis/results/print/{id}") {
        val requisitionId = call.parameters["id"]?.toIntOrNull()
        if (requisitionId == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid ID")
            return@get
        }

        val requisitionInfo = getRequisitionById(requisitionId)
        if (requisitionInfo == null) {
            call.respond(HttpStatusCode.NotFound, "Requisition not found")
            return@get
        }

        val parametersInfo = fetchParametersWithValues(requisitionId)
        val commentsInfo = fetchCommentsFromDatabase(requisitionId)

        // Convert requisition values to String to avoid serialization issues
        val requisitionAsString = requisitionInfo.mapValues { it.value?.toString() ?: "" }

        // Wrap everything in a single response object
        val response = LabResultResponse(
            requisition = requisitionAsString,
            parameters = parametersInfo,
            comments = commentsInfo
        )

        call.respond(response)
    }

    get("lis/process-sample") {
        val sampleId = call.request.queryParameters["id"]?.trim()

        // Check if sampleId is empty
        if (sampleId.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "Sample ID is required")
            return@get
        }

        // Safely convert sampleId to Int
        val sampleIdInt = sampleId.toIntOrNull()
        if (sampleIdInt == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid Sample ID")
            return@get
        }

        // Query the database safely
        val requisitionId = transaction {
            Requisitions
                .selectAll().where { Requisitions.sampleId eq sampleIdInt } // ✅ CORRECT CONDITION
                .map { it[Requisitions.id] }
                .firstOrNull()
        }

        if (requisitionId != null) {
            call.respondRedirect("/lis/process-result?id=$requisitionId")
        } else {
            call.respond(HttpStatusCode.NotFound, "No requisition found for sample ID: $sampleIdInt")
        }
    }





}