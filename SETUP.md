# Identity Service - Setup & Migration Guide

## Prerequisites

Before starting, ensure you have:

- **Java 21** - Download from [Adoptium](https://adoptium.net/)
- **PostgreSQL 15+** - Same database as Node.js service
- **Redis 7+** - For distributed locking
- **Gradle 8.7+** - Build tool (or use included wrapper)

## Environment Setup

### 1. Java Installation

```bash
# macOS with Homebrew
brew install openjdk@21

# Verify installation
java -version
# Should show: openjdk version "21.x.x"
```

### 2. Database Setup

The Java service uses the same database as the Node.js service. If migrating:

```bash
# Option A: Use existing database
# Just point DATABASE_URL to your existing PostgreSQL

# Option B: Create new database for testing
createdb user_db
```

### 3. Redis Setup

```bash
# macOS with Homebrew
brew install redis
brew services start redis

# Or use Docker
docker run -d -p 6379:6379 redis:7-alpine
```

### 4. Environment Configuration

```bash
# Copy example env file
cp .env.example .env

# Edit with your values
nano .env
```

Required variables:
```
DATABASE_URL=jdbc:postgresql://localhost:5432/user_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
REDIS_URL=redis://localhost:6379
JWT_SECRET=your-jwt-secret-from-auth-service
SUBSCRIPTION_SERVICE_API_KEY=your-api-key
```

**Important**: Use the SAME `JWT_SECRET` as your auth-service!

## Running the Service

### Development Mode

```bash
# Run with hot reload
./gradlew bootRun

# With specific profile
SPRING_PROFILES_ACTIVE=development ./gradlew bootRun
```

### Production Mode

```bash
# Build JAR
./gradlew bootJar

# Run JAR
java -jar build/libs/identity-service.jar
```

### Docker Mode

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f identity-service

# Stop services
docker-compose down
```

## Database Migration

Flyway handles database migrations automatically on startup.

### Manual Migration (if needed)

```bash
# Run migrations only
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo
```

### Migration Files

Located at: `src/main/resources/db/migration/`

- `V1__initial_schema.sql` - Creates all tables and indexes

## Verification

### 1. Health Check

```bash
curl http://localhost:3002/health
```

Expected response:
```json
{
  "success": true,
  "message": "Service is healthy",
  "services": {
    "database": "connected",
    "redis": "connected"
  }
}
```

### 2. API Documentation

Open in browser: http://localhost:3002/swagger-ui.html

### 3. Test Endpoints

```bash
# Get a JWT token from auth-service first
TOKEN="your-jwt-token"

# List users
curl -H "Authorization: Bearer $TOKEN" http://localhost:3002/users

# Get current user
curl -H "Authorization: Bearer $TOKEN" http://localhost:3002/users/me
```

## Running Tests

```bash
# All tests
./gradlew test

# With coverage report
./gradlew test jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html
```

## Switching from Node.js to Java

### Step 1: Stop Node.js Service

```bash
# In the Node.js service directory
npm stop
# or
pm2 stop identity-service
```

### Step 2: Start Java Service

```bash
# In the Java service directory
./gradlew bootRun
# or
docker-compose up -d
```

### Step 3: Update Load Balancer/Proxy (if applicable)

Point traffic from Node.js port to Java port (both use 3002 by default).

### Step 4: Verify

Test all critical endpoints:

```bash
# Create user
curl -X POST http://localhost:3002/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"test","name":"Test","email":"test@test.com","phone":"+1234567890"}'

# Check usage
curl -X POST http://localhost:3002/users/test/usage/check \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 10}'
```

## Rollback Procedure

If issues occur:

1. Stop Java service
2. Start Node.js service
3. Traffic will resume on Node.js

```bash
# Stop Java
docker-compose down
# or
pkill -f identity-service.jar

# Start Node.js (in Node.js directory)
npm start
```

## Troubleshooting

### Database Connection Failed

```
Check:
1. PostgreSQL is running
2. DATABASE_URL is correct
3. User has permissions
```

### Redis Connection Failed

```
Check:
1. Redis is running
2. REDIS_URL is correct
3. No firewall blocking port 6379
```

### JWT Validation Failed

```
Check:
1. JWT_SECRET matches auth-service
2. Token is not expired
3. Token has required claims (userId)
```

### Port Already in Use

```bash
# Find process using port 3002
lsof -i :3002

# Kill the process
kill -9 <PID>
```

## Performance Tuning

### JVM Options

```bash
# For containers with limited memory
java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar

# For production with more memory
java -Xms512m -Xmx2g -jar app.jar
```

### Connection Pools

In `application.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

## Support

For issues:
1. Check logs: `docker-compose logs identity-service`
2. Enable debug logging: `SPRING_PROFILES_ACTIVE=development`
3. Check health endpoint: `/health`
