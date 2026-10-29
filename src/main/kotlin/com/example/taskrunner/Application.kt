package com.example.taskrunner

import com.example.taskrunner.plugins.configureDatabase
import com.example.taskrunner.plugins.configureRouting
import com.example.taskrunner.plugins.configureSerialization
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.websocket.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(WebSockets)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal error"))
        }
    }
    configureDatabase()
    configureSerialization()
    configureRouting()
}