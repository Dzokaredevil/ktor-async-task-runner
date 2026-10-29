package com.example.taskrunner.service

import com.example.taskrunner.dto.ProgressMessage
import com.example.taskrunner.model.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.UUID

object TaskService {
    private val log = LoggerFactory.getLogger(TaskService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createTask(userId: String, name: String): TaskDto {
        val id = UUID.randomUUID()
        transaction {
            Tasks.insert {
                it[Tasks.id] = id
                it[Tasks.userId] = userId
                it[Tasks.name] = name
                it[Tasks.status] = TaskStatus.PENDING
                it[Tasks.progress] = 0
            }
        }
        val dto = getTask(id)!!
        runTaskAsync(id, userId)
        return dto
    }

    fun getTask(id: UUID): TaskDto? = transaction {
        Tasks.select { Tasks.id eq id }.map(::toDto).singleOrNull()
    }

    fun getTasksByUser(userId: String): List<TaskDto> = transaction {
        Tasks.select { Tasks.userId eq userId }
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map(::toDto)
    }

    private fun runTaskAsync(taskId: UUID, userId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                updateStatus(taskId, TaskStatus.RUNNING, 0)
                ProgressNotifier.notify(userId, ProgressMessage(taskId.toString(), 0, TaskStatus.RUNNING))

                for (p in 10..100 step 10) {
                    delay(500)
                    updateStatus(taskId, TaskStatus.RUNNING, p)
                    ProgressNotifier.notify(userId, ProgressMessage(taskId.toString(), p, TaskStatus.RUNNING))
                }
                val result = "Task completed at ${System.currentTimeMillis()}"
                updateStatus(taskId, TaskStatus.COMPLETED, 100, result)
                ProgressNotifier.notify(userId, ProgressMessage(taskId.toString(), 100, TaskStatus.COMPLETED, result))
            } catch (e: CancellationException) {
                log.info("Task $taskId cancelled")
                throw e
            } catch (e: Exception) {
                log.error("Task $taskId failed", e)
                updateStatus(taskId, TaskStatus.FAILED, -1, e.message)
                ProgressNotifier.notify(userId, ProgressMessage(taskId.toString(), -1, TaskStatus.FAILED, e.message))
            }
        }
    }

    private fun updateStatus(id: UUID, status: TaskStatus, progress: Int, result: String? = null) {
        transaction {
            Tasks.update({ Tasks.id eq id }) {
                it[Tasks.status] = status
                it[Tasks.progress] = progress
                if (result != null) it[Tasks.result] = result
            }
        }
    }

    private fun toDto(row: ResultRow) = TaskDto(
        id = row[Tasks.id].toString(),
        userId = row[Tasks.userId],
        name = row[Tasks.name],
        status = row[Tasks.status],
        progress = row[Tasks.progress],
        result = row[Tasks.result],
        createdAt = row[Tasks.createdAt].toString()
    )
}