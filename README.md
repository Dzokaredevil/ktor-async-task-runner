# ktor-async-task-runner

Async background task runner with real-time progress via WebSocket — Kotlin, Ktor, PostgreSQL, Docker.

## Возможности
- Создание фоновой задачи через REST, прогресс приходит по WebSocket только её владельцу.
- Прогресс и результат сохраняются в PostgreSQL.
- Простой веб-клиент на `/`.
- Docker-compose поднимает приложение и БД одной командой.

## Запуск через Docker

```bash
docker compose up --build
```

<img width="1879" height="937" alt="image" src="https://github.com/user-attachments/assets/214b5ca5-fa1b-4483-894c-d19fd3480581" />
