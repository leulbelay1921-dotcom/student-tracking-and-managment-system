# Backend

`sitms-backend` — Spring Boot 3 REST API for the Student Internship Tracking and Management System (SITMS). It exposes REST endpoints under the `/api` context path, enforces role-based access control (Keycloak OAuth2), persists to PostgreSQL via JPA, and applies schema migrations with Flyway.

## Technology stack

- **Java 17** + **Maven 3.9+**
- **Spring Boot 3.4.5** (Spring MVC, Spring Data JPA, Validation, Actuator, Mail)
- **OAuth2 Resource Server** — protected by Keycloak JWT bearer tokens
- **PostgreSQL 16** with **Flyway** migrations
- **OpenPDF** (PDF export) and **Apache POI** (Excel export)
- **Testcontainers** (Postgres) for integration tests

## Prerequisites

- JDK 17
- Maven 3.9+
- PostgreSQL 16
- A Keycloak realm named `sitms` with client `sitms-webapp` configured as the OIDC issuer

## Running the server

```bash
mvn spring-boot:run
```

Once started, the API is available at `http://localhost:8080/api` (defaults, overridable via environment variables — see [Configuration](#configuration)).

Main class: `com.sitms.SitmsApplication`.

## Building

```bash
mvn package
```

This produces the executable jar `target/sitms-backend-0.0.1-SNAPSHOT.jar`. Run it with:

```bash
java -jar target/sitms-backend-0.0.1-SNAPSHOT.jar
```

## Running tests

```bash
mvn test
```

Runs the unit and integration test suites (currently 377 tests). Database-backed integration tests use **Testcontainers** to spin up a Postgres instance; the surefire plugin is configured with `TESTCONTAINERS_RYUK_DISABLED=true` and `TESTCONTAINERS_PULL_TIMEOUT=180000`.

## Configuration

The server is configured entirely via environment variables. Sensible dev defaults are provided in `src/main/resources/application.yml`.

| Variable | Default | Description |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sitms` | JDBC datasource URL |
| `DB_USERNAME` | `sitms` | Database user |
| `DB_PASSWORD` | `sitms` | Database password |
| `JPA_DDL_AUTO` | `validate` | Hibernate DDL strategy (schema managed by Flyway) |
| `OIDC_ISSUER_URI` | `http://localhost:8081/realms/sitms` | Keycloak realm issuer for JWT validation |
| `OIDC_CLIENT_ID` | `sitms-webapp` | Expected JWT audience/client |
| `SERVER_PORT` | `8080` | HTTP port |
| `CONTEXT_PATH` | `/api` | Servlet context path |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Allowed CORS origins (comma-separated) |
| `MULTIPART_MAX_FILE_SIZE` | `12MB` | Max upload size |
| `MULTIPART_MAX_REQUEST_SIZE` | `12MB` | Max multipart request size |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | empty / `587` / empty / empty | SMTP settings |
| `MAIL_NOTIFICATIONS_ENABLED` | `false` | Toggles email notifications |
| `MAIL_NOTIFICATIONS_FROM` | `no-reply@sitms.local` | Sender address |

## API & security

- All endpoints are exposed under `/api`.
- Endpoints are protected as an OAuth2 resource server and authorize JWTs issued by Keycloak using role-based access control (`ADMIN`, `SUPERVISOR`, `STUDENT`).
- Spring Boot Actuator exposes `health` and `info`.

## Docker

Build the image from the Dockerfile:

```bash
docker build -t sitms-backend -f Dockerfile .
```

Or run the full stack (Postgres + Keycloak + backend + frontend) with the root `docker-compose.yml` — copy `.env.example` to `.env` and fill in the required credentials first:

```bash
docker compose up --build
```
