# User Service - Architecture & Technical Documentation

## Overview

The User Service is a microservice responsible for managing users, groups, and usage tracking. It's built with Java 21 and Spring Boot 3.3, designed for scalability, security, and maintainability.

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 LTS | Runtime platform with modern features (records, pattern matching, virtual threads) |
| Spring Boot | 3.3.0 | Application framework for rapid development |
| Spring Data JPA | 3.3.0 | Database abstraction and ORM |
| PostgreSQL | 15+ | Primary relational database |
| Redis | 7+ | Distributed locking and caching |
| Flyway | 10.x | Database migration management |
| JWT (jjwt) | 0.12.x | Authentication token handling |
| Gradle (Kotlin DSL) | 8.x | Build tool and dependency management |
| Docker | - | Containerization |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
│                    (Web, Mobile, Other Services)                 │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway / Load Balancer              │
└─────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
            ┌──────────────┐                ┌──────────────┐
            │  JWT Token   │                │   API Key    │
            │    Auth      │                │    Auth      │
            └──────────────┘                └──────────────┘
                    │                               │
                    └───────────────┬───────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                         User Service                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Users     │  │   Groups    │  │   Usage     │              │
│  │ Controller  │  │ Controller  │  │ Controller  │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
│         │                │                │                      │
│         ▼                ▼                ▼                      │
│  ┌─────────────────────────────────────────────────┐            │
│  │                 Service Layer                    │            │
│  │  UserService  │  GroupService  │  UsageService  │            │
│  └─────────────────────────────────────────────────┘            │
│                          │                                       │
│         ┌────────────────┼────────────────┐                     │
│         ▼                ▼                ▼                     │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐              │
│  │    JPA     │   │    JPA     │   │   Redis    │              │
│  │ Repository │   │ Repository │   │  Template  │              │
│  └──────┬─────┘   └──────┬─────┘   └──────┬─────┘              │
└─────────┼────────────────┼────────────────┼─────────────────────┘
          │                │                │
          ▼                ▼                ▼
   ┌────────────┐   ┌────────────┐   ┌────────────┐
   │ PostgreSQL │   │ PostgreSQL │   │   Redis    │
   │  (Users)   │   │  (Groups)  │   │  (Locks)   │
   └────────────┘   └────────────┘   └────────────┘
```

---

## Why Each Technology?

### 1. PostgreSQL - Primary Database

**Purpose:** Stores all persistent data (users, groups, memberships, usage records)

**Why PostgreSQL?**
- **ACID compliance**: Guarantees data integrity for financial/usage tracking
- **Rich data types**: Native support for JSON, arrays, timestamps with timezone
- **Advanced indexing**: B-tree, GIN, GiST indexes for optimized queries
- **Robust transactions**: Essential for group membership audit trails
- **Scalability**: Supports read replicas, partitioning for high-volume data

**Used for:**
- User profiles and authentication data
- Group hierarchies (parent-child relationships)
- Membership audit logs (who added/removed whom and when)
- Monthly usage tracking per user

### 2. Redis - Distributed Locking & Caching

**Purpose:** Prevents race conditions in usage consumption; caches frequently accessed data

**Why Redis?**
- **Atomic operations**: SETNX for distributed locks
- **Sub-millisecond latency**: Critical for usage limit checks
- **TTL support**: Automatic lock expiration prevents deadlocks
- **High availability**: Supports clustering and replication

**Used for:**
- **Distributed locking**: When consuming usage, prevents double-spending
  ```
  Lock key: usage:lock:{userId}:{year}:{month}
  TTL: 30 seconds
  ```
- **Idempotency**: Prevents duplicate usage consumption requests
  ```
  Key: usage:idempotency:{idempotencyKey}
  TTL: 24 hours
  ```

**Example flow:**
```
1. User requests to consume 10 tasks
2. Service acquires Redis lock for user's monthly usage
3. Check current usage in PostgreSQL
4. If within limit, update PostgreSQL
5. Release Redis lock
6. Return result
```

### 3. Spring Boot 3.3 - Application Framework

**Purpose:** Provides production-ready features out of the box

**Why Spring Boot?**
- **Auto-configuration**: Minimal boilerplate
- **Embedded server**: No external Tomcat needed
- **Actuator**: Health checks, metrics, monitoring
- **Security**: Integrated authentication/authorization
- **Ecosystem**: Vast library of starters

**Key Spring modules used:**
| Module | Purpose |
|--------|---------|
| Spring Web | REST API endpoints |
| Spring Data JPA | Database operations |
| Spring Data Redis | Redis operations |
| Spring Security | Authentication & authorization |
| Spring Validation | Request validation |
| Spring Actuator | Health & metrics endpoints |

### 4. JWT (JSON Web Tokens) - Authentication

**Purpose:** Stateless authentication for API requests

**Why JWT?**
- **Stateless**: No session storage needed
- **Self-contained**: Contains user ID and roles
- **Scalable**: Works across multiple service instances
- **Standard**: Widely supported, interoperable

**Token structure:**
```json
{
  "sub": "user-id-123",           // User ID
  "roles": ["USER", "ADMIN"],     // User roles
  "iat": 1705123456,              // Issued at
  "exp": 1705209856               // Expiration
}
```

### 5. Flyway - Database Migrations

**Purpose:** Version-controlled database schema changes

**Why Flyway?**
- **Version control**: Track schema changes in Git
- **Repeatable**: Same migrations run identically everywhere
- **Rollback support**: Undo migrations if needed
- **Team collaboration**: Multiple developers can add migrations

**Migration naming:**
```
V1__initial_schema.sql
V2__add_user_groups.sql
V3__add_usage_tracking.sql
```

### 6. Gradle (Kotlin DSL) - Build Tool

**Purpose:** Dependency management and build automation

**Why Gradle with Kotlin DSL?**
- **Type safety**: IDE auto-completion and error checking
- **Performance**: Incremental builds, build cache
- **Flexibility**: Custom tasks and plugins
- **Modern**: Better than Maven for complex builds

---

## Core Features

### 1. User Management

| Feature | Description |
|---------|-------------|
| Create User | Register new users with email, phone, name |
| Get User | Retrieve user profile with usage history |
| Update User | Modify user details (name, email, status) |
| Delete User | Soft delete (sets status to DELETED) |
| List Users | Paginated list with filters (Admin only) |

**User statuses:**
- `ACTIVE` - Normal operational state
- `INACTIVE` - Temporarily disabled
- `DELETED` - Soft deleted, retained for audit

### 2. Group Management

| Feature | Description |
|---------|-------------|
| Create Group | Create with optional initial members |
| Hierarchical Groups | Parent-child relationships |
| Add/Remove Members | With full audit trail |
| Bulk Operations | Add multiple users at once |
| Duplicate Detection | Prevents groups with same name AND members |

**Duplicate group logic:**
```
Two groups are considered duplicates if:
1. Same name (case insensitive) AND
2. Exactly the same set of members

