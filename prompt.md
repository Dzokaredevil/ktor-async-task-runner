# Промпт для генерации проекта

Создай проект на Kotlin + Ktor (Gradle Kotlin DSL) для фоновой обработки задач.

Требования:
1. REST POST /tasks — создать задачу { userId, name }, вернуть { id, status: "PENDING" }.
2. REST GET /tasks?userId=... — список задач пользователя.
3. WebSocket /ws?userId=... — сервер пушит прогресс только этому пользователю.
4. При создании задачи запускается корутина: имитация работы (0..100 шаг 10, delay 500ms),
   после каждого шага — сохранение прогресса в PostgreSQL и отправка WebSocket-сообщения.
5. PostgreSQL через Exposed + HikariCP. Таблица tasks: id (uuid), user_id, name, status, progress, result, created_at.
6. Статический index.html на / — простой интерфейс: поле имени задачи, кнопка создания, таблица с прогресс-барами.
7. Dockerfile и docker-compose.yml (app + postgres).
8. README.md с инструкцией запуска.

Используй kotlinx.serialization для JSON. Не используй Spring.
