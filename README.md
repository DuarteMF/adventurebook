# AdventureBook

AdventureBook is a Spring Boot backend for managing and playing text-based adventure stories. It lets you browse valid story collections, start an adventure from a chosen story, make decisions through story branches, and continue or pause saved sessions.

The project loads story data from JSON book files on startup, validates them, stores them in a database, and exposes the playable content through REST endpoints.

## What this project does

The application supports:

- Browsing story catalog entries
  - Search by title/author
  - Filter by difficulty
  - Paginated results
- Loading book data from JSON files under `src/main/resources/books`
- Loading new books (in JSON format) via a web interface
- Validating imported stories before they can be played
- Starting a new adventure from a valid story
- Progressing through story sections by choosing options
- Tracking adventure state such as:
  - in progress
  - paused
  - abandoned
  - won
  - died
- Saving and resuming adventures
- Auto-cleaning stale unsaved adventures using a scheduled task

## Core domain

The app is split into two main parts:

- Story domain
  - Story catalog
  - Story sections
  - Story options
  - Difficulty and validation rules
- Adventure domain
  - Active game sessions
  - Health calculation and branch progression
  - Game status transitions

## Technologies used

This project is built with:

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- H2 database
- Lombok
- SpringDoc OpenAPI / Swagger UI
- ShedLock for scheduled task locking
- Maven wrapper for local builds
- Jackson for JSON ingestion

## Project structure

Key folders include:

- `src/main/java/com/demo/adventurebook/story` — story catalog and validation logic
- `src/main/java/com/demo/adventurebook/adventure` — gameplay state and progression logic
- `src/main/resources/books` — story JSON files loaded at startup
- `src/main/resources/application.yaml` — app configuration

## Local run instructions

### Prerequisites

- JDK 25 installed and available on your PATH
- Maven wrapper is included, so you do not need a separate Maven install
- Git (optional, for cloning)

### Run the app on Windows

From the project root:

```powershell
./mvnw.cmd clean spring-boot:run
```

### Run the app on macOS/Linux

```bash
./mvnw clean spring-boot:run
```

The app starts on:

- API: http://localhost:8080
- H2 console: http://localhost:8080/h2-console
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## Default database configuration

The application uses an embedded H2 file database:

- JDBC URL: `jdbc:h2:file:./data/adventurebook;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE`
- Username: `sa`
- Password: empty

The database files are stored under the `data/` directory in the project.

## Example endpoints

### Stories

- `GET /stories` — list valid stories with pagination and filtering
- `POST /stories` — create a new story

### Adventures

- `POST /adventures` — create a new adventure
- `GET /adventures/{id}` — fetch current adventure state
- `POST /adventures/{id}/choices` — choose an option in the current section
- `PATCH /adventures/{id}/pause` — pause an adventure
- `PATCH /adventures/{id}/resume` — resume a paused adventure
- `PATCH /adventures/{id}/stop` — abandon an adventure
- `PATCH /adventures/{id}/save` — mark an adventure as saved

## Notes about startup behavior

On application startup, the app scans all JSON files in `src/main/resources/books` and imports any stories that are not already present in the database. Invalid or malformed story files are recorded as invalid story shells instead of crashing the app.

The scheduled cleanup task runs daily at 03:00 UTC and removes stale unsaved adventures older than 24 hours.

## Useful commands

### Build the project

```powershell
./mvnw.cmd clean package
```

### Run tests

```powershell
./mvnw.cmd test
```
