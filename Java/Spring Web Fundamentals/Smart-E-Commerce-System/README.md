# Smart E-Commerce System

A Spring Boot backend for an e-commerce platform, exposing both a **REST API** and a **GraphQL API** over a PostgreSQL database. It includes Bean Validation with custom validators, global exception handling, OpenAPI (Swagger) documentation, AOP-based logging and performance monitoring, and hand-written sorting, searching and pagination algorithms.

The data-access phase adds Spring Data JPA repositories with derived, JPQL and native SQL queries; transactional order placement with verified rollback; evidence-based database indexes; and Spring Cache for frequently read data and reports.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 (Web MVC, Data JPA, Validation, GraphQL, AspectJ, Cache) |
| Database | PostgreSQL (H2 in-memory for the `test` profile) |
| Caching | Spring Cache with the built-in in-memory cache (`ConcurrentMapCacheManager`) |
| API docs | springdoc-openapi 3 (Swagger UI) |
| Testing | JUnit 5, AssertJ, Spring Boot Test (integration tests on H2) |
| Build | Gradle (Kotlin DSL) |
| Other | Lombok, spring-security-crypto (BCrypt password hashing) |

> **Note on the Spring Boot version:** the brief specifies Spring Boot 3.x. This project uses Spring Boot 4.1.2, the current release. All required features (REST, GraphQL, validation, AOP, OpenAPI, Spring Data JPA, transactions, caching) work the same way; the differences are package names in a few dependencies (`spring-boot-starter-aspectj` instead of `spring-boot-starter-aop`, springdoc 3.x instead of 2.x).

## Prerequisites

- JDK 21
- PostgreSQL 14 or later, running locally, with `psql` on the command line
- No separate Gradle install needed: use the included `./gradlew` wrapper

## Setup

### 1. Database

This application builds on the `smart_ecommerce` PostgreSQL database designed and populated in the previous module (database design phase). It does not create tables itself: the database must already contain the `users`, `categories`, `products`, `inventory`, `orders`, `order_items` and `reviews` tables and the `order_totals` view.

If the database is empty of data, create records through the API (for example `POST /api/categories`, then `POST /api/products`).

If the tables were created by a different PostgreSQL user, grant access to the application user:

```sql
GRANT ALL ON ALL TABLES IN SCHEMA public TO admin;
```

### 2. Indexes

