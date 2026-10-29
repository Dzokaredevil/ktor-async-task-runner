package com.example.taskrunner.service

import com.example.taskrunner.dto.ProgressMessage
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ProgressNotifier {
    private val log = LoggerFactory.getLogger(ProgressNotifier::class.java)
    private val sessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()

    fun register(userId: String, session: DefaultWebSocketSession) {
        sessions.computeIfAbsent(userId) { Collections.synchronizedSet(mutableSetOf()) }.add(session)
        log.info("WS registered user=$userId total=${sessions[userId]?.size}")
    }

    fun unregister(userId: String, session: DefaultWebSocketSession) {
        sessions[userId]?.remove(session)
        if (sessions[userId]?.isEmpty() == true) sessions.remove(userId)
        log.info("WS unregistered user=$userId")
    }

    suspend fun notify(userId: String, message: ProgressMessage) {
        val userSessions = sessions[userId] ?: return
        val json = Json.encodeToString(message)
        // Копируем список, чтобы избежать ConcurrentModificationException
        // при параллельном подключении/отключении других сессий
        userSessions.toList().forEach { session ->
            runCatching { session.send(Frame.Text(json)) }
                .onFailure { log.warn("Failed to send to user=$userId: ${it.message}") }
        }
    }
}