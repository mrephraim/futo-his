package com.example.application.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.csrf.*

//fun Application.configureSecurity() {
//    install(CSRF) {
//        // tests Origin is an expected value
//        allowOrigin("http://127.0.0.1:8080")
//
//        // tests Origin matches Host header
//        originMatchesHost()
//
//        // custom header checks
//        checkHeader("X-CSRF-Token")
//    }
//}