Add the indexes chosen during query optimization (see [Data access](#data-access-repositories-and-queries) and the performance report). The app runs without them, but order history and the admin order list are several times slower.

```bash
psql -U admin -d smart_ecommerce -f script/add-indexes.sql
```

### 3. Test data for performance work (optional)

To measure performance on a realistic data volume, load the generated dataset: about 2,000 users, 5,000 products with inventory, 100,000 orders, 300,000 order items and up to 30,000 reviews. It needs at least one existing category. Back up first:

```bash
pg_dump -U admin smart_ecommerce > backup.sql
psql -U admin -d smart_ecommerce -f script/seed-large-data.sql
```

### 4. Check the connection settings

The dev profile in `src/main/resources/application-dev.yml` expects:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smart_ecommerce
    username: admin
    password: ""
```

Change these if your database name or credentials differ.

### 5. Run the application

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`. On startup Hibernate validates every entity against the existing tables (`ddl-auto: validate`) and refuses to start if they don't match. Spring Data also checks every repository query at startup, so a mistyped field name in a query fails immediately with a clear message.

## Profiles

| Profile | Database | Schema handling | Notes |
| --- | --- | --- | --- |
| `dev` (default) | Local PostgreSQL | `validate` | SQL logging, DEBUG logs, GraphiQL enabled |
| `test` | H2 in-memory (PostgreSQL mode) | `create-drop` | Isolated from real data; used by the integration tests |
| `prod` | From environment variables | `validate` | Swagger UI and GraphiQL disabled, WARN logging |

Switch profiles with:

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
# or
SPRING_PROFILES_ACTIVE=test ./gradlew bootRun
```

The `prod` profile reads `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` from the environment. Caching is configured in the shared `application.yml`, so it is active in every profile.

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
├── config/        OpenAPI, password encoder and cache configuration (CacheConfig, CacheNames)
├── controller/    REST controllers
├── dto/           Request/response records and the ApiResponse envelope
│   ├── order/       Order requests, responses and the order-history summary
│   ├── review/      Review requests and responses
│   └── report/      Report rows (monthly revenue, top products, low stock)
├── entity/        JPA entities mapped to the existing schema
├── exception/     Custom exceptions and the global REST exception handler
├── graphql/       GraphQL controllers and GraphQL error handler
├── monitoring/    In-memory performance metrics store
├── repository/    Spring Data JPA repositories and Specifications
│   └── projection/  Read-only views of native-query rows
├── service/       Service interfaces and implementations (business logic, transactions, caching)
└── validation/    Custom validators (@UniqueEmail, @StrongPassword)

src/main/resources/
├── application.yml           Shared settings (including the cache)
├── application-{dev,test,prod}.yml
└── graphql/schema.graphqls   GraphQL schema

src/test/java/.../service/
└── OrderServiceIntegrationTest.java   Transaction and rollback tests

script/
├── add-indexes.sql          Indexes chosen from the query-plan analysis
├── seed-large-data.sql      Generated dataset for performance testing
├── baseline-explain.sql     EXPLAIN ANALYZE of the key queries
├── perf-measure.sh          Query plans + endpoint timings, saved under a label
├── perf-sort-page.sh        Page-depth and sort-column experiment
├── sort-pagination-explain.sql   EXPLAIN ANALYZE for that experiment
└── perf-test.sh             REST vs GraphQL comparison

docs/                        Performance reports
perf-results/                Output of perf-measure.sh (CSV, summaries, query plans)
```

The application follows a layered architecture: **Controller → Service → Repository**. REST and GraphQL controllers both call the same services, so validation, business rules, transactions and caching are written once. All dependencies use constructor injection.

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
| GET | `/api/users/{id}` | Get a user (cached) |
| PUT | `/api/users/{id}` | Update a user (password optional) |
| DELETE | `/api/users/{id}` | Delete a user (blocked if they have orders) |
| GET | `/api/users/{userId}/orders` | A customer's order history: one row per order with item count and total |
| POST | `/api/categories` | Create a category |
| GET | `/api/categories` | List categories (cached per page) |
| GET | `/api/categories/{id}` | Get a category (cached) |
| PUT | `/api/categories/{id}` | Update a category |
| DELETE | `/api/categories/{id}` | Delete a category (blocked if it has products) |
| POST | `/api/products` | Create a product and its inventory row |
| GET | `/api/products` | Search, filter, sort and paginate products |
| GET | `/api/products/{id}` | Get a product (cached) |
| GET | `/api/products/by-category?name=` | Products in a category, found by category name (derived query) |
| GET | `/api/products/by-price?min=&max=` | Products in a price range, both bounds inclusive (derived query) |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product (blocked if it appears in orders) |
| POST | `/api/products/{productId}/reviews` | Add a review (one per user per product) |
| GET | `/api/products/{productId}/reviews` | List a product's reviews |
| GET | `/api/products/{productId}/rating` | Average rating and review count |
| POST | `/api/orders` | Place an order: creates the order and removes stock in one transaction |
| GET | `/api/orders/{id}` | Get an order with its items and total |
| GET | `/api/orders?status=PAID` | List orders by status (admin) |
| POST | `/api/orders/{id}/pay` | Pay for a pending order (simulated payment) |
| POST | `/api/orders/{id}/cancel` | Cancel a pending or paid order and return its stock |
| GET | `/api/reports/monthly-revenue` | Revenue per month; optional `from`, `to` (`yyyy-MM-dd`, default last 12 months) (cached) |
| GET | `/api/reports/top-products` | Best-selling products; `limit` 1 to 100, default 10 (cached) |
| GET | `/api/reports/low-stock` | Products at or below their reorder level (cached) |
| GET | `/api/dsa/products` | Products sorted with merge sort and paginated in memory |
| GET | `/api/dsa/products/{id}` | Find a product with binary search |
| GET | `/api/metrics/performance` | AOP timing statistics per method |
| DELETE | `/api/metrics/performance` | Reset the timing statistics |

### Pagination, sorting and filtering

All list endpoints accept `page` (0-based), `size` (max 100) and `sort=field,asc|desc`. Order and review lists default to newest first. Products also accept optional filters, combined with AND:

```
GET /api/products?categoryId=1&minPrice=10&maxPrice=500&name=mouse&page=0&size=10&sort=price,desc
```

Filtering, sorting and paging all run in PostgreSQL through JPA Specifications and `Pageable`, so only the requested page leaves the database.

Two things affect speed (measured in the performance report). Sorting by an indexed column (`id`, or `orderDate` for orders) lets PostgreSQL read rows already in order; sorting by an unindexed one (`price`, `name`) makes it read and sort every row. Deep pages are slower than early ones, because `OFFSET` reads and discards every earlier row: the last page of PAID orders costs about 14 ms in the database against 0.13 ms for the first.

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

```http
POST /api/orders
Content-Type: application/json

{ "userId": 1, "items": [ { "productId": 1, "quantity": 2 }, { "productId": 2, "quantity": 1 } ] }
```

```http
POST /api/products/1/reviews
Content-Type: application/json

{ "userId": 1, "rating": 5, "comment": "Works perfectly" }
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
- Both call the same service layer, so validation (including the custom validators), business rules, transactions and caching are shared.
- Each has its own error handler: REST returns HTTP status codes; GraphQL returns HTTP 200 with an `errors` array containing a classification and a `code` extension.

## Data access: repositories and queries

Every table has a Spring Data JPA repository that extends `JpaRepository<Entity, IdType>`, which provides CRUD, paging and sorting without any code. `OrderItemRepository` uses the composite key class `OrderItemId`, and `InventoryRepository` uses `Integer` because inventory shares the product's primary key.

Queries come in three kinds, each used where it fits best.

### Derived queries

Spring builds the query from the method name. Used for simple lookups and existence checks.

| Repository | Methods |
| --- | --- |
| `UserRepository` | `existsByEmailIgnoreCase`, `existsByEmailIgnoreCaseAndIdNot`, `findByRole` |
| `CategoryRepository` | `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot` |
| `ProductRepository` | `findWithCategoryById`, `existsByCategoryId`, `findByCategoryNameIgnoreCase`, `findByPriceBetween` |
| `OrderRepository` | `findByStatus` (paged), `existsByUserId` |
| `OrderItemRepository` | `existsByProductId` |
| `ReviewRepository` | `findByProductId` (paged), `existsByProductIdAndUserId` |

`findByCategoryNameIgnoreCase` and `findByPriceBetween` back the `/api/products/by-category` and `/api/products/by-price` endpoints; `existsByProductId` gives a clear 409 before a product that appears in orders is deleted.

Methods that load related data use `@EntityGraph` to join it into the same SQL query (for example, `findByStatus` loads each order's user), which avoids N+1 queries. Product search also uses JPA Specifications (`ProductRepository` extends `JpaSpecificationExecutor`) to combine optional filters into one `WHERE` clause.

### JPQL queries (`@Query`)

Written against entity and field names, used when a method name can't express the logic.

| Method | Purpose |
| --- | --- |
| `OrderRepository.findOrderHistory` | Groups each order's items into an item count and total, built straight into `OrderSummaryResponse` with a constructor expression. Has an explicit `countQuery`, because Spring can't derive a correct count from a `GROUP BY` query. |
| `OrderRepository.findDetailById` | Loads an order, its user, items and each item's product in one query (`join fetch`) |
| `ReviewRepository.findRatingSummary` | Average rating and review count for a product |
| `InventoryRepository.decrementStock` | `@Modifying` update that removes stock only if enough is available (see [Transactions](#transactions)) |
| `InventoryRepository.findLowStock` | Compares two columns (`quantityOnHand <= reorderLevel`), which a derived query can't do |

### Native SQL queries

Used for reports that need PostgreSQL-specific functions. Each row maps to a projection interface in `repository/projection`; Spring matches getters to column aliases, which are quoted so PostgreSQL keeps their camelCase.

| Method | Purpose |
| --- | --- |
| `OrderRepository.findMonthlyRevenue` | Revenue and order count per month, using `date_trunc` and `to_char`; excludes cancelled orders |
| `OrderItemRepository.findTopSellingProducts` | Products ranked by units sold, with revenue |

### What queries return

- **Entities** when the data will be changed, or the whole object is needed (then mapped to DTOs in the service).
- **DTOs or projections** for read-only summaries and reports. They load only the needed columns, skip Hibernate's change tracking and can't trigger lazy loading.

### Indexes

PostgreSQL does not index foreign key columns automatically. Indexes were added only where `EXPLAIN ANALYZE` showed a full table scan on a large table (`script/add-indexes.sql`):

| Index | Serves | Effect |
| --- | --- | --- |
| `idx_orders_user_id` | Order history and its count query | Full scan of 100,000 orders replaced by an index lookup |
| `idx_orders_status_order_date` | Orders by status, newest first | Reads the newest 20 matching rows in order and stops, instead of sorting about 20,000 |
| `idx_orders_order_date` | Monthly revenue over a date range | Helps narrow date ranges; wide ranges still scan, which PostgreSQL correctly prefers |

Reviews by product already use the index created by the `(product_id, user_id)` unique constraint, and product lookups were already fast, so they got no extra index: every index also slows down inserts and updates.

## Transactions

All services are annotated `@Transactional(readOnly = true)` at class level, so reads never accidentally write and Hibernate can skip change tracking. Methods that change data override this with `@Transactional`.

### Placing an order

`OrderServiceImpl.placeOrder` runs as a single transaction: either the order, its items and every stock change are saved, or none of them are.

1. Load the user (404 if missing).
2. Merge duplicate lines for the same product (each product can appear only once per order).
3. For each product: load it (404 if missing), then remove stock through `InventoryService.reserveStock`.
4. Save the order; its items are saved with it through `cascade = ALL`.

| Setting | Value | Why |
| --- | --- | --- |
| Propagation | `REQUIRED` (default) on `placeOrder` | Starts a transaction, or joins one if the caller already has one |
| Propagation | `MANDATORY` on `reserveStock` | Refuses to run without an existing transaction, so stock can only change as part of an order and is rolled back with it |
| Isolation | `READ_COMMITTED` (stated explicitly) | Only committed data from other transactions is visible. Enough here because the stock check and change happen in one statement (below). |
| Rollback | Any `RuntimeException` | `InsufficientStockException` and `ResourceNotFoundException` are unchecked, so Spring rolls back automatically. Checked exceptions would not trigger a rollback by default. |

### Safe stock updates

`decrementStock` checks and changes stock in a single `UPDATE`:

```sql
update Inventory i set i.quantityOnHand = i.quantityOnHand - :qty, i.updatedAt = local datetime
where i.productId = :productId and i.quantityOnHand >= :qty
```

It returns the number of rows changed: 0 means not enough stock, and the service throws `InsufficientStockException` (409). Because the check and the change are one statement, two customers buying the last item at the same time cannot both succeed. Bulk updates skip `@UpdateTimestamp`, so `updatedAt` is set in the query.

### Paying and cancelling orders

Orders move through a small set of statuses. Each change is checked and applied in a single conditional `UPDATE` (`OrderRepository.updateStatusIfCurrentIn`), the same idea as the stock update:

```
PENDING ──pay──▶ PAID
   │               │
   └────cancel─────┴──▶ CANCELLED
```

| Action | Allowed from | What happens in the transaction | On failure |
| --- | --- | --- | --- |
| `payOrder` | `PENDING` | Status set to `PAID`, then the total is charged through `PaymentService` | Payment declined (402): the status change is rolled back and the order stays `PENDING` |
| `cancelOrder` | `PENDING`, `PAID` | Status set to `CANCELLED`, then each item's stock is returned through `InventoryService.releaseStock` (`MANDATORY`) | Any error rolls back both the status and the stock changes |

Any other move (paying a cancelled order, cancelling twice) changes 0 rows and returns 409. Because the check and the change are one statement, two simultaneous cancel requests can't both succeed: the second one's `UPDATE` waits for the first to commit, then finds the order already cancelled, so stock is never returned twice.

Payment is simulated by `SimulatedPaymentService`, which declines any amount above a configurable limit, giving a repeatable failure case without any card data:

```yaml
app:
  payment:
    max-amount: 10000
```

A real payment provider is an external call that a database transaction can't undo. A production system would use the provider's idempotency keys and confirm the payment before marking the order paid.

### Rollback in practice

If an order has two products and the second lacks stock, the first product's stock was already reduced inside the transaction. The exception rolls that back too, and no order row is created. `OrderServiceIntegrationTest` proves this against a real database (see [Testing](#testing)).

Spring applies transactions through proxies, the same way it applies AOP. A `@Transactional` method called from another method of the same class bypasses the proxy and gets no transaction of its own, which is why `reserveStock` lives in a separate `InventoryService`.

## Caching

Frequently read data and slow reports are cached with Spring Cache, using the simple in-memory cache built into Spring.

### Configuration

Dependency in `build.gradle.kts`:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-cache")
```

`config/CacheConfig.java` enables caching with `@EnableCaching`. The cache names are fixed in `application.yml`, so a misspelled name causes an error instead of silently creating a new cache:

```yaml
spring:
  cache:
    type: simple
    cache-names: products,categories,categoryLists,users,reports
```

The same names are constants in `config/CacheNames.java`, which the annotations use.

### What is cached, and when it is cleared

`@Cacheable` stores a method's result on the first call and returns the stored copy afterwards. `@CacheEvict` removes entries when the data changes, so the next read reloads fresh data. Eviction happens only if the changing method succeeds.

| Cache | Filled by | Key | Cleared by |
| --- | --- | --- | --- |
| `products` | `ProductService.getById` | product id | Product update or delete (that id); category update (all, because product responses include the category) |
| `categories` | `CategoryService.getById` | category id | Category update or delete (that id) |
| `categoryLists` | `CategoryService.getAll` | page, size and sort | Any category create, update or delete (all pages) |
| `users` | `UserService.getById` | user id | User update or delete (that id) |
| `reports` | All three reports | report name + parameters | Any order placed or cancelled; any product create, update or delete (all entries) |

All cached values are immutable DTO records, never entities, so a cached object can't trigger lazy loading or be modified by accident. User profiles are cached as `UserResponse`, which contains no password hash. GraphQL queries that call the same service methods share the cache.

### Deliberately not cached

- **Product search:** each combination of filters, page and sort would be a separate entry, so the same request rarely repeats, and every product change would have to clear them all.
- **Order history, order lists and reviews:** they change with every order or review, and the indexes already make them fast.
- **The admin user list and GraphQL batch loading (`getByIds`):** varying parameters give a poor hit rate.

### Limitations

The in-memory cache belongs to one running application: it is emptied on restart, and several instances would each have their own copy. Entries have no expiry time; they are removed only by eviction. A distributed cache such as Redis would be the next step for running multiple instances.

## Validation and error handling

Request DTOs are validated with Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@DecimalMin`, `@Digits`, `@Positive`, `@Min`/`@Max` and others) through `@Valid`, in both REST and GraphQL. Order lines are validated individually (`List<@Valid OrderItemRequest>`).

**Custom validators**

- `@UniqueEmail` checks the database for an existing user with the same email (case-insensitive). Spring creates the validator, so `UserRepository` is injected through its constructor.
- `@StrongPassword` requires at least 8 characters with an uppercase letter, a lowercase letter and a digit.
- Email format uses a stricter regex than the default `@Email` (shared through `EmailRules`), so addresses like `a@b` are rejected.

**REST error responses** (from `GlobalExceptionHandler`)

| Status | When |
| --- | --- |
| 400 Bad Request | Validation failures (with a field-by-field error map), malformed JSON, invalid parameters or sort fields, a report `from` date after `to` |
| 404 Not Found | Resource or endpoint does not exist |
| 402 Payment Required | The simulated payment was declined; the order stays `PENDING` |
| 409 Conflict | Duplicate name or email, a delete blocked by related records, not enough stock for an order, a second review of the same product by the same user, an order status change that isn't allowed (such as paying a cancelled order) |
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

Spring AOP works through proxies, so calls between methods of the same class are not intercepted. The same rule applies to `@Transactional` and the caching annotations.

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

These endpoints load all products into memory to demonstrate the algorithms. The main `/api/products` endpoint leaves the same operations to PostgreSQL (`ORDER BY`, index lookups, `LIMIT`/`OFFSET`), which scales better because only the requested page is read and transferred. The same idea drives the indexes above: a B-tree index lets PostgreSQL find rows in O(log n) instead of scanning all n rows, and returns them already sorted.

## Testing

### Automated: transaction tests

`OrderServiceIntegrationTest` starts the full application on the `test` profile (H2) and checks order placement, payment and cancellation against a real database (11 tests):

| Test | Proves |
| --- | --- |
| `placeOrder_success` | The order and items are saved, the total is correct and stock is reduced |
| `placeOrder_insufficientStock_rollsBackEverything` | When the second product lacks stock, the first product's stock change is rolled back and no order exists |
| `placeOrder_unknownUser_throwsNotFound` | A missing user stops the order before anything changes |
| `placeOrder_unknownProduct_rollsBack` | A missing product also rolls back earlier stock changes |
| `reserveStock_withoutTransaction_isRejected` | `MANDATORY` propagation refuses a call without a transaction |
| `payOrder_pending_becomesPaid` | A pending order can be paid |
| `payOrder_declined_rollsBackStatus` | A declined payment rolls the status back to `PENDING` |
| `payOrder_unknownOrder_throwsNotFound` | Paying a missing order returns 404 |
| `cancelOrder_restoresStock` | Cancelling returns every item's stock |
| `cancelOrder_paidOrder_restoresStock` | A paid order can be cancelled too |
| `cancelledOrder_rejectsFurtherChanges` | A cancelled order can't be paid or cancelled again, and stock is returned only once |

Run them with:

```bash
./gradlew test
```

The report is written to `build/reports/tests/test/index.html`.

The test class is deliberately **not** annotated with `@Transactional`. If it were, `placeOrder` would join the test's transaction, its rollback would be postponed until the test ended, and the stock checks would see the reduced value. Without it, each `placeOrder` call runs in its own real transaction, exactly as when called from the controller; the test creates its data in `@BeforeEach` and removes it in `@AfterEach`.

### Manual: API

- **REST:** Swagger UI ("Try it out") and Postman, covering successful calls and error cases (validation failures, missing records, duplicates, blocked deletes, insufficient stock, duplicate reviews).
- **GraphQL:** queries and mutations run in GraphiQL, including validation errors returned in the `errors` array.

### Manual: cache behavior

With SQL logging on (dev profile), each check shows whether a request reached the database:

1. Call `GET /api/products/{id}` twice. The first call logs a `select`; the second logs nothing, because it came from the cache.
2. Update that product with `PUT`, then `GET` it again. A `select` appears and the new values are returned, so the old entry was evicted.
3. Call `GET /api/reports/top-products` twice (no SQL on the second call), place an order, then call it again. The SQL returns, because the order cleared the reports cache.

### Performance

- `script/perf-measure.sh` measures query plans and endpoint timings under a label (see [Performance](#performance)).
- `script/perf-test.sh` compares REST and GraphQL response times and sizes.

## Performance

### Query optimization and caching

Measured with `script/perf-measure.sh` on the generated dataset (100,000 orders, 299,934 order items): 50 timed requests per endpoint after 5 warm-up calls, on localhost. Average response times:

| Endpoint | Before | After indexes | After caching |
| --- | --- | --- | --- |
| Monthly revenue (12 months) | 172.7 ms | 160.3 ms | 0.9 ms |
| Top-selling products | 64.6 ms | 64.4 ms | 1.8 ms |
| Orders by status | 18.6 ms | 4.2 ms | 5.8 ms |
| Order history | 14.8 ms | 2.5 ms | 3.5 ms |
| Product by id | 1.2 ms | 1.0 ms | 0.6 ms |

Indexes fixed queries that look for a few rows in a large table; caching fixed reports that must read every row, whose results only change when an order is placed. The full analysis, including query plans, is in `docs/PERFORMANCE_REPORT_DATA_ACCESS.md`.

To reproduce (app running with the dev profile, indexes dropped for the "before" run):

```bash
./script/perf-measure.sh before
psql -U admin -d smart_ecommerce -f script/add-indexes.sql
USER_ID=<id> PRODUCT_ID=<id> ./script/perf-measure.sh after-indexes
# add caching, restart the app, then:
USER_ID=<id> PRODUCT_ID=<id> ./script/perf-measure.sh after-caching
```

The script prints the test IDs it picks on the first run; passing them to later runs keeps the comparison fair. Results are saved in `perf-results/`: `endpoint-timings.csv` holds every run, with one row per endpoint per label.

### Sorting and pagination

| Query (database time) | First page | Last page |
| --- | --- | --- |
| PAID orders, newest first, with `OFFSET` | 0.13 ms | 14.2 ms |
| PAID orders, keyset pagination | | 0.21 ms |
| Products sorted by `id` (indexed) | 0.03 ms | 1.72 ms |
| Products sorted by `price` (not indexed) | 2.06 ms | 3.85 ms |

`OFFSET` cost grows with the page number, while keyset pagination ("rows after the last one seen") stays constant; sorting by an unindexed column requires reading every row. To reproduce, from the project root with the app running:

```bash
./script/perf-sort-page.sh
```

### REST vs GraphQL

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
- **No N+1 queries:** `@EntityGraph` joins related data into repository queries for REST, `join fetch` loads an order's items and products in one query, and `@BatchMapping` batches categories for GraphQL.
- **`open-in-view` disabled**, so every database access happens inside `@Transactional` service methods.
- **Service interfaces with implementations**, so controllers depend on abstractions and implementations can be swapped or mocked.
- **Read-only summaries return DTOs straight from the query** (JPQL constructor expressions and native-query projections), so reports never load full entities.
- **Stock and order status are changed with one conditional `UPDATE` each**, so overselling, double cancellation and invalid status changes are impossible without extra locking.
- **Indexes only where a measurement showed a problem**, because each index also slows down writes.
- **Cache DTOs, not entities, and evict on every change**, so cached data is safe to share and never outlives the data it came from.