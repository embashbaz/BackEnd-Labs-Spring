# Smart E-Commerce System

A Spring Boot backend for an e-commerce platform, exposing both a **REST API** and a **GraphQL API** over a PostgreSQL database. It includes Bean Validation with custom validators, global exception handling, OpenAPI (Swagger) documentation, AOP-based logging and performance monitoring, and hand-written sorting, searching and pagination algorithms.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 (Web MVC, Data JPA, Validation, GraphQL, AspectJ) |
| Database | PostgreSQL (H2 in-memory for the `test` profile) |
| API docs | springdoc-openapi 3 (Swagger UI) |
| Build | Gradle (Kotlin DSL) |
| Other | Lombok, spring-security-crypto (BCrypt password hashing) |

> **Note on the Spring Boot version:** the brief specifies Spring Boot 3.x. This project uses Spring Boot 4.1.2, the current release. All required features (REST, GraphQL, validation, AOP, OpenAPI) work the same way; the differences are package names in a few dependencies (`spring-boot-starter-aspectj` instead of `spring-boot-starter-aop`, springdoc 3.x instead of 2.x).

## Prerequisites

- JDK 21
- PostgreSQL 14 or later, running locally
- No separate Gradle install needed: use the included `./gradlew` wrapper

## Setup

### 1. Database

This application builds on the `smart_ecommerce` PostgreSQL database designed and populated in the previous module (database design phase). It does not create tables itself: the database must already contain the `users`, `categories`, `products`, `inventory`, `orders`, `order_items` and `reviews` tables and the `order_totals` view.

If the database is empty of data, create records through the API (for example `POST /api/categories`, then `POST /api/products`).

If the tables were created by a different PostgreSQL user, grant access to the application user:

```sql
GRANT ALL ON ALL TABLES IN SCHEMA public TO admin;
```

### 2. Check the connection settings

The dev profile in `src/main/resources/application-dev.yml` expects:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smart_ecommerce
    username: admin
    password: ""
```

Change these if your database name or credentials differ.

### 3. Run the application

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`. On startup Hibernate validates every entity against the existing tables (`ddl-auto: validate`) and refuses to start if they don't match.

## Profiles

| Profile | Database | Schema handling | Notes |
| --- | --- | --- | --- |
| `dev` (default) | Local PostgreSQL | `validate` | SQL logging, DEBUG logs, GraphiQL enabled |
| `test` | H2 in-memory (PostgreSQL mode) | `create-drop` | Isolated from real data |
| `prod` | From environment variables | `validate` | Swagger UI and GraphiQL disabled, WARN logging |

Switch profiles with:

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
# or
SPRING_PROFILES_ACTIVE=test ./gradlew bootRun
```

The `prod` profile reads `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` from the environment.

## Useful URLs

| URL | What it is |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Interactive REST API documentation |
| http://localhost:8080/v3/api-docs | Raw OpenAPI specification (JSON) |
| http://localhost:8080/graphiql | GraphQL playground (dev profile only) |
| http://localhost:8080/graphql | GraphQL endpoint (POST) |
| http://localhost:8080/graphql/schema | Full GraphQL schema |
| http://localhost:8080/api/metrics/performance | Method timings collected by AOP |

## Project structure

```
src/main/java/com/example/Smart_E_Commerce_System/
├── algorithm/     Merge sort, binary search, in-memory pagination
├── aspect/        AOP pointcuts, logging and performance aspects
├── config/        OpenAPI and password encoder configuration
├── controller/    REST controllers
├── dto/           Request/response records and the ApiResponse envelope
├── entity/        JPA entities mapped to the existing schema
├── exception/     Custom exceptions and the global REST exception handler
├── graphql/       GraphQL controllers and GraphQL error handler
├── monitoring/    In-memory performance metrics store
├── repository/    Spring Data JPA repositories and Specifications
├── service/       Service interfaces and implementations (business logic)
└── validation/    Custom validators (@UniqueEmail, @StrongPassword)

