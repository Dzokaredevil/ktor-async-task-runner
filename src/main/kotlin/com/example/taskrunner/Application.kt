package com.example.taskrunner

import com.example.taskrunner.dto.ErrorResponse
import com.example.taskrunner.plugins.configureDatabase
import com.example.taskrunner.plugins.configureRouting
import com.example.taskrunner.plugins.configureSerialization
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(WebSockets)

    install(StatusPages) {
        // Битый JSON в теле запроса
        exception<ContentTransformationException> { call, cause ->
            call.application.log.debug("Malformed request body", cause)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_json", "Malformed JSON body")
            )
        }

        // Отсутствует обязательное поле
        exception<MissingRequestParameterException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("missing_field", "Missing parameter: ${cause.parameterName}")
            )
        }

        // Прочие ошибки клиента (400-499), брошенные через BadRequestException
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("bad_request", cause.message ?: "Bad request")
            )
        }

        // Всё остальное — 500
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal", "Internal server error")
            )
        }
    }

    configureDatabase()
    configureSerialization()
    configureRouting()
}