# Performance Report: REST vs GraphQL

Smart E-Commerce System · Oct 30, 2026

## Summary

REST is faster for fixed, simple requests; GraphQL wins when the client needs less data or related data in one call.

- For identical data, GraphQL took 47% longer (4.71 ms vs 3.20 ms) because it parses and validates every query and resolves categories with a second SQL query.
- When the client only needed name and price, GraphQL sent 80% less data (525 vs 2,644 bytes) at the same speed.
- When the client needed products plus category details, GraphQL replaced two REST calls with one, and was 18% faster with 55% less data.

The project keeps both APIs on the same service layer, so clients can use whichever fits each screen.

## Test setup

Each scenario was sent 50 times with `curl` after 5 untimed warm-up calls, and the average total response time and response size were recorded.

- **Tool:** `perf-test.sh`, using `curl -w "%{time_total} %{size_download}"`.
- **Environment:** Spring Boot 4 app and PostgreSQL on the same MacBook Pro, called over `localhost`.
- **App settings:** application logging at INFO and `spring.jpa.show-sql=false`, so debug output did not slow requests.
- **Data:** the project's seed dataset; every list request asked for a page of 20 products.
- **Server-side timings:** the AOP `PerformanceMonitoringAspect` also recorded per-method timings, saved from `/api/metrics/performance`.

## Results

GraphQL cut response size in every scenario, but was only faster when it saved data or a round trip.

| Scenario | API | Requests | Avg time (ms) | Size (bytes) | GraphQL vs REST |
| --- | --- | --- | --- | --- | --- |
| 1. 20 products, all fields | REST | 1 | 3.20 | 2,644 |  |
| 1. 20 products, all fields | GraphQL | 1 | 4.71 | 2,545 | 47% slower, 4% smaller |
| 2. 20 products, name + price only | REST | 1 | 2.75 | 2,644 |  |
| 2. 20 products, name + price only | GraphQL | 1 | 2.65 | 525 | about equal, 80% smaller |
| 3. Products + category details | REST | 2 | 3.41 | 3,240 |  |
| 3. Products + category details | GraphQL | 1 | 2.79 | 1,461 | 18% faster, 55% smaller |
| 4. One product by id | REST | 1 | 1.39 | 263 |  |
| 4. One product by id | GraphQL | 1 | 2.15 | 226 | 55% slower, 14% smaller |

Scenario 3's REST row adds two calls: products (1.99 ms, 2,644 bytes) and categories (1.42 ms, 596 bytes).

## Analysis

### 1. Same data: GraphQL has fixed overhead

GraphQL was 1.5 ms slower for the same fields. Every request must parse and validate the query, then resolve each field through the GraphQL engine. Its `category` field is also loaded by `@BatchMapping` in a second SQL query, while REST gets the category in the same query through a JOIN (`@EntityGraph`). The response was 4% smaller only because the query did not ask for REST's envelope and page metadata.

### 2. Fewer fields: GraphQL removes over-fetching

REST always returns every product field, even when the screen shows only name and price. GraphQL returned just those two fields, cutting the response from 2,644 to 525 bytes. Response time was about equal: the database work is the same, but GraphQL skipped the category query and serialized less JSON, which offset its parsing cost.

### 3. Related data: GraphQL removes under-fetching

REST has no endpoint for products with full category details, so the client needs two calls and receives every category, even unused ones. GraphQL fetched both in one query: 0.62 ms faster in total and 1,779 bytes smaller. On a real network, the saved round trip matters far more than on `localhost`, where it costs almost nothing.

### 4. Single lookup: REST is simpler and faster

For one small object, GraphQL's fixed overhead dominated: 2.15 ms vs 1.39 ms. The 37 bytes saved are negligible. REST's plain `GET /api/products/{id}` URL can also be cached by browsers and proxies, which GraphQL's `POST /graphql` requests cannot.

## Server-side timings (AOP)

Inside the application, REST and GraphQL handlers took similar time (about 1.2 to 1.4 ms per product search); most of GraphQL's extra cost happens outside the timed methods.

