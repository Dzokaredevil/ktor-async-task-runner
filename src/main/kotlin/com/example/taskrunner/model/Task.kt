package com.example.taskrunner.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

enum class TaskStatus { PENDING, RUNNING, COMPLETED, FAILED }

object Tasks : Table("tasks") {
    val id = uuid("id").autoGenerate()
    val userId = varchar("user_id", 100)
    val name = varchar("name", 200)
    val status = enumerationByName("status", 20, TaskStatus::class).default(TaskStatus.PENDING)
    val progress = integer("progress").default(0)
    val result = text("result").nullable()
    val createdAt = timestamp("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)
    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class TaskDto(
    val id: String,
    val userId: String,
    val name: String,
    val status: TaskStatus,
    val progress: Int,
    val result: String? = null,
    val createdAt: String
)