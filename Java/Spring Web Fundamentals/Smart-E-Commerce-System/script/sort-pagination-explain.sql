-- ============================================================================
-- Sorting and pagination experiment (PostgreSQL).
--
-- Run:   psql -U admin -d smart_ecommerce -f script/sort-pagination-explain.sql
-- (perf-sort-page.sh runs it automatically and saves the output.)
--
-- Question 1: does the page number matter?
--   Spring turns page=N into OFFSET N*size. PostgreSQL can't jump to row N*size:
--   it reads that many rows and throws them away, so deep pages should be slower.
--   Keyset pagination ("the 20 rows after the last one I saw") is shown as the fix.
--
-- Question 2: does the sort column matter?
--   Sorting by an indexed column (product id) can read rows already in order and stop
--   after 20. Sorting by an unindexed column (price, name) must read every row and sort.
--
-- Read in each plan: the "rows=" of the scan (how many rows were read),
-- "Sort Method" (if any), and "Execution Time".
-- ============================================================================

\set ON_ERROR_STOP on
\pset pager off

-- ---------------------------------------------------------------- inputs
SELECT count(*) AS paid_count FROM orders WHERE status = 'PAID' \gset
SELECT count(*) AS product_count FROM products \gset

-- Offsets of the first row of the middle and last page (page size 20)
SELECT ((:paid_count - 1) / 20) * 20          AS paid_last_offset,
       (((:paid_count - 1) / 20) / 2) * 20    AS paid_mid_offset,
       ((:product_count - 1) / 20) * 20       AS product_last_offset \gset

-- For keyset pagination: the order_date of the last row on the page BEFORE the last page
SELECT :paid_last_offset - 1 AS paid_prev_row \gset
SELECT order_date AS keyset_after
FROM orders WHERE status = 'PAID'
ORDER BY order_date DESC
OFFSET :paid_prev_row ROWS FETCH FIRST 1 ROW ONLY \gset

    \echo 'PAID orders:' :paid_count '  middle page offset:' :paid_mid_offset '  last page offset:' :paid_last_offset
    \echo 'Products:' :product_count '  last page offset:' :product_last_offset
    \echo 'Keyset start (last order_date seen before the last page):' :keyset_after

-- ================================================================ QUESTION 1: page depth
-- Mirrors OrderRepository.findByStatus(PAID, page, size 20, sort orderDate desc)

    \echo '\n===== Orders by status: FIRST page (OFFSET 0) ====='
    EXPLAIN (ANALYZE, BUFFERS)
SELECT o.*, u.*
FROM orders o JOIN users u ON u.user_id = o.user_id
WHERE o.status = 'PAID'
ORDER BY o.order_date DESC
OFFSET 0 ROWS FETCH FIRST 20 ROWS ONLY;

\echo '\n===== Orders by status: MIDDLE page ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.*, u.*
FROM orders o JOIN users u ON u.user_id = o.user_id
WHERE o.status = 'PAID'
ORDER BY o.order_date DESC
OFFSET :paid_mid_offset ROWS FETCH FIRST 20 ROWS ONLY;

\echo '\n===== Orders by status: LAST page ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.*, u.*
FROM orders o JOIN users u ON u.user_id = o.user_id
WHERE o.status = 'PAID'
ORDER BY o.order_date DESC
OFFSET :paid_last_offset ROWS FETCH FIRST 20 ROWS ONLY;

\echo '\n===== Orders by status: LAST page with KEYSET pagination (no OFFSET) ====='
-- Same rows as the last page, but found by "older than the last date already seen".
-- The index jumps straight to that date, like a binary search, instead of counting rows.
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.*, u.*
FROM orders o JOIN users u ON u.user_id = o.user_id
WHERE o.status = 'PAID'
  AND o.order_date < :'keyset_after'
ORDER BY o.order_date DESC
    FETCH FIRST 20 ROWS ONLY;

-- ================================================================ QUESTION 2: sort column
-- Mirrors the product list endpoint (/api/products) with no filters.

\echo '\n===== Products sorted by id (primary key index), first page ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*, c.*
FROM products p JOIN categories c ON c.category_id = p.category_id
ORDER BY p.product_id
OFFSET 0 ROWS FETCH FIRST 20 ROWS ONLY;

\echo '\n===== Products sorted by price (no index), first page ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*, c.*
FROM products p JOIN categories c ON c.category_id = p.category_id
ORDER BY p.price
OFFSET 0 ROWS FETCH FIRST 20 ROWS ONLY;

\echo '\n===== Products sorted by name (no index), first page ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*, c.*
FROM products p JOIN categories c ON c.category_id = p.category_id
ORDER BY p.name
OFFSET 0 ROWS FETCH FIRST 20 ROWS ONLY;

\echo '\n===== Products sorted by id, LAST page ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*, c.*
FROM products p JOIN categories c ON c.category_id = p.category_id
ORDER BY p.product_id
OFFSET :product_last_offset ROWS FETCH FIRST 20 ROWS ONLY;

\echo '\n===== Products sorted by price, LAST page ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*, c.*
FROM products p JOIN categories c ON c.category_id = p.category_id
ORDER BY p.price
OFFSET :product_last_offset ROWS FETCH FIRST 20 ROWS ONLY;