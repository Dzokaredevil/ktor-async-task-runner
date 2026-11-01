package com.example.taskrunner

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.taskrunner.model.Tasks
import kotlin.test.*

class RestApiTest {

    private fun cleanDb() = transaction { Tasks.deleteAll() }

    @Test
    fun `POST tasks creates task with PENDING status`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        val res = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","name":"demo"}""")
        }

        assertEquals(HttpStatusCode.Created, res.status)
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertEquals("PENDING", body["status"]!!.jsonPrimitive.content)
        assertEquals("demo", body["name"]!!.jsonPrimitive.content)
        assertEquals("u1", body["userId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST tasks with blank name returns 400`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        val res = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","name":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("\"code\":\"validation\""), "expected unified error code, got: $body")
    }

    @Test
    fun `POST tasks with malformed json returns 400 invalid_json`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        val res = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("{not json")
        }

        assertEquals(HttpStatusCode.BadRequest, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("\"code\":\"invalid_json\""), "expected invalid_json, got: $body")
    }

    @Test
    fun `POST tasks with too long name returns 400`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        val longName = "x".repeat(201)
        val res = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","name":"$longName"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `GET tasks returns only tasks of given user`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","name":"a"}""")
        }
        client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u2","name":"b"}""")
        }

        val res = client.get("/tasks?userId=u1")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("\"name\": \"a\""), "u1 should see own task, got: $body")
        assertFalse(body.contains("\"name\": \"b\""), "u1 must not see u2 task, got: $body")
    }

    @Test
    fun `GET task by unknown id returns 404`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        val res = client.get("/tasks/00000000-0000-0000-0000-000000000000")
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun `GET task with invalid uuid returns 400`() = testApplication {
        useTestConfig()
        application { module() }
        cleanDb()

        val res = client.get("/tasks/not-a-uuid")
        assertEquals(HttpStatusCode.BadRequest, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("\"code\":\"invalid_id\""), "expected invalid_id, got: $body")
    }
}