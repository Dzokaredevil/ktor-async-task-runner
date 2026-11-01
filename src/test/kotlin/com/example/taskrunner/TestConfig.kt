package com.example.taskrunner

import io.ktor.server.config.*
import io.ktor.server.testing.*

fun ApplicationTestBuilder.useTestConfig() {
    environment {
        config = MapApplicationConfig(
            "database.url" to "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "database.user" to "sa",
            "database.password" to "",
            "database.maxPoolSize" to "2"
        )
    }
}