src/main/resources/
├── application.yml           Shared settings
├── application-{dev,test,prod}.yml
└── graphql/schema.graphqls   GraphQL schema
```

The application follows a layered architecture: **Controller → Service → Repository**. REST and GraphQL controllers both call the same services, so validation, business rules and transactions are written once. All dependencies use constructor injection.

## REST API

Every response uses the same envelope:

```json
{
  "status": "success",
  "message": "Product retrieved",
  "data": { }
}
```

List endpoints return a page inside `data`:

```json
{ "content": [ ], "page": 0, "size": 10, "totalElements": 47, "totalPages": 5, "last": false }
```

### Endpoints

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/users` | Register a user (password stored as a BCrypt hash) |
| GET | `/api/users` | List users; optional `role` filter |
| GET | `/api/users/{id}` | Get a user |
| PUT | `/api/users/{id}` | Update a user (password optional) |
| DELETE | `/api/users/{id}` | Delete a user (blocked if they have orders) |
| POST | `/api/categories` | Create a category |
| GET | `/api/categories` | List categories |
| GET | `/api/categories/{id}` | Get a category |
| PUT | `/api/categories/{id}` | Update a category |
| DELETE | `/api/categories/{id}` | Delete a category (blocked if it has products) |
| POST | `/api/products` | Create a product and its inventory row |
| GET | `/api/products` | Search, filter, sort and paginate products |
| GET | `/api/products/{id}` | Get a product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product (blocked if it appears in orders) |
| GET | `/api/dsa/products` | Products sorted with merge sort and paginated in memory |
| GET | `/api/dsa/products/{id}` | Find a product with binary search |
| GET | `/api/metrics/performance` | AOP timing statistics per method |
| DELETE | `/api/metrics/performance` | Reset the timing statistics |

### Pagination, sorting and filtering

All list endpoints accept `page` (0-based), `size` (max 100) and `sort=field,asc|desc`. Products also accept optional filters, combined with AND:

```
GET /api/products?categoryId=1&minPrice=10&maxPrice=500&name=mouse&page=0&size=10&sort=price,desc
```

Filtering, sorting and paging all run in PostgreSQL through JPA Specifications and `Pageable`, so only the requested page leaves the database.

### Example requests

```http
POST /api/categories
Content-Type: application/json

{ "name": "Electronics", "description": "Gadgets and devices" }
```

```http
POST /api/products
Content-Type: application/json

{ "categoryId": 1, "name": "Wireless Mouse", "price": 25.99, "initialStock": 50 }
```

```http
POST /api/users
Content-Type: application/json

{ "fullName": "Jane Doe", "email": "jane@example.com", "password": "Secret123", "role": "CUSTOMER" }
```

## GraphQL API

GraphQL is served at `/graphql` and explored in GraphiQL at `/graphiql`. The schema lives in `src/main/resources/graphql/schema.graphqls`.

| Queries | Mutations |
| --- | --- |
| `category(id)`, `categories(page, size, sortBy, direction)` | `createCategory`, `updateCategory`, `deleteCategory` |
| `product(id)`, `products(filter, page, size, sortBy, direction)` | `createProduct`, `updateProduct`, `deleteProduct` |
| `user(id)`, `users(role, page, size, sortBy, direction)` | `createUser`, `updateUser`, `deleteUser` |

### Example query

The client chooses exactly which fields come back:

```graphql
query {
  products(filter: { minPrice: 10, name: "mouse" }, size: 5, sortBy: "price", direction: DESC) {
    totalElements
    content {
      name
      price
      category { name }
    }
  }
}
```

`Product.category` is resolved with `@BatchMapping`, so all categories for a page load in one query, and only when the client asks for them.

### Example mutation

```graphql
mutation {
  createCategory(input: { name: "Books", description: "Printed and digital books" }) {
    id
    name
  }
}
```

### How REST and GraphQL coexist

- They use separate paths (`/api/**` and `/graphql`), so their routes never conflict.
- Both call the same service layer, so validation (including the custom validators), business rules and transactions are shared.
- Each has its own error handler: REST returns HTTP status codes; GraphQL returns HTTP 200 with an `errors` array containing a classification and a `code` extension.

## Validation and error handling

