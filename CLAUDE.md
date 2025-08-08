# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Local Development
```bash
# Run application with Docker (recommended)
docker compose up --build

# Stop application
docker compose down

# Stop and remove volumes (clears database data)
docker compose down -v
```

### Gradle Commands
```bash
# Build the application
./gradlew build

# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Create JAR file
./gradlew bootJar

# Clean build
./gradlew clean
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.pirogramming.recruit.domain.admin.AdminServiceTest"
```

## Project Architecture

This is a Spring Boot 3.5.3 application using Java 21 with a DDD (Domain-Driven Design) architecture pattern.

### Package Structure
```
com.pirogramming.recruit
├── domain/                 # Domain-specific modules
│   └── [domain-name]/     # e.g., admin, member
│       ├── controller/    # REST API endpoints
│       ├── service/       # Business logic
│       ├── entity/        # JPA entities
│       ├── repository/    # Data access layer
│       └── dto/          # Request/Response DTOs
└── global/               # Cross-cutting concerns
    ├── config/          # Configuration classes
    ├── exception/       # Global exception handling
    └── jwt/            # JWT authentication
```

### Key Technologies
- **Framework**: Spring Boot 3.5.3 with Spring Security
- **Database**: PostgreSQL with Spring Data JPA
- **Authentication**: JWT with custom security configuration
- **Documentation**: Swagger/OpenAPI 3
- **Build Tool**: Gradle 8.5
- **Java Version**: 21
- **Container**: Docker with multi-stage build

### Database Configuration
- Uses PostgreSQL 15 in Docker
- Database name: `piro-recruit`
- JPA auditing enabled via `@EnableJpaAuditing`
- Hibernate SQL logging enabled in development

### Response Format
The application uses a standardized response format via `ApiRes<T>` class:
- Success: `{ "success": true, "data": {...}, "message": "...", "status": 200, "code": ..., "time": "..." }`
- Error: `{ "success": false, "data": null, "message": "...", "status": 4xx/5xx, "code": ..., "time": "..." }`

### Security Configuration
- JWT-based authentication (currently commented out in SecurityConfig)
- CORS enabled for all origins with credentials
- All endpoints currently permit all requests
- BCrypt password encoding

### Environment Profiles
- `dev`: Development environment
- `prod`: Production environment (default for Docker)
- Configuration files: `application.yml`, `application-dev.yml`, `application-prod.yml`

## Commit Conventions

Follow the established commit message format:
```
[type] module: message content

Types:
- feat: new feature
- fix: bug fix
- docs: documentation
- style: formatting (no code changes)
- refactor: code refactoring
- test: test code
- chore: build tasks, dependency management

Examples:
feat admin: implement admin login functionality
fix auth: resolve authentication error on login
```

## Branch Strategy
- Main branch: `main`
- Development branch: `develop`
- Feature branches: `[issue-number]-feature/description`
- Bug fix branches: `[issue-number]-fix/description`

## Code Style
- Uses NAVER IntelliJ Java Formatter
- Lombok annotations for reducing boilerplate code
- Constructor injection with `@RequiredArgsConstructor`

## Important Notes
- JWT secret is currently hardcoded in application.yml - should be externalized for production
- Security filters are commented out in SecurityConfig - implement when authentication is needed
- Application includes file processing capabilities (Apache POI, CSV)
- Health check endpoint available at `/actuator/health`
- Swagger UI available at `/swagger-ui.html`