This allows:
- Multiple "Team Alpha" groups with different members ✓
- Same members in groups with different names ✓

This blocks:
- Same name + same members ✗
```

### 3. Usage Tracking

| Feature | Description |
|---------|-------------|
| Check Usage | Verify if user can consume N tasks |
| Consume Usage | Deduct tasks with distributed locking |
| Monthly Limits | Per-user configurable limits |
| Unlimited Plans | Support for unlimited usage (-1) |
| Usage History | Track last 12 months of usage |

**Usage consumption flow:**
```
1. Acquire distributed lock (Redis)
2. Check idempotency key (Redis)
3. Load current usage (PostgreSQL)
4. Validate against limit
5. Update usage (PostgreSQL)
6. Store idempotency key (Redis)
7. Release lock
8. Return result
```

### 4. Authentication & Authorization

| Method | Use Case |
|--------|----------|
| JWT Token | User-facing API requests |
| API Key | Service-to-service (webhooks) |

**Role-based access:**
- `USER` - Standard user operations
- `ADMIN` - List all users, system operations
- `SERVICE` - Webhook endpoints

---

## API Endpoints

### Users (`/users`)
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/users/create-user` | Authenticated | Create new user |
| GET | `/users/me` | Authenticated | Get current user |
| GET | `/users` | Admin only | List users with filters |
| GET | `/users/{id}` | Authenticated | Get user by ID |
| PATCH | `/users/{id}` | Authenticated | Update user |
| DELETE | `/users/{id}` | Authenticated | Soft delete user |
| GET | `/users/{id}/groups` | Authenticated | Get user's groups |

### Groups (`/groups`)
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/groups` | Authenticated | Create group (with optional users) |
| GET | `/groups` | Authenticated | List groups |
| GET | `/groups/{id}` | Authenticated | Get group details |
| PATCH | `/groups/{id}` | Authenticated | Update group |
| DELETE | `/groups/{id}` | Authenticated | Soft delete group |
| POST | `/groups/{id}/users` | Authenticated | Add user to group |
| POST | `/groups/{id}/users/bulk` | Authenticated | Bulk add users |
| DELETE | `/groups/{id}/users/{userId}` | Authenticated | Remove user |
| GET | `/groups/{id}/members` | Authenticated | Get current members |
| GET | `/groups/{id}/history` | Authenticated | Get membership history |

### Usage (`/usage`)
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/usage/check` | Authenticated | Check if usage allowed |
| POST | `/usage/consume` | Authenticated | Consume usage |
| GET | `/usage/current` | Authenticated | Get current month usage |
| GET | `/usage/{year}/{month}` | Authenticated | Get specific month |

