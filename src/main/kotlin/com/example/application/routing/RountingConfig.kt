package com.example.application.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    routing {
        // Serve static files
        staticResources("/static", "static")

        get("/") {
            val indexPageStream = application.environment.classLoader.getResourceAsStream("static/index.html")
            if (indexPageStream != null) {
                call.respondText(indexPageStream.reader().readText(), ContentType.Text.Html)
            } else {
                call.respond(HttpStatusCode.NotFound, "index.html not found")
            }
        }
    }
}

