package com.example.taskrunner.plugins

import com.example.taskrunner.model.TaskStatus
import com.example.taskrunner.model.Tasks
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun Application.configureDatabase() {
    val config = environment.config.config("database")
    val hikari = HikariConfig().apply {
        jdbcUrl = config.property("url").getString()
        username = config.property("user").getString()
        password = config.property("password").getString()
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = config.property("maxPoolSize").getString().toInt()
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }
    val dataSource = HikariDataSource(hikari)
    Database.connect(dataSource)

    transaction {
        SchemaUtils.create(Tasks)
        val recovered = Tasks.update({ Tasks.status eq TaskStatus.RUNNING }) {
            it[status] = TaskStatus.FAILED
            it[result] = "Server restarted during execution"
        }
        if (recovered > 0) {
            log.warn("Marked $recovered stale RUNNING task(s) as FAILED after restart")
        }
    }
    log.info("Database connected: ${hikari.jdbcUrl}")
}