### Webhooks (`/webhooks`)
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/webhooks/subscription-updated` | API Key | Handle subscription changes |

### Health (`/health`)
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/health` | Public | Service health check |

---

## Database Schema

### Entity Relationships

```
┌─────────────┐       ┌─────────────────────┐       ┌─────────────┐
│    User     │       │ UserGroupMembership │       │  UserGroup  │
├─────────────┤       ├─────────────────────┤       ├─────────────┤
│ id (PK)     │◄──────│ user_id (FK)        │       │ id (PK)     │
│ auth_user_id│       │ group_id (FK)       │──────►│ name        │
│ name        │       │ action (ADDED/      │       │ description │
│ email       │       │         REMOVED)    │       │ parent_id   │──┐
│ phone       │       │ performed_by        │       │ is_active   │  │
│ is_verified │       │ created_at          │       │ created_at  │  │
│ status      │       └─────────────────────┘       │ updated_at  │  │
│ monthly_limit│                                    └─────────────┘  │
│ plan_id     │                                           ▲          │
│ created_at  │       ┌─────────────────────┐             │          │
│ updated_at  │       │ UserMonthlyUsage    │             └──────────┘
└─────────────┘       ├─────────────────────┤             (self-ref)
       │              │ id (PK)             │
       │              │ user_id (FK)        │
       └─────────────►│ year                │
                      │ month               │
                      │ monthly_limit       │
                      │ utilised            │
                      │ created_at          │
                      │ updated_at          │
                      └─────────────────────┘
```

---

## Security

### Authentication Flow

```
┌────────┐     ┌─────────────┐     ┌──────────────┐     ┌────────────┐
│ Client │────►│ JWT Filter  │────►│ Validate JWT │────►│ Controller │
└────────┘     └─────────────┘     └──────────────┘     └────────────┘
                     │                    │
                     │              ┌─────▼─────┐
                     │              │ Extract   │
                     │              │ User ID & │
                     │              │ Roles     │
                     │              └───────────┘
                     │
              ┌──────▼──────┐
              │ API Key     │ (for webhooks)
              │ Filter      │
              └─────────────┘
```

### Security Best Practices

1. **Password-less**: Users authenticated via external auth provider
2. **JWT expiration**: Tokens expire after configured time
3. **Role-based**: Different access levels (USER, ADMIN, SERVICE)
4. **API Key rotation**: Webhook API keys can be rotated
5. **Input validation**: All requests validated with Jakarta Validation
6. **SQL injection prevention**: Parameterized queries via JPA

---

## Running the Service

### Prerequisites
- Java 21
- PostgreSQL 15+
- Redis 7+
- Docker (optional)

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | 3002 |
| `DATABASE_URL` | PostgreSQL JDBC URL | - |
| `DATABASE_USERNAME` | DB username | postgres |
| `DATABASE_PASSWORD` | DB password | - |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | - |
| `SUBSCRIPTION_SERVICE_API_KEY` | Webhook API key | - |

### Start with Docker Compose

```bash
# Start all services
docker-compose up -d

# Start only dependencies (for local development)
docker-compose up -d postgres redis

# Run the app locally
./gradlew bootRun
```

### Start Locally

```bash
# Start PostgreSQL and Redis separately, then:
DATABASE_URL=jdbc:postgresql://localhost:5432/user_db \
DATABASE_USERNAME=postgres \
DATABASE_PASSWORD=yourpassword \
REDIS_HOST=localhost \
JWT_SECRET=your-super-secret-jwt-key-must-be-at-least-32-characters \
./gradlew bootRun
```

---

## Testing

### Run Tests

```bash
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests "com.myapp.userservice.unit.*"

# Integration tests (requires Docker)
./gradlew test --tests "com.myapp.userservice.integration.*"
```

### Test Coverage

| Layer | Coverage |
|-------|----------|
| Service | Unit tests with mocked dependencies |
| Repository | Integration tests with test containers |
| Controller | Integration tests with MockMvc |

---

## Monitoring

### Health Endpoint

```bash
curl http://localhost:3002/health
```

### Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Application metrics |

---

## Future Enhancements

1. **Caching**: Redis cache for frequently accessed users/groups
2. **Event sourcing**: Publish events for user/group changes
3. **Rate limiting**: Per-user API rate limits
4. **Audit logging**: Comprehensive audit trail
5. **Multi-tenancy**: Support for multiple organizations
