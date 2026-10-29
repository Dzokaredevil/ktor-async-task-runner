package com.example.taskrunner.dto

import com.example.taskrunner.model.TaskStatus
import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskRequest(val userId: String, val name: String)

@Serializable
data class ProgressMessage(
    val taskId: String,
    val progress: Int,
    val status: TaskStatus,
    val result: String? = null
)