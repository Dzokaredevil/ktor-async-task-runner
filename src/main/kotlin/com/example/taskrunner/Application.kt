package com.example.taskrunner

import com.example.taskrunner.plugins.configureDatabase
import com.example.taskrunner.plugins.configureRouting
import com.example.taskrunner.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.websocket.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(WebSockets)
    configureDatabase()
    configureSerialization()
    configureRouting()
}