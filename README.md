# ktor-async-task-runner

Async background task runner with real-time progress via WebSocket — Kotlin, Ktor, PostgreSQL, Docker.

О проекте:

Код сгенерирован с помощью LLM и затем доработан вручную. Исходный промт лежит в prompt.md. История коммитов отражает итерации: исправление несовместимости версий Kotlin/Ktor, установка плагина WebSockets, изоляция сбоев задач, защита от гонок при рассылке и валидация входных данных — с обоснованием каждого изменения.

## Возможности

- Создание фоновой задачи через REST, прогресс приходит по WebSocket только её владельцу.
- Прогресс и результат сохраняются в PostgreSQL.
- Простой веб-клиент на `/`.
- Docker-compose поднимает приложение и БД одной командой.

## Стек

- Kotlin 2.2.21, Ktor 3.3.0 (Netty)
- PostgreSQL 16, Exposed + HikariCP
- kotlinx.serialization, kotlinx.coroutines
- Gradle (Kotlin DSL), Docker, docker-compose

## Запуск через Docker
```bash
docker compose up --build
```

После старта откройте http://localhost:8080. Приложение поднимется вместе с PostgreSQL; схема таблицы tasks создаётся автоматически при первом запуске.

Остановить и удалить контейнеры вместе с данными БД:
```bash
docker compose down -v
```

## Локальный запуск

Поднять PostgreSQL (только БД, без приложения):
```bash
docker compose up -d postgres
```

Запустить приложение:
```bash
./gradlew run
```

Открыть http://localhost:8080.

## Переменные окружения
| Переменная | По умолчанию | Назначение |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/taskrunner` | JDBC-URL PostgreSQL |
| `DB_USER` | `taskrunner` | Пользователь БД |
| `DB_PASSWORD` | `taskrunner` | Пароль БД |
| `PORT` | `8080` | Порт HTTP-сервера |

Значения по умолчанию совпадают с настройками docker-compose.yml, поэтому при локальном запуске переменные можно не задавать.

## Использование
Откройте http://localhost:8080. Клиент сгенерирует userId, сохранит его в localStorage и подключится по WebSocket. Введите название задачи и нажмите «Создать» — прогресс-бар будет расти по мере выполнения.

## REST API
Создать задачу:
```bash
curl -X POST http://localhost:8080/tasks \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1","name":"demo"}'
```

Список задач пользователя:
```bash
curl 'http://localhost:8080/tasks?userId=u1'
```

Конкретная задача:
```bash
curl http://localhost:8080/tasks/<uuid>
```

WebSocket
ws://localhost:8080/ws?userId=u1 — сервер отправляет JSON-сообщения о прогрессе:

```json
{"taskId":"...","progress":40,"status":"RUNNING","result":null}
```

При завершении:
```json
{"taskId":"...","progress":100,"status":"COMPLETED","result":"Task completed at ..."}
```

## Проверка изоляции пользователей
Откройте http://localhost:8080 в двух окнах — обычном и в режиме инкогнито. Это два разных userId. 
Создайте задачу в первом окне: прогресс должен обновляться только в нём. Во втором окне сообщения о чужой задаче появляться не должны — это подтверждает, что WebSocket-рассылка фильтруется по userId.

## Структура проекта
```text
src/main/kotlin/com/example/taskrunner/
├── Application.kt              — точка входа, установка плагинов
├── plugins/
│   ├── Database.kt             — HikariCP + Exposed, создание схемы
│   ├── Serialization.kt        — ContentNegotiation + kotlinx.serialization
│   └── Routing.kt              — REST-маршруты и WebSocket /ws
├── model/Task.kt               — таблица Exposed + TaskDto
├── dto/Messages.kt             — DTO для REST и WebSocket
└── service/
    ├── TaskService.kt          — бизнес-логика, запуск корутин
    └── ProgressNotifier.kt     — управление WS-сессиями по userId

src/main/resources/
├── application.conf            — конфигурация Ktor и БД
├── logback.xml
└── static/index.html           — веб-клиент
```

<img width="1879" height="937" alt="image" src="https://github.com/user-attachments/assets/214b5ca5-fa1b-4483-894c-d19fd3480581" />
