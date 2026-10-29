# Student API — Nooruzbek Toktobaev

**Author:** Nooruzbek Toktobaev

**Description:** Spring Boot CRUD REST API for managing students (create, read, update, delete, search by last name) with PostgreSQL, Docker and a Jenkins pipeline — DevOps midterm of Nooruzbek Toktobaev.

## Stack

- Java 21, Spring Boot 3.5, Gradle (wrapper)
- PostgreSQL 16 (H2 in-memory for tests)
- Docker, Docker Compose
- Jenkins (declarative pipeline)

## Project structure

```
toktobaev-nooruzbek-student-api/
├── scripts/      # Toktobaev_Nooruzbek_system_report.sh — system monitoring script
├── logs/         # personal system report log + log analysis
├── backups/      # README backup
├── src/          # Spring Boot application source code and tests
├── Dockerfile
├── docker-compose.yml
├── Jenkinsfile
├── build.gradle
└── README.md
```

## REST API

| Method | URL                  | Description              |
|--------|----------------------|--------------------------|
| GET    | `/api/info`          | App name, author, version|
| GET    | `/api/students`      | List all students        |
| GET    | `/api/students?lastName=tok` | Search by last name (case-insensitive) |
| GET    | `/api/students/{id}` | Get student by id        |
| POST   | `/api/students`      | Create student           |
| PUT    | `/api/students/{id}` | Update student           |
| DELETE | `/api/students/{id}` | Delete student           |

Unknown id → `404 Not Found` with a JSON error body. Invalid data → `400 Bad Request`.

## Build and test

```bash
./gradlew clean build     # compile + run tests (H2, no PostgreSQL needed)
```

## Changes by Nooruzbek Toktobaev

### feature/toktobaev-nooruzbek-crud — search students by last name

- `GET /api/students?lastName=<text>` returns students whose last name contains `<text>`
  (case-insensitive, partial match), sorted by last name and first name.
- An empty or blank `lastName` returns the full list, so the old behaviour is unchanged.
- Covered by the integration test `StudentSearchTest` (runs on H2, no PostgreSQL needed).

## Run with Docker Compose

```bash
cp .env.example .env            # set your own POSTGRES_PASSWORD
docker compose up -d --build    # app on http://localhost:8080, PostgreSQL in the private network
docker compose ps
curl http://localhost:8080/api/info
docker compose down             # data stays in the volume toktobaev-nooruzbek-pgdata
```

Image: `toktobaev-nooruzbek-student-api:v1.1-toktobaev-nooruzbek` (multi-stage: Gradle on JDK 21 → JRE 21 alpine).
Postman collection: `postman/Toktobaev_Nooruzbek_Student_API.postman_collection.json`.

## CI with Jenkins

Jenkins job `Toktobaev_Nooruzbek_Student_API` runs the `Jenkinsfile`:
**Checkout → Build → Test → Docker Build → Verify**. The pipeline stops on the first failing stage.
Jenkins polls GitHub every 2 minutes (`pollSCM('H/2 * * * *')`), so every push to `main` starts a build automatically.
