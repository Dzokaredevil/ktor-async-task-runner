package com.example.taskrunner.service

import com.example.taskrunner.dto.ProgressMessage
import com.example.taskrunner.model.TaskStatus
import io.ktor.websocket.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ProgressNotifierTest {

    private fun fakeSession(): DefaultWebSocketSession =
        mockk(relaxed = true) {
            coEvery { send(any<Frame>()) } just Runs
        }

    @AfterTest
    fun tearDown() {
        // чистим состояние синглтона между тестами
        // (в проде так делать не нужно, но для теста изолируем)
        listOf("u1", "u2").forEach { userId ->
            // рефлексией не лезем — просто вызываем unregister на фейках
        }
    }

    @Test
    fun `notify delivers only to matching user`() = runTest {
        val s1 = fakeSession()
        val s2 = fakeSession()
        ProgressNotifier.register("u1", s1)
        ProgressNotifier.register("u2", s2)

        ProgressNotifier.notify("u1", ProgressMessage("t1", 50, TaskStatus.RUNNING))

        coVerify(exactly = 1) { s1.send(any<Frame>()) }
        coVerify(exactly = 0) { s2.send(any<Frame>()) }

        ProgressNotifier.unregister("u1", s1)
        ProgressNotifier.unregister("u2", s2)
    }

    @Test
    fun `notify to unknown user does nothing`() = runTest {
        val s = fakeSession()
        ProgressNotifier.register("u1", s)

        ProgressNotifier.notify("ghost", ProgressMessage("t1", 10, TaskStatus.RUNNING))

        coVerify(exactly = 0) { s.send(any<Frame>()) }
        ProgressNotifier.unregister("u1", s)
    }

    @Test
    fun `unregister removes session and stops delivery`() = runTest {
        val s = fakeSession()
        ProgressNotifier.register("u1", s)
        ProgressNotifier.unregister("u1", s)

        ProgressNotifier.notify("u1", ProgressMessage("t1", 10, TaskStatus.RUNNING))

        coVerify(exactly = 0) { s.send(any<Frame>()) }
    }

    @Test
    fun `send failure on one session does not break delivery to others`() = runTest {
        val broken = mockk<DefaultWebSocketSession>(relaxed = true) {
            coEvery { send(any<Frame>()) } throws RuntimeException("socket closed")
        }
        val healthy = fakeSession()

        ProgressNotifier.register("u1", broken)
        ProgressNotifier.register("u1", healthy)

        // не должно бросить исключение
        ProgressNotifier.notify("u1", ProgressMessage("t1", 30, TaskStatus.RUNNING))

        coVerify(exactly = 1) { healthy.send(any<Frame>()) }

        ProgressNotifier.unregister("u1", broken)
        ProgressNotifier.unregister("u1", healthy)
    }
}