Request DTOs are validated with Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@DecimalMin`, `@Digits` and others) through `@Valid`, in both REST and GraphQL.

**Custom validators**

- `@UniqueEmail` checks the database for an existing user with the same email (case-insensitive). Spring creates the validator, so `UserRepository` is injected through its constructor.
- `@StrongPassword` requires at least 8 characters with an uppercase letter, a lowercase letter and a digit.
- Email format uses a stricter regex than the default `@Email` (shared through `EmailRules`), so addresses like `a@b` are rejected.

**REST error responses** (from `GlobalExceptionHandler`)

| Status | When |
| --- | --- |
| 400 Bad Request | Validation failures (with a field-by-field error map), malformed JSON, invalid parameters or sort fields |
| 404 Not Found | Resource or endpoint does not exist |
| 409 Conflict | Duplicate name or email, or a delete blocked by related records |
| 500 Internal Server Error | Unexpected errors (logged with a stack trace, generic message returned) |

Example validation error:

```json
{
  "status": "error",
  "message": "Validation failed",
  "data": { "email": "Email must be a valid address (e.g. name@example.com)", "password": "Password must be at least 8 characters ..." }
}
```

## AOP: logging and monitoring

Cross-cutting concerns live in the `aspect` package, so services contain no logging or timing code.

| Class | Advice | What it does |
| --- | --- | --- |
| `AppPointcuts` | — | Named pointcuts for the service layer, REST controllers and GraphQL controllers |
| `LoggingAspect` | `@Before` | Logs method entry with arguments; passwords are masked as `****` |
| | `@AfterReturning` | Logs a short result summary, e.g. `PageResponse(10 of 47 items)` |
| | `@AfterThrowing` | Logs the exception type and message |
| | `@After` | Logs method exit, whether it succeeded or failed |
| `PerformanceMonitoringAspect` | `@Around` | Times every service, REST and GraphQL method, stores the stats and warns about slow calls |

Timings are available at `GET /api/metrics/performance` (calls, failures, and average, min and max milliseconds per method). The slow-call threshold is configurable:

```yaml
app:
  monitoring:
    slow-threshold-ms: 500
```

Spring AOP works through proxies, so calls between methods of the same class are not intercepted.

## Algorithms (DSA)

The `algorithm` package contains hand-written implementations, exposed through `/api/dsa/products`:

| Algorithm | Class | Complexity | Used for |
| --- | --- | --- | --- |
| Merge sort | `Sorting.mergeSort` | O(n log n) time, O(n) space, stable | Sorting products by id, name or price |
| Binary search | `Searching.binarySearch` | O(log n) | Finding a product by id in a sorted list |
| Offset pagination | `Paging.paginate` | O(page size) | Slicing the sorted list into pages |

```
GET /api/dsa/products?sortBy=price&descending=true&page=0&size=5
GET /api/dsa/products/5
```

These endpoints load all products into memory to demonstrate the algorithms. The main `/api/products` endpoint leaves the same operations to PostgreSQL (`ORDER BY`, index lookups, `LIMIT`/`OFFSET`), which scales better because only the requested page is read and transferred.

## Testing

The API was tested manually; the project has no automated test suite.

- **REST:** Swagger UI ("Try it out") and Postman, covering successful calls and error cases (validation failures, missing records, duplicates, blocked deletes).
- **GraphQL:** queries and mutations run in GraphiQL, including validation errors returned in the `errors` array.
- **Performance:** `script/perf-test.sh` compares REST and GraphQL response times and sizes (see below).

## Performance

Measured with `script/perf-test.sh` (50 timed requests per scenario after 5 warm-up calls, on localhost):

| Scenario | REST | GraphQL |
| --- | --- | --- |
| 20 products, all fields | 3.20 ms · 2,644 B | 4.71 ms · 2,545 B |
| 20 products, name + price only | 2.75 ms · 2,644 B | 2.65 ms · 525 B |
| Products + category details | 3.41 ms · 3,240 B (2 calls) | 2.79 ms · 1,461 B (1 call) |
| One product by id | 1.39 ms · 263 B | 2.15 ms · 226 B |

REST is faster for simple, fixed-shape requests; GraphQL sends far less data when the client needs only some fields, and replaces several REST calls when it needs related data. The full analysis, including server-side AOP timings, is in the performance report (`docs/PERFORMANCE_REPORT.md`).

To reproduce:

```bash
./gradlew bootRun --args='--logging.level.com.example=INFO --spring.jpa.show-sql=false'
# in a second terminal
./script/perf-test.sh 50
```

## Key design decisions

- **DTOs as Java records**, separate from entities, so the API shape is independent of the database and password hashes are never exposed.
- **Entities match the existing 3NF schema** exactly, including the shared-key `Inventory` table and the composite key on `order_items`; `ddl-auto: validate` guarantees they stay in sync.
- **No N+1 queries:** `@EntityGraph` joins categories into product queries for REST, and `@BatchMapping` batches them for GraphQL.
- **`open-in-view` disabled**, so every database access happens inside `@Transactional` service methods.
- **Service interfaces with implementations**, so controllers depend on abstractions and implementations can be swapped or mocked.