package com.example.taskrunner.plugins

import com.example.taskrunner.dto.CreateTaskRequest
import com.example.taskrunner.dto.ProgressMessage
import com.example.taskrunner.service.ProgressNotifier
import com.example.taskrunner.service.TaskService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText(
                this::class.java.classLoader.getResource("static/index.html")!!.readText(),
                ContentType.Text.Html
            )
        }

        route("/tasks") {
            post {
                val req = call.receive<CreateTaskRequest>()
                if (req.userId.isBlank() || req.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId and name required"))
                    return@post
                }
                val task = TaskService.createTask(req.userId, req.name)
                call.respond(HttpStatusCode.Created, task)
            }
            get {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId required"))
                call.respond(TaskService.getTasksByUser(userId))
            }
            get("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val task = TaskService.getTask(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(task)
            }
        }

        webSocket("/ws") {
            val userId = call.request.queryParameters["userId"]
                ?: run {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "userId required"))
                    return@webSocket
                }
            ProgressNotifier.register(userId, this)
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text && frame.readText() == "ping") {
                        send(Frame.Text("pong"))
                    }
                }
            } finally {
                ProgressNotifier.unregister(userId, this)
            }
        }
    }
}