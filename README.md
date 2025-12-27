# Surest Member Management Application

A production-ready Spring Boot REST API for managing member information with JWT authentication and role-based authorization. Built following Domain-Driven Design principles with comprehensive test coverage and modern Java best practices.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Testing](#testing)
- [Configuration](#configuration)
- [Database](#database)
- [Development](#development)
- [License](#license)

## Features

- **Member Management (CRUD Operations)**
  - Create, read, update, and delete member records
  - Pagination, sorting, and filtering support
  - Member caching for optimized performance
  - UUID-based unique identifiers

- **Authentication & Authorization**
  - JWT-based stateless authentication
  - Role-based access control (USER and ADMIN roles)
  - Secure password storage with BCrypt hashing
  - Token expiration and validation

- **Performance & Scalability**
  - In-memory caching for frequently accessed data
  - Database indexing on key columns
  - Efficient pagination for large datasets

- **API Documentation**
  - Interactive Swagger UI
  - OpenAPI 3.0 specification
  - Comprehensive endpoint documentation

## Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 4.0.1 |
| **Build Tool** | Gradle | 9.2.1 |
| **Database** | PostgreSQL | 16 |
| **ORM** | Spring Data JPA | (Spring Boot managed) |
| **Migration** | Flyway | (Spring Boot managed) |
| **Security** | Spring Security + JWT | JJWT 0.12.3 |
| **API Docs** | SpringDoc OpenAPI | 3.0.0 |
| **Testing** | JUnit 5 + Mockito | (Spring Boot managed) |
| **Code Coverage** | JaCoCo | 0.8.11 |
| **Caching** | Spring Cache | (Spring Boot managed) |
| **Logging** | SLF4J + Logback | (Spring Boot managed) |

## Project Structure

```
src/main/java/com/tietoevry/surestapp/
├── SurestApp.java                          # Spring Boot application entry point
├── config/                                  # Configuration classes
│   ├── SecurityConfiguration.java          # Spring Security + JWT setup
│   ├── OpenApiConfiguration.java           # Swagger/OpenAPI configuration
│   ├── JwtProperties.java                  # JWT properties binding
│   └── CacheConfiguration.java             # Cache configuration
├── controller/                              # REST API endpoints
│   ├── MemberController.java               # Member CRUD operations
│   └── AuthenticationController.java       # Login/authentication
├── service/                                 # Business logic layer
│   ├── MemberService.java                  # Member business logic
│   └── AuthenticationService.java          # Authentication logic
├── repository/                              # Data access layer
│   ├── MemberRepository.java               # Member JPA repository
│   ├── UserRepository.java                 # User JPA repository
│   └── RoleRepository.java                 # Role JPA repository
├── domain/                                  # Domain entities
│   ├── Member.java                         # Member entity
│   ├── User.java                           # User entity
│   └── Role.java                           # Role enumeration
├── dto/                                     # Data transfer objects
│   ├── request/                            # Request DTOs
│   │   ├── CreateMemberRequest.java
│   │   ├── UpdateMemberRequest.java
│   │   └── LoginRequest.java
│   └── response/                           # Response DTOs
│       ├── MemberResponse.java
│       ├── PagedMemberResponse.java
│       ├── LoginResponse.java
│       └── ErrorResponse.java
├── exception/                               # Exception handling
│   ├── GlobalExceptionHandler.java         # Global exception handler
│   ├── MemberNotFoundException.java
│   ├── DuplicateEmailException.java
│   └── InvalidCredentialsException.java
├── security/                                # Security components
│   ├── JwtAuthenticationFilter.java        # JWT validation filter
│   ├── JwtAuthenticationEntryPoint.java    # Unauthorized handler
│   └── UserDetailsServiceImpl.java         # User details service
└── util/                                    # Utility classes
    ├── JwtUtil.java                        # JWT token operations
    └── PasswordHashGenerator.java          # Password hash generator

src/main/resources/
├── application.yml                          # Application configuration
├── logback-spring.xml                       # Logging configuration
└── db/migration/                            # Database migrations
    ├── V1__Create_schema.sql               # Schema creation
    └── V2__Seed_data.sql                   # Initial data seeding

src/test/java/                               # Unit tests
src/integration-test/java/                   # Integration tests
```

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Docker** and **Docker Compose** (for PostgreSQL)
- **Gradle 9.2.1** or higher (or use included Gradle wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd surest-member-app
   ```

2. **Start PostgreSQL with Docker Compose**
   ```bash
   docker compose up -d
   ```
   This will start PostgreSQL 16 on `localhost:5432` with database `surest_db`.

3. **Build the project**
   ```bash
   ./gradlew clean build
   ```

### Running the Application

**Using Gradle:**
```bash
./gradlew bootRun
```

**Using JAR file:**
```bash
java -jar build/libs/surest-app-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

**Verify it's running:**
```bash
curl http://localhost:8080/actuator/health
```

## API Documentation

### Interactive Documentation

Once the application is running, access the Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

### Authentication Endpoints

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ROLE_ADMIN"
}
```

### Member Endpoints

All member endpoints require a valid JWT token in the `Authorization` header:
```
Authorization: Bearer <your-jwt-token>
```

#### Get All Members (Paginated)
```http
GET /api/v1/members?page=0&size=10&sort=firstName&direction=ASC
```

**Query Parameters:**
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 10
- `sort` (optional): Sort field, default "createdAt"
- `direction` (optional): Sort direction (ASC/DESC), default "DESC"
- `firstName` (optional): Filter by first name
- `lastName` (optional): Filter by last name

**Authorization:** USER or ADMIN

#### Get Member by ID
```http
GET /api/v1/members/{id}
```

**Authorization:** USER or ADMIN

#### Create Member
```http
POST /api/v1/members
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15",
  "email": "john.doe@example.com"
}
```

**Authorization:** ADMIN only

#### Update Member
```http
PUT /api/v1/members/{id}
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith",
  "dateOfBirth": "1990-01-15",
  "email": "john.smith@example.com"
}
```

**Authorization:** ADMIN only

#### Delete Member
```http
DELETE /api/v1/members/{id}
```

**Authorization:** ADMIN only

## Security

### JWT Authentication

The application uses JWT (JSON Web Tokens) for stateless authentication:

- **Token Expiration:** 24 hours (86400000 ms)
- **Algorithm:** HMAC-SHA256
- **Token Claims:** username, role, issued-at, expiration

### Default Users

Two users are pre-seeded in the database:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `password` | ROLE_ADMIN |
| `user` | `password` | ROLE_USER |

**Note:** Change these credentials in production!

### Authorization Rules

- **Read Operations** (GET): Accessible by USER and ADMIN roles
- **Write Operations** (POST, PUT, DELETE): Accessible by ADMIN role only

### Security Configuration

- CSRF disabled (stateless API)
- Stateless session management
- JWT filter processes all requests except `/api/v1/auth/**` and Swagger endpoints
- BCrypt password encoding with strength 10

## Testing

The project includes comprehensive unit tests with high code coverage requirements.

### Run All Tests
```bash
./gradlew test
```

### Run Integration Tests
```bash
./gradlew integrationTest
```

### Generate Coverage Report
```bash
./gradlew test jacocoTestReport
```

Coverage report will be available at: `build/reports/jacoco/test/html/index.html`

### Coverage Requirements

- **Minimum Coverage:** 80%
- **Excluded Packages:** Domain entities and configuration classes

### Test Naming Convention

Tests follow the `should_when` pattern:
```java
@Test
@DisplayName("Should return member when valid ID is provided")
void should_returnMember_when_validIdProvided() {
    // Test implementation
}
```

## Configuration

### Application Configuration

Key configuration properties in `application.yml`:

```yaml
spring:
  application:
    name: surest-member-management

  datasource:
    url: jdbc:postgresql://localhost:5432/surest_db
    username: surestuser
    password: password123

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

  cache:
    type: simple
    cache-names: members

jwt:
  secret: <your-secret-key>
  expirationMs: 86400000

logging:
  level:
    com.tietoevry.surestapp: INFO
    org.springframework.security: INFO
```

### Environment Variables

For production deployment, override sensitive values using environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-host:5432/prod_db
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=secure_password
export JWT_SECRET=your-secure-secret-key
```

## Database

### Schema

The application uses three main tables:

1. **role** - User roles (ROLE_ADMIN, ROLE_USER)
2. **user** - System users with credentials
3. **member** - Member information

### Migrations

Database migrations are managed by Flyway:

- **Location:** `src/main/resources/db/migration`
- **V1__Create_schema.sql** - Creates tables, indexes, and triggers
- **V2__Seed_data.sql** - Seeds initial users and sample members

### Generate Password Hash

To generate BCrypt password hashes for seeding:

```bash
./gradlew generatePasswordHash
```

## Development

### Code Style

- **Constructor Injection:** Always prefer constructor injection over field injection
- **Immutable DTOs:** Use `@Data` and `@Builder` from Lombok for DTOs
- **Validation:** Use Jakarta Bean Validation annotations
- **Exception Handling:** Create custom exceptions with meaningful messages
- **Logging:** Use SLF4J with appropriate log levels

### Development Tools

- **Spring Boot DevTools:** Automatic restart on code changes
- **Lombok:** Reduces boilerplate code
- **H2 Database:** Available for testing (in-memory)

### Building for Production

```bash
# Clean build with tests
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Build JAR only
./gradlew bootJar
```

The executable JAR will be in `build/libs/surest-app-0.0.1-SNAPSHOT.jar`

## Architecture Principles

This application follows Domain-Driven Design (DDD) principles:

- **Layered Architecture:** Controller → Service → Repository
- **Domain Models:** Rich domain entities with business logic
- **DTOs:** Separate data transfer objects for API contracts
- **Repository Pattern:** Data access abstraction
- **Service Layer:** Business logic orchestration
- **Exception Handling:** Centralized error handling with meaningful responses

## License

Copyright © 2024 TietoEvry. All rights reserved.
