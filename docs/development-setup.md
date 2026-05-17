# Development Setup

## Prerequisites

- Docker / Podman.
- JDK 25 LTS.
- Gradle.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/vertyll/veds.git
cd veds
```

### 2. Start the infrastructure

```bash
docker-compose up -d
```

### 3. Build and run microservices

**Building:**
```bash
cd <service-name>
./gradlew build
```

**Local running:**
```bash
cd <service-name>
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Available services:**
- `api-gateway`.
- `iam-service`.
- `mail-service`.
- `template-service` (reference template service).
- `shared-infrastructure` (library).
- `iam-contracts`, `mail-contracts`, `template-contracts` (Apache Avro contracts).

## Service URLs

| Service      | URL                   |
|--------------|-----------------------|
| API Gateway  | http://localhost:8080 |
| IAM Service  | http://localhost:8082 |
| Mail Service | http://localhost:8083 |
| Keycloak     | http://localhost:9000 |
| Kafka UI     | http://localhost:8090 |
| MailDev      | http://localhost:1080 |

## API Documentation (Swagger UI)

Each service provides its own Swagger UI:

| Service          | URL                                   |
|------------------|---------------------------------------|
| IAM Service      | http://localhost:8082/swagger-ui.html |
| Mail Service     | http://localhost:8083/swagger-ui.html |
| Template Service | http://localhost:8084/swagger-ui.html |

## API Testing (Insomnia)

An Insomnia collection is provided at `insomnia-collection.yaml` in the project root.

## Monitoring

Each service exposes health and metrics endpoints through Spring Boot Actuator:
- Health checks: `/actuator/health` on each service.
- Metrics can be collected for observability and monitoring service health.

## Code Style & Formatting

The project uses ktlint for code formatting and style checks.

```bash
# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck
```
