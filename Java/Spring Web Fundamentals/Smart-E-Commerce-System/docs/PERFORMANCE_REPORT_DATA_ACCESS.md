# Performance Report: Query Optimization and Caching

Smart E-Commerce System · Nov 3, 2026 (sorting and pagination added Nov 6, 2026)

## Summary

Indexes fixed queries that look for a few rows in a large table; caching fixed reports that must read every row. Neither technique alone would have fixed all the slow endpoints.

- **Indexes** made order history 83% faster (14.8 to 2.5 ms) and the admin order list 77% faster (18.6 to 4.2 ms). Inside PostgreSQL, the order-list query went from 9.41 to 0.10 ms, about 90 times faster.
- **Caching** made the monthly revenue report 99.4% faster (160.3 to 0.9 ms) and the top-selling products report 97% faster (64.4 to 1.8 ms).
- **Indexes barely helped the reports** (7% and 0%), because a report that adds up every order line must read every row whatever indexes exist.
- **Deep pages cost more than early ones.** The last page of PAID orders took 14.2 ms in PostgreSQL against 0.13 ms for the first, because `OFFSET` reads and throws away every earlier row. Keyset pagination fetched the same last page in 0.21 ms.
- **Sorting by an unindexed column costs a full scan.** Products sorted by `id` (indexed) took 0.03 ms; sorted by `price`, 2.1 ms, because PostgreSQL had to read and sort all 5,013 products.
- **Already-fast endpoints gained little from caching** (for example, product by id went from 1.0 to 0.6 ms), because about 0.6 ms of every request is fixed application overhead.

## Test setup

Each endpoint was called 50 times with `curl` after 5 untimed warm-up calls, and the query plans of the underlying SQL were recorded with `EXPLAIN ANALYZE`, in three runs:

| Run | State of the application |
| --- | --- |
| `before` | Repositories and endpoints complete; no extra indexes; no caching |
| `after-indexes` | The three indexes from `script/add-indexes.sql` added; app left running |
| `after-caching` | Spring Cache enabled; app restarted to load the new code |
| `sort-page` | Separate experiment on page depth and sort columns (Nov 6), with indexes and caching in place; the endpoints it calls are not cached |

- **Tool:** `script/perf-measure.sh`, which runs both parts and saves everything under the run's label in `perf-results/`.
- **Endpoint timing:** `curl -w "%{time_total}"`; average, minimum, 95th percentile and maximum per endpoint.
- **Query plans:** `script/baseline-explain.sql` runs each key query with `EXPLAIN (ANALYZE, BUFFERS)`. It was run twice per label and the second, warm run was kept, so all numbers come from data already in memory.
- **Environment:** Spring Boot 4 app (dev profile) and PostgreSQL on the same MacBook Pro, called over `localhost`, with identical settings in all three runs.
- **Data:** the generated dataset from `script/seed-large-data.sql`: 2,003 users, 5,012 products, about 100,000 orders, 299,934 order items and their reviews. `ANALYZE` was run after loading it, so the planner had current statistics.
- **Test values:** the same in every run: user 1362 (74 orders) and product 3153 (18 reviews), the user and product with the most data.

## Results

### Endpoints

Average response time over 50 requests, in milliseconds:

| Endpoint | Before | After indexes | After caching | Overall | Main cause |
| --- | --- | --- | --- | --- | --- |
| Monthly revenue (12 months) | 172.7 | 160.3 | 0.9 | 99.5% faster | Caching |
| Top-selling products | 64.6 | 64.4 | 1.8 | 97% faster | Caching |
| Orders by status | 18.6 | 4.2 | 5.8 | 69% faster | Index |
| Order history | 14.8 | 2.5 | 3.5 | 76% faster | Index |
| Low stock | 2.5 | 2.1 | 0.9 | 64% faster | Caching |
| Product by id | 1.2 | 1.0 | 0.6 | 50% faster | Caching |
| Categories (first page) | 1.2 | 0.9 | 0.7 | 42% faster | Caching |
| User by id | 0.8 | 0.7 | 0.6 | 25% faster | Caching |
| Product reviews | 2.5 | 1.6 | 2.7 | No change | Not targeted |
| Product rating | 1.4 | 1.0 | 1.8 | No change | Not targeted |
| Product search | 2.5 | 2.5 | 3.2 | No change | Not targeted |

The slowest requests improved even more than the averages: the 95th percentile of monthly revenue fell from 197.1 ms to 1.3 ms, and of orders by status from 24.8 ms to 4.8 ms after indexing.

### Database queries