| Method | Calls | Avg (ms) | Max (ms) |
| --- | --- | --- | --- |
| \[REST\] ProductController.search | 165 | 1.75 | 58.93 |
| \[GRAPHQL\] ProductGraphQlController.products | 165 | 1.23 | 3.21 |
| \[SERVICE\] ProductServiceImpl.search | 330 | 1.47 | 58.27 |
| \[REST\] ProductController.getById | 55 | 1.09 | 18.78 |
| \[GRAPHQL\] ProductGraphQlController.product | 55 | 0.66 | 1.05 |
| \[SERVICE\] ProductServiceImpl.getById | 110 | 0.87 | 18.54 |
| \[GRAPHQL\] ProductGraphQlController.category (batch) | 165 | 0.67 | 6.13 |
| \[SERVICE\] CategoryServiceImpl.getByIds | 165 | 0.65 | 5.65 |
| \[REST\] CategoryController.getAll | 55 | 0.75 | 2.38 |
| \[SERVICE\] CategoryServiceImpl.getAll | 55 | 0.73 | 2.37 |

Calls include the 5 warm-up requests per scenario. Service methods count twice because REST and GraphQL both call them.

- **Cold start inflates REST averages.** The first-ever product search took 58.9 ms and the first lookup by id 18.8 ms, while JIT compilation, the connection pool and Hibernate's query cache warmed up. Without that single call, REST search averages 1.40 ms against GraphQL's 1.22 ms.
- **The batched category query is real extra work.** It ran 165 times at 0.67 ms each, which is about 44% of GraphQL's 1.5 ms gap in scenario 1. REST avoids it by joining categories in the main query.
- **GraphQL skips unrequested work.** The batch ran for scenarios 1, 3 and 4 (55 calls each) but never for scenario 2, which did not ask for `category`.
- **The rest of the gap is the GraphQL engine.** Query parsing, validation and field-by-field resolution run before and after the controller methods, so the aspect does not see them. Client time minus handler time is about 0.3 to 0.6 ms for REST and about 0.8 ms for GraphQL in scenario 4.

## Retrieval optimizations

Both APIs share these design choices, which keep each request to one or two SQL queries regardless of page size.

| Technique | Where | Effect |
| --- | --- | --- |
| Filtering in the database | `ProductSpecifications` (JPA Specifications) | Only matching rows leave PostgreSQL; filters combine into one WHERE clause |
| Database pagination and sorting | `Pageable` on every list endpoint | `LIMIT`/`OFFSET` and `ORDER BY` run in SQL; page size capped at 100 |
| JOIN fetch of categories | `@EntityGraph` on `ProductRepository` | A page of products loads with its categories in 1 query instead of 1 + N |
| Batched GraphQL resolution | `@BatchMapping` for `Product.category` | All categories on a page load in 1 `WHERE id IN (...)` query, and only if requested |
| No lazy loading in views | `spring.jpa.open-in-view: false` | All queries run inside `@Transactional` service methods, so none are hidden in serialization |
| Monitoring | AOP `PerformanceMonitoringAspect` | Times every service, REST and GraphQL method; logs calls slower than 500 ms |

The DSA endpoints (`/api/dsa/products`) show the same operations implemented by hand: merge sort, binary search and offset pagination. They load every product into memory first, so they demonstrate the algorithms; the production endpoints leave this work to PostgreSQL, which uses indexes and returns only the requested page.

## Limitations

Differences under about 1 ms should be read as trends, not exact results.

- **Run-to-run variance:** the same REST request (`/api/products?size=20`) averaged 3.20, 2.75 and 1.99 ms across scenarios 1 to 3. The AOP timings confirm the JVM was still warming up during scenario 1 (its first call took 58.9 ms), which may overstate GraphQL's overhead there.
- **Localhost only:** no real network latency, so the cost of extra round trips and larger payloads is understated.
- **Small dataset:** the seed data is small, so serialization and transfer costs stay low.
- **One client at a time:** requests ran one after another; concurrent load was not tested.

A fairer repeat would run each scenario in random order, several times, with a larger dataset and a load-testing tool such as JMeter or k6.

## Conclusion

Neither API is faster overall; each wins in a different situation, which is why the system offers both.

| Use REST when | Use GraphQL when |
| --- | --- |
| The response shape is fixed, like an admin CRUD form | Different screens need different fields from the same data |
| A request returns one small object | A screen needs related data, like products with their categories |
| Responses should be cached by browsers or proxies | Clients are on slow or mobile networks, where size and round trips matter |
| Clients want simple tooling: a URL, Swagger, Postman | Frontend teams want to change what they fetch without backend changes |

In this project, admin management fits REST, while customer-facing product browsing benefits most from GraphQL. Because both call the same services, validation, business rules and performance optimizations apply to both without duplication.
