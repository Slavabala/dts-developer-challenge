# Work Done

## What was built

A full-stack task management system for HMCTS caseworkers, built from scratch against the company tech stack.

### Backend — Java / Spring Boot 3.2 (`backend/`)

- REST API with five endpoints: create, get by ID, get all, update status, delete
- PostgreSQL persistence via Spring Data JPA with Flyway database migrations
- Redis caching on all read paths (10-minute TTL, evicted on any write)
- Bean validation on all request bodies (`@Valid`) with RFC 9457 `ProblemDetail` error responses
- OpenAPI/Swagger UI auto-generated at `/swagger-ui.html`
- Prometheus metrics exposed at `/actuator/prometheus` for Dynatrace scraping
- Unit tests: `TaskServiceTest` (Mockito) and `TaskControllerTest` (MockMvc + `@WebMvcTest`)
- Multi-stage Dockerfile: JDK build stage → minimal JRE runtime, non-root `hmcts` user

### Frontend — Node.js / Express (`frontend/`)

- GOV.UK Frontend styled with Nunjucks templates
- Task list, create form (server-side validation), detail/status-update, and delete flows
- Axios HTTP client calling the backend API
- `express-validator` for input validation before proxying to the backend
- Unit tests: Jest + Supertest covering all route handlers with mocked `taskService`
- Multi-stage Dockerfile: `npm ci --omit=dev` build stage → minimal runtime, non-root `hmcts` user

### Infrastructure

| File | Purpose |
|---|---|
| `docker-compose.yml` | Local stack: Postgres + Redis + backend + frontend with healthchecks and dependency ordering |
| `Jenkinsfile` | Full CI/CD pipeline: test → security scan → Docker build → push to ACR → Terraform plan/apply → AKS deploy → Dynatrace deployment event |
| `infrastructure/main.tf` | Terraform modules for Azure: AKS cluster, Postgres Flexible Server, Azure Cache for Redis |
| `infrastructure/variables.tf` | Parameterised for `dev`, `staging`, `prod` environments with validation |
| `kubernetes/` | K8s Deployments and Services for backend, frontend, and Redis with resource limits, liveness/readiness probes, and non-root security context |

---

## Fixes applied during development

### Fix 1 — Missing Gradle wrapper (`gradlew` / `gradle/` not found in Docker build)

**Error:**
```
failed to compute cache key: "/gradle": not found
```

**Cause:** The `backend/Dockerfile` copies `gradlew` and `gradle/` but those files had not been committed — the Gradle wrapper was never generated.

**Fix:**
1. Updated `build.gradle` to replace the Gradle 9-incompatible top-level `sourceCompatibility = '17'` with:
   ```groovy
   java {
       sourceCompatibility = JavaVersion.VERSION_17
       targetCompatibility = JavaVersion.VERSION_17
   }
   ```
2. Ran `gradle wrapper --gradle-version=8.8` to generate `gradlew` and `gradle/wrapper/`.

---

### Fix 2 — `flyway-database-postgresql` dependency missing version

**Error:**
```
Could not find org.flywaydb:flyway-database-postgresql:
```

**Cause:** Flyway 10 split the Postgres driver into a separate `flyway-database-postgresql` artifact. Spring Boot's BOM manages `flyway-core` but does not include this new artifact, so Gradle had no version to resolve.

**Fix:** Added explicit versions to both Flyway dependencies in `build.gradle`:
```groovy
implementation 'org.flywaydb:flyway-core:10.10.0'
implementation 'org.flywaydb:flyway-database-postgresql:10.10.0'
```

---

### Fix 3 — Gradle daemon crashes on Java 25 (`Unsupported class file major version 69`)

**Error:**
```
BUG! exception in phase 'semantic analysis' Unsupported class file major version 69
```

**Cause:** The system default JVM is Java 25 (class file version 69). Gradle 8.8 only supports up to Java 22, so the Gradle daemon failed before it could parse the build script.

**Fix:** Created `backend/gradle.properties` to point the Gradle daemon at the Java 21 installation that is also present on the system:
```properties
org.gradle.java.home=/usr/lib/jvm/java-21-openjdk
```
The application still compiles to Java 17 bytecode (`targetCompatibility = VERSION_17`). The Docker build is unaffected because the JDK 17 image provides its own JVM.

---

### Fix 4 — Frontend home page returning 500 (`Something went wrong`)

**Error:**
```
AxiosError: Request failed with status code 500
```
The backend returned HTTP 500 on every `GET /api/tasks` request.

**Root cause:** Three related issues:

**4a. `LocalDateTime` serialization failure in Redis**

`GenericJackson2JsonRedisSerializer()` (no-arg constructor) creates its own `ObjectMapper` internally without `JavaTimeModule`. When Spring tried to write `TaskResponse` objects (which contain `LocalDateTime` fields) into Redis after a cache miss, Jackson threw an unrecognised type exception. The `GlobalExceptionHandler` caught this and returned a generic 500 with no log output, making it invisible.

Fixed by constructing the serializer with a properly configured `ObjectMapper` in `RedisConfig.java`:
```java
ObjectMapper mapper = JsonMapper.builder()
    .addModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(Object.class).build(),
        ObjectMapper.DefaultTyping.NON_FINAL
    )
    .build();
new GenericJackson2JsonRedisSerializer(mapper);
```

**4b. `TaskResponse` missing no-arg constructor**

`@Builder` (Lombok) generates only a package-private all-args constructor. Jackson requires a public no-arg constructor to deserialize objects back from Redis on a cache hit. Added `@NoArgsConstructor` and `@AllArgsConstructor` to `TaskResponse`.

**4c. Stale Redis entries from the broken serializer**

The old backend had already written malformed entries into Redis before the fix was applied. After rebuilding the backend image, Redis was flushed (`FLUSHALL`) so the new serializer started with a clean cache.

**4d. Silent error handler**

The `GlobalExceptionHandler` swallowed all unhandled exceptions without logging, making diagnosis impossible. Added `log.error("Unhandled exception", ex)` to the catch-all handler so future errors appear in the container logs.
