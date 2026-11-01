package com.example.taskrunner

import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.taskrunner.model.Tasks
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketIsolationTest {

    private fun cleanDb() = transaction { Tasks.deleteAll() }

    @Test
    fun `progress is delivered only to task owner`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        val client = createClient { install(WebSockets) }

        coroutineScope {
            val u1Messages = Channel<String>(Channel.UNLIMITED)
            val u2Messages = Channel<String>(Channel.UNLIMITED)

            val job1 = launch {
                client.webSocket("/ws?userId=u1") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) u1Messages.trySend(frame.readText())
                    }
                }
            }
            val job2 = launch {
                client.webSocket("/ws?userId=u2") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) u2Messages.trySend(frame.readText())
                    }
                }
            }

            delay(200)

            val res = client.post("/tasks") {
                contentType(ContentType.Application.Json)
                setBody("""{"userId":"u1","name":"only-for-u1"}""")
            }
            assertEquals(201, res.status.value)

            val msg1 = withTimeout(3000) { u1Messages.receive() }
            val parsed = Json.parseToJsonElement(msg1).jsonObject
            assertEquals("RUNNING", parsed["status"]!!.jsonPrimitive.content)

            val leaked = withTimeoutOrNull(1000) { u2Messages.receive() }
            assertNull(leaked, "u2 must not receive progress of u1's task, got: $leaked")

            job1.cancel()
            job2.cancel()
        }
    }
}