Execution time inside PostgreSQL (warm run), in milliseconds. Caching doesn't appear here: these queries run directly in PostgreSQL, so the application's cache never sees them.

| Query | Before | After indexes | Change | Plan before → after |
| --- | --- | --- | --- | --- |
| Order history (data) | 3.10 | 0.64 | 4.9× faster | Seq Scan on orders → Bitmap Index Scan on `idx_orders_user_id` |
| Order history (count) | 2.61 | 0.01 | ~240× faster | Seq Scan → Index Only Scan |
| Orders by status (data) | 9.41 | 0.10 | ~90× faster | Seq Scan + sort of 20,036 rows → Index Scan Backward, stops after 20 rows |
| Orders by status (count) | 4.51 | 1.57 | 2.9× faster | Seq Scan → Index Only Scan, still counting 20,036 entries |
| Monthly revenue (3 months) | 63.6 | 56.1 | 12% faster | Orders found by `idx_orders_order_date`; all 299,934 order items still scanned |
| Top-selling products | 67.5 | 71.5 | No change | Seq Scan of every order item, before and after |
| Reviews of a product | 0.14 | 0.17 | No change | Already used the unique-constraint index |
| Products by category | 0.03 | 0.03 | No change | Primary-key scan, stops once 20 matches are found |
| Products by price range | 0.05 | 0.04 | No change | Primary-key scan, stops once 20 matches are found |

## Analysis

### 1. Order history: a missing foreign key index

To find user 1362's 74 orders, PostgreSQL read all 100,000 orders, because it does not index foreign key columns automatically. The index on `orders(user_id)` replaced that scan with a direct lookup: the data query dropped from 3.10 to 0.64 ms. The count query, which `Page` needs for its totals, dropped from 2.61 to 0.01 ms. That is an Index Only Scan: PostgreSQL answered from the index alone, without reading the table.

At the endpoint, time fell from 14.8 to 2.5 ms. That is more than the database saved (about 5 ms), so part of the gain is probably the JVM being better warmed up in the second run (see [Limitations](#limitations)).

### 2. Orders by status: a composite index that matches the sort

The admin list asks for the newest 20 orders with a given status. Before indexing, PostgreSQL read every order, kept the 20,036 PAID ones, sorted them all by date and returned 20. The composite index on `(status, order_date)` stores matching rows already in date order, so PostgreSQL reads the index backwards (newest first) and stops after 20 rows: 9.41 to 0.10 ms.

The count query improved less (4.51 to 1.57 ms) because a count must visit every matching entry: 20,036 of them, now from the smaller index instead of the table. Counting cost grows with the number of matches. Screens that only need "is there a next page?" could return a Spring Data `Slice` instead of a `Page`, which skips the count query entirely.

### 3. Reports: why indexes barely helped

An index helps find **a few** rows among many. Reports that summarize a large share of the data have to read it anyway.

- **Top-selling products** adds up every non-cancelled order line, so PostgreSQL must read all 299,934 of them. Its time was the same before and after (about 65 to 70 ms), as expected.
- **Monthly revenue** improved slightly. For a 3-month range, the date index found the matching orders quickly (12% faster), but all order lines were still read to join them. For the endpoint's default 12-month range, about half of all orders, a full scan is cheaper than the index, and PostgreSQL correctly chose it: only 7% faster.

Adding more indexes would not change this. The work itself is large.

### 4. Caching: reports become nearly free

A report's result only changes when an order is placed or a product changes, so the same answer can be reused until then. With caching, monthly revenue fell from 160.3 to 0.9 ms and top products from 64.4 to 1.8 ms. These endpoints went from the slowest in the system to among the fastest.

The cache is cleared whenever an order is placed or a product is created, updated or deleted. The first request after that pays the full cost once and refills the cache.

### 5. Caching already-fast endpoints: the request floor

Product by id, categories and user by id were already fast (0.8 to 1.2 ms), and caching brought them to 0.6 to 0.7 ms. Even a cached request, which never touches the database, costs about 0.6 ms: HTTP handling, Spring MVC, the AOP logging and monitoring aspects, and JSON conversion. When most of a request's time is this floor, caching can only remove the small remainder.

Caching these entities still matters as traffic grows: each hit is a database query that never happens, which reduces load on PostgreSQL, even if a single request barely gets faster.

### 6. Uncached endpoints in the third run

Order history, orders by status, reviews, rating and product search are not cached, yet were 0.7 to 1.6 ms slower in the `after-caching` run than in the `after-indexes` run. Nothing changed for these endpoints in between, so this is run-to-run variation, not a regression. The third run started right after an application restart, while the JVM's just-in-time compiler was still optimizing the code. Differences of about 1 ms between runs should therefore be read as noise.

## Sorting and pagination

This experiment answers two questions with `script/perf-sort-page.sh` and `script/sort-pagination-explain.sql`, using the same method as above. There were 20,036 PAID orders (1,002 pages of 20) and 5,013 products (251 pages of 20).

### Page depth: OFFSET gets slower the further you go

Spring's `Pageable` turns `page=500&size=20` into `OFFSET 10000 ROWS FETCH FIRST 20 ROWS ONLY`. An index can find rows by value, but not by position, so PostgreSQL has to read the first 10,000 matching rows and throw them away before it reaches the 20 it returns.

| Page of PAID orders | Offset | Rows read | Database | Endpoint avg |
| --- | --- | --- | --- | --- |
| First (page 0) | 0 | 20 | 0.13 ms | 5.5 ms |
| Middle (page 500) | 10,000 | 10,020 | 8.2 ms | 13.3 ms |
| Last (page 1001) | 20,020 | 20,036 (all) | 14.2 ms | 11.5 ms |
| Last, with keyset pagination | none | 16 | 0.21 ms | not exposed in the API |

- **The cost grows with the page number.** On the middle page, the index scan read 10,020 rows to return 20. For the last page PostgreSQL switched plans: reading the index in order would have meant walking all 20,036 entries anyway, so it loaded every PAID order, sorted them in memory (quicksort, 3.3 MB) and kept the last 16.
- **At the endpoint the gap is smaller** (5.5 ms against 11 to 13 ms) because every request also pays fixed costs: the count query for `totalElements` (about 1.3 ms), Hibernate, JSON and HTTP. The middle page measuring slower than the last is within noise; the last page also returns only 16 rows.
- **Keyset pagination removes the cost.** Instead of "skip 20,020 rows", the client asks for "the 20 orders older than the last date I saw" (`WHERE order_date < :lastSeen`). The index jumps straight to that date, so the last page took 0.21 ms, as fast as the first.

Keyset pagination was measured but not added to the API. It can only move to the next or previous page, not jump to page 500, and it needs a unique tie-breaker (such as the order id) when two rows share a date. That makes it the better fit for infinite scrolling or data exports than for numbered admin pages. With page size capped at 100 and the worst page at about 14 ms, `OFFSET` is acceptable at the current data volume.

### Sort column: indexed vs. unindexed

| Products sorted by | Page | Rows read | How PostgreSQL sorted | Database | Endpoint avg |
| --- | --- | --- | --- | --- | --- |
| `id` (primary key index) | First | 20 | Already in order from the index | 0.03 ms | 3.1 ms |
| `price` (no index) | First | 5,013 | Top-N heapsort, 33 kB | 2.06 ms | 5.8 ms |
| `name` (no index) | First | 5,013 | Top-N heapsort, 32 kB | 1.73 ms | 5.1 ms |
| `id` | Last | 5,013 | Already in order from the index | 1.72 ms | 4.3 ms |
| `price` | Last | 5,013 | Full quicksort, 1.5 MB | 3.85 ms | 9.1 ms |

- **An indexed sort column costs almost nothing** on early pages. The primary-key index stores products in `id` order, so PostgreSQL reads 20 entries and stops.
- **An unindexed sort column means reading every row.** For the first page, PostgreSQL uses a *top-N heapsort*: it scans all 5,013 products but only keeps the best 20 in a small heap, which takes O(n log k) time for k = 20 instead of sorting everything.
- **Deep pages combine both costs.** The last page by `id` still reads 5,013 index entries because of `OFFSET`. The last page by `price` needs almost every row in order, so top-N no longer helps and PostgreSQL falls back to a full quicksort, which makes it the slowest product query.

No index was added on `price`. With 5,013 products the extra cost is about 2 to 4 ms, but it grows in step with the catalog. An index on `products(price)` would remove the sort for price-ordered pages and also serve the `/api/products/by-price` range query, so it is the first index to add if the catalog grows or measurements show price sorting slowing down.

### Connection to the hand-written algorithms

The DSA endpoints from the previous phase do the same jobs in memory, and the comparison shows why the database behaves as it does:

| In memory (`algorithm` package) | In PostgreSQL | Why they differ |
| --- | --- | --- |
| `Paging.paginate`: O(page size) | `OFFSET`: O(offset + page size) | An array can jump to any position; an index can only find rows by value, so it counts from the start |
| `Searching.binarySearch`: O(log n) | Keyset pagination: O(log n) | Both jump to a value in sorted data instead of walking to it |
| `Sorting.mergeSort`: O(n log n), sorts everything | Top-N heapsort: O(n log k) | When only the first k rows are needed, keeping a heap of k is cheaper than a full sort |
| | Index order: no sort at all | A B-tree index is data kept sorted in advance, paid for on every insert |

## Index decisions

Indexes were added only where a query plan showed a full scan of a large table, because each index also slows down every insert and update on that table and uses disk space.

| Query | Decision | Reason |
| --- | --- | --- |
| Order history | `idx_orders_user_id` | Seq Scan on 100,000 orders to find 74 |
| Orders by status | `idx_orders_status_order_date` | Seq Scan plus a sort of 20,036 rows to return 20 |
| Monthly revenue | `idx_orders_order_date` | Helps narrow date ranges; kept, with its limited benefit recorded |
| Reviews of a product | No new index | The unique constraint on `(product_id, user_id)` already provides one; 0.14 ms |
| Products by category or price | No new index | Under 0.05 ms; PostgreSQL stops after the first page |
| Top-selling products | No index possible | Must read every order line; solved with caching instead |

## Caching strategy

| Cache | What | Cleared when |
| --- | --- | --- |
| `products` | Product by id | That product is updated or deleted; all entries when a category is renamed (products include the category) |
| `categories` | Category by id | That category is updated or deleted |
| `categoryLists` | Pages of the category list | Any category is created, updated or deleted |
| `users` | User profile by id | That user is updated or deleted |
| `reports` | All three reports | An order is placed; a product is created, updated or deleted |

Only immutable DTO records are cached, never entities. Eviction runs only when the changing method succeeds, so a failed update leaves the cache untouched. Product search and order history were deliberately not cached: their many parameter combinations rarely repeat, and they change with every order.

## Correctness

Speed is only useful if the data stays correct, so both changes were checked for correctness as well as speed.

- **Transactions:** `OrderServiceIntegrationTest` runs 11 integration tests on H2, and all pass. The key tests place an order whose second product lacks stock and confirm that the first product's stock change was rolled back and no order was saved; decline a payment and confirm the order stays `PENDING`; and cancel an order twice and confirm its stock is returned only once.
- **Cache eviction:** checked manually with SQL logging, following the steps in the README ("Manual: cache behavior"). A product read twice runs one query; after an update the next read queries again and returns the new values; placing an order makes the next report request query the database again.

## Limitations

Differences under about 1 ms should be read as trends, not exact results.

- **Run-to-run variance:** endpoints that nothing changed for moved by up to 1.6 ms between runs (section 6), mostly from JVM warm-up after the restart. The large improvements (4× to 180×) are far beyond this noise; the small ones (0.1 to 0.4 ms) are not.
- **Best case for caching:** the warm-up requests filled the cache, so every timed request was a cache hit. In real use, the first request after each eviction pays the full cost.
- **Different date ranges:** the query-plan script measures monthly revenue over 3 months; the endpoint defaults to 12 months. Their numbers are not directly comparable.
- **Localhost and one client:** no network latency, and requests ran one after another. Concurrent load, including competition for the same stock, was not load-tested; the stock update's safety under concurrency comes from its single-statement design.
- **Generated data:** users, dates and statuses are spread evenly at random. Real data is usually skewed (a few very active customers, more recent orders), which changes how selective each index is.
- **Write cost not measured:** the three indexes make inserts and updates on `orders` slightly slower; this was not measured.

A fairer repeat would run more requests per endpoint (for example 200 after 50 warm-ups), several times in random order, and add a load-testing tool such as JMeter or k6 for concurrent users.

## Conclusion

The slow endpoints had two different causes, and each needed its own fix.

| Use an index when | Use caching when |
| --- | --- |
| A query finds a few rows among many, like one customer's orders | A result is expensive to compute and requested often, like reports |
| The plan shows a Seq Scan on a large table to return few rows | The result changes rarely, and you know exactly which events change it |
| The query sorts by the same columns it filters on (composite index) | Stale data must never be shown, and every change can clear the right entries |

The sorting and pagination experiment adds two more rules: sort by indexed columns where possible, and prefer keyset pagination when clients page deep into large lists.

In this project, order history and the admin order list were fixed by indexes, the two reports by caching, and frequently read entities (products, categories, user profiles) were cached to reduce database load. Fast queries were left alone on purpose: an index or a cache that saves nothing still costs memory and complexity.