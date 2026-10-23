# SupportDesk

SupportDesk is a small helpdesk ticket tracker built with Spring Boot. Employees report problems
(login failures, network issues, broken applications) as tickets, and each ticket can be viewed,
moved between statuses, and assigned to an employee. It is a REST/JSON API only; there is no web UI.

It is a training application, so it is deliberately small and keeps all data in memory.

## Requirements

- Java 17
- Nothing else. Maven is provided by the wrapper (`./mvnw`), which downloads Maven on first use,
  so the first build needs internet access.

## Build, run, and test

```bash
./mvnw test                 # run the automated tests
./mvnw package              # build target/supportdesk-1.0.0.jar (also runs the tests)
./mvnw spring-boot:run      # start the application on http://localhost:2224
```

Stop the application with `Ctrl+C` in the terminal where it is running.

The default port is 2224. If that port is already in use, Spring Boot refuses to start. Use another port:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

(or set `server.port` in `src/main/resources/application.properties`). Use the same port in the
`curl` examples below.

## API

| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/api/health` | Health check | 200 |
| GET | `/api/tickets` | List tickets, ordered by ID; optional `status` and `assignee` filters | 200 |
| GET | `/api/tickets/{id}` | Get one ticket | 200 (404 if missing) |
| POST | `/api/tickets` | Create a ticket (status starts as `OPEN`) | 201 + `Location` header |
| PATCH | `/api/tickets/{id}/status` | Change status | 200 |
| PATCH | `/api/tickets/{id}/assignee` | Assign, or unassign with `null` | 200 |

A ticket looks like this:

```json
{
  "id": 1,
  "title": "Cannot log in",
  "description": "Login fails with an error after entering valid credentials",
  "status": "OPEN",
  "assignee": "alex"
}
```

Valid statuses: `OPEN`, `IN_PROGRESS`, `CLOSED`. Any status can change to any other status.

Filters on `GET /api/tickets` are optional and case-insensitive, and surrounding spaces in `assignee` are
ignored. When both are given, a ticket must match both. An invalid `status` returns 400, and no matches
returns an empty array.

```bash
curl -s "http://localhost:2224/api/tickets?status=OPEN&assignee=alex"
```

Validation (applied after trimming whitespace):

- `title`: 3 to 100 characters
- `description`: 1 to 500 characters
- `assignee`: optional; when given, 1 to 60 characters. `null` means unassigned.

### Examples

```bash
# Health check
curl -s http://localhost:2224/api/health
# {"status":"UP"}

# List and retrieve
curl -s http://localhost:2224/api/tickets
curl -s http://localhost:2224/api/tickets/1

# Create a ticket
curl -s -X POST http://localhost:2224/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Network is unavailable",
    "description": "Cannot connect to the office network",
    "assignee": "alex"
  }'
# {"id":7,"title":"Network is unavailable","description":"Cannot connect to the office network","status":"OPEN","assignee":"alex"}

# Change status
curl -s -X PATCH http://localhost:2224/api/tickets/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'

# Assign, then unassign
curl -s -X PATCH http://localhost:2224/api/tickets/1/assignee \
  -H "Content-Type: application/json" \
  -d '{"assignee":"morgan"}'
curl -s -X PATCH http://localhost:2224/api/tickets/1/assignee \
  -H "Content-Type: application/json" \
  -d '{"assignee":null}'
```

### Errors

Errors are returned as JSON with a matching HTTP status. No stack traces are exposed.

```json
{ "error": "NOT_FOUND", "message": "Ticket 99 was not found" }
```

| `error` | HTTP status | Meaning |
|---|---|---|
| `NOT_FOUND` | 404 | The ticket does not exist |
| `VALIDATION_ERROR` | 400 | A field or status value is invalid |
| `MALFORMED_REQUEST` | 400 | The request body is missing or not valid JSON |

## Project structure

```text
src/main/java/com/alta3/supportdesk/
├── SupportDeskApplication.java   Application entry point
├── controller/                   HTTP endpoints (TicketController, HealthController)
├── service/                      Business logic and validation (TicketService),
│                                 plus SampleDataLoader, which loads the sample tickets
├── repository/                   In-memory ticket storage (TicketRepository)
├── model/                        Ticket and TicketStatus
├── dto/                          Request bodies and the error response
└── exception/                    Exceptions and the global error handler
src/test/java/com/alta3/supportdesk/
└── TicketApiTest.java            API tests using MockMvc
```

## Important behavior

- **Storage is in memory.** Tickets live in a `ConcurrentHashMap`; nothing is written to disk.
- **Data resets on every restart.** There is no database to configure or clean up.
- **Six sample tickets are loaded automatically** at startup (IDs 1 to 6; two are assigned to `alex`, two to `morgan`, and two are unassigned).
  The first ticket you create gets ID 7.
- Each test resets the ticket data to the six sample tickets, so tests do not affect each other.
