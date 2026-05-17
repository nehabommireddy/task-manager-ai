# Task Manager — Spring Boot REST API

A personal task manager REST API built with Java 17, Spring Boot 3, H2, and AI-powered suggestions via the Anthropic API.

## Tech Stack

| Layer | Choice | Why |
|-------|--------|-----|
| Language | Java 17 | Records, sealed classes, text blocks |
| Framework | Spring Boot 3.2 | Auto-config, JPA, Validation, MVC |
| Database | H2 (in-memory) | Zero setup, reset on restart |
| Migrations | Flyway | Versioned schema, reproducible |
| Lombok | Yes | Reduces boilerplate in entity |
| AI | Anthropic claude-sonnet-4 | Via direct REST call |
| Tests | JUnit 5 + Mockito + MockMvc | Unit + integration |

## Project Structure

```
src/
├── main/
│   ├── java/com/example/taskmanager/
│   │   ├── TaskManagerApplication.java
│   │   ├── config/          AppConfig.java (RestTemplate, CORS)
│   │   ├── controller/      TaskController.java, AiController.java
│   │   ├── domain/
│   │   │   ├── dto/         TaskDtos.java  (Java records)
│   │   │   └── entity/      Task.java
│   │   ├── exception/       TaskNotFoundException, GlobalExceptionHandler
│   │   ├── repository/      TaskRepository.java
│   │   └── service/         TaskService.java, AiService.java
│   └── resources/
│       ├── application.properties
│       ├── static/index.html          (minimal frontend)
│       └── db/migration/V1__*.sql
└── test/
    ├── java/com/example/taskmanager/
    │   ├── controller/      AiControllerTest.java
    │   ├── service/         TaskServiceTest.java
    │   └── integration/     TaskControllerIntegrationTest.java
    └── resources/application-test.properties
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- An Anthropic API key (for the AI endpoint)

### Run

```bash
# Set your API key (or edit application.properties directly)
export ANTHROPIC_API_KEY=sk-ant-...

# Build and run
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**

- Frontend UI: http://localhost:8080
- H2 console:  http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:taskdb`)

### Run Tests

```bash
./mvnw test
```

## API Reference

### Tasks

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List all tasks (optional: `?status=TODO&priority=HIGH`) |
| GET | `/api/tasks/{id}` | Get a task by ID |
| POST | `/api/tasks` | Create a task |
| PATCH | `/api/tasks/{id}` | Partially update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |

#### Create Task (POST /api/tasks)
```json
{
  "title": "Buy groceries",
  "description": "Milk, eggs, bread",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2025-12-31"
}
```

Valid `status` values: `TODO`, `IN_PROGRESS`, `DONE`  
Valid `priority` values: `LOW`, `MEDIUM`, `HIGH`

#### Update Task (PATCH /api/tasks/{id})
All fields optional — only provided fields are updated:
```json
{
  "status": "IN_PROGRESS"
}
```

### AI Suggestions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/suggest` | Get AI-generated task breakdown |

```json
{ "prompt": "I need to plan a product launch for Q4" }
```

Response:
```json
{ "suggestion": "1. Define launch goals\n2. ..." }
```

## Design Notes

- **DTOs as Java records** — immutable, concise, no boilerplate
- **Partial updates via PATCH** — only non-null fields are applied
- **`@ControllerAdvice`** — all errors return consistent JSON: `{status, message, timestamp}`
- **AiService is isolated** — uses a plain `RestTemplate` call, easy to mock in unit tests
- **Flyway** — schema is in `V1__create_tasks_table.sql`, not Hibernate DDL, so schema changes are auditable
