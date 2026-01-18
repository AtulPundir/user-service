# User Service - Java 21 Spring Boot

A production-ready user management microservice built with Java 21 and Spring Boot 3.x, migrated from the Node.js/TypeScript implementation.

## Overview

This service provides:
- User profile management
- Subscription-based usage tracking
- Hierarchical user group management
- JWT authentication
- Service-to-service webhook integration

## Technology Stack

- **Java 21** (LTS)
- **Spring Boot 3.3.x**
- **Spring Data JPA** (PostgreSQL)
- **Spring Security** (JWT authentication)
- **Gradle** (Kotlin DSL)
- **Flyway** (database migrations)
- **Redis** (distributed locking, caching)
- **Docker** (containerization)

## Node.js to Java Mapping

| Node.js Component | Java Component |
|-------------------|----------------|
| Express.js routes | Spring @RestController |
| Prisma ORM | Spring Data JPA |
| Zod validation | Jakarta Validation |
| JWT middleware | Spring Security Filter |
| Winston logger | SLF4J/Logback |
| ioredis | Spring Data Redis |

## Project Structure

```
src/main/java/com/myapp/userservice/
├── controller/        # REST endpoints
├── service/           # Business logic
├── domain/            # JPA entities
├── repository/        # Data access layer
├── dto/               # Request/Response objects
│   ├── request/
│   └── response/
├── security/          # JWT authentication
├── config/            # Spring configuration
├── exception/         # Custom exceptions
└── util/              # Utilities
```

## Quick Start

### Prerequisites

- Java 21+
- PostgreSQL 15+
- Redis 7+
- Gradle 8.7+

### Local Development

1. **Clone and navigate to the project**
   ```bash
   cd /Users/atulpundir/Projects/MYPROJECT/myAppJava/user-service
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start dependencies (PostgreSQL & Redis)**
   ```bash
   docker-compose up -d postgres redis
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

5. **Access the service**
   - API: http://localhost:3002
   - Swagger UI: http://localhost:3002/swagger-ui.html
   - Health: http://localhost:3002/health

### Docker Deployment

```bash
# Build and run all services
docker-compose up --build

# Run in background
docker-compose up -d --build
```

## API Endpoints

### Users (`/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Create user |
| GET | `/users/me` | Get current user |
| GET | `/users` | List users (paginated) |
| GET | `/users/:id` | Get user by ID |
| PATCH | `/users/:id` | Update user |
| DELETE | `/users/:id` | Soft delete user |
| GET | `/users/:id/groups` | Get user's groups |

### Usage (`/users/:id/usage`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users/:id/usage/check` | Check usage allowance |
| POST | `/users/:id/usage/consume` | Consume usage |
| GET | `/users/:id/usage/current` | Get current month usage |
| GET | `/users/:id/usage/:year/:month` | Get specific month usage |
| GET | `/users/:id/usage?year=2024` | Get yearly usage |

### Groups (`/groups`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/groups` | Create group |
| POST | `/groups/with-users` | Create group with users |
| GET | `/groups` | List groups (paginated) |
| GET | `/groups/:id` | Get group details |
| PATCH | `/groups/:id` | Update group |
| DELETE | `/groups/:id` | Soft delete group |
| POST | `/groups/:id/members` | Add user to group |
| POST | `/groups/:id/members/bulk` | Bulk add users |
| GET | `/groups/:id/members` | Get group members |
| DELETE | `/groups/:id/members/:userId` | Remove user |
| GET | `/groups/:id/history` | Get membership history |

### Webhooks (`/webhooks`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/webhooks/subscription` | Handle subscription events |

## Authentication

The service uses JWT tokens from the auth-service:

```bash
# Include in request header
Authorization: Bearer <jwt-token>
```

JWT payload structure:
```json
{
  "userId": "user-id",
  "phone": "+1234567890",
  "role": "USER"
}
```

## Configuration

Key configuration in `application.yml`:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
  data:
    redis:
      url: ${REDIS_URL}

app:
  jwt:
    secret: ${JWT_SECRET}
  subscription-service:
    api-key: ${SUBSCRIPTION_SERVICE_API_KEY}
```

## Testing

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew test --tests "*.unit.*"

# Run integration tests
./gradlew test --tests "*.integration.*"

# Generate test report
./gradlew test jacocoTestReport
```

## Building

```bash
# Build executable JAR
./gradlew bootJar

# The JAR will be at: build/libs/user-service.jar

# Run the JAR
java -jar build/libs/user-service.jar
```

## Migration Notes

### Feature Parity

| Feature | Node.js | Java |
|---------|---------|------|
| User CRUD | Express routes | Spring REST Controller |
| Usage tracking | Prisma + Redis | JPA + Spring Redis |
| Distributed locking | ioredis SETNX | RedisTemplate |
| Idempotency | Redis cache | Spring Redis |
| Group hierarchy | Prisma relations | JPA relationships |
| JWT validation | jsonwebtoken | jjwt library |
| Validation | Zod schemas | Jakarta Validation |

### API Compatibility

All API endpoints maintain the same:
- URL paths
- HTTP methods
- Request/response JSON structure
- Error codes and messages
- Status codes

### Database Compatibility

The same PostgreSQL database schema is used. Flyway migrations create identical tables.

## Health Check

```bash
curl http://localhost:3002/health
```

Response:
```json
{
  "success": true,
  "message": "Service is healthy",
  "timestamp": "2024-01-15T10:00:00Z",
  "services": {
    "database": "connected",
    "redis": "connected"
  }
}
```

## License

Private - All rights reserved
