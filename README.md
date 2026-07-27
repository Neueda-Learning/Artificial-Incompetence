# Artificial-Incompetence

HSBC team portfolio management training project.

## Technology

- Java 21
- Spring Boot
- Maven
- MySQL 8.4
- Docker Compose

## Project structure

```text
.
├── backend/          Spring Boot REST API and Dockerfile
├── compose.yaml      Backend and MySQL services
├── .env.example      Environment variable template
└── portfolio_manager.md
```

## Configure Docker

Create a local `.env` file and replace both development passwords:

```bash
cp .env.example .env
```

The `.env` file is ignored by Git and must not be committed.

## Build and start the complete stack

```bash
docker compose up --build -d
```

Check service status and logs:

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f database
```

The API is available at `http://localhost:8080`.
The health endpoint is `http://localhost:8080/actuator/health`.

Stop the services without deleting MySQL data:

```bash
docker compose down
```

Rebuild only the backend after a code change:

```bash
docker compose up --build -d backend
```

## Run the backend outside Docker

Start only MySQL:

```bash
docker compose up -d database
```

Run Spring Boot locally:

```bash
cd backend
DB_USERNAME=portfolio_user \
DB_PASSWORD=your_password_from_dot_env \
mvn spring-boot:run
```

## Initial API

List portfolio items:

```bash
curl http://localhost:8080/api/portfolio/items
```

Create an item:

```bash
curl -X POST http://localhost:8080/api/portfolio/items \
  -H 'Content-Type: application/json' \
  -d '{"assetType":"STOCK","symbol":"AAPL","quantity":10}'
```

Delete an item:

```bash
curl -X DELETE http://localhost:8080/api/portfolio/items/1
```

## Run tests

Tests use an in-memory H2 database and do not require Docker:

```bash
cd backend
mvn test
```

## Continuous integration

GitHub Actions runs the backend CI workflow for pushes and pull requests
targeting `main`. The workflow:

1. Sets up Java 21.
2. Runs Maven verification, including tests and packaging.
3. Builds the backend Docker image without pushing it.

The workflow can also be started manually from the repository's **Actions**
page. Its definition is in `.github/workflows/ci.yml`.
