-- ============================================================================
-- Query baseline: run BEFORE adding indexes, save the output, run again AFTER.
--
-- Run:   psql -U admin -d smart_ecommerce -f script/baseline-explain.sql > baseline-before.txt
--
-- Each query mirrors the SQL Hibernate generates for a repository method.
-- EXPLAIN (ANALYZE, BUFFERS) really runs the query and shows:
--   - "Seq Scan"   = read the whole table row by row (slow on big tables)
--   - "Index Scan" = jump straight to matching rows using an index
--   - "Execution Time" at the bottom = the number to record
-- Run the script twice and record the SECOND run: the first one is slower
-- because the data is not yet in memory ("cold cache").
-- ============================================================================

\timing off
\pset pager off

-- ---------------------------------------------------------------- 0. Inputs
-- Pick realistic test values and store them in psql variables (\gset).

SELECT user_id AS uid FROM orders
GROUP BY user_id ORDER BY count(*) DESC LIMIT 1 \gset

SELECT product_id AS pid FROM reviews
GROUP BY product_id ORDER BY count(*) DESC LIMIT 1 \gset

SELECT c.name AS cat FROM products p JOIN categories c ON c.category_id = p.category_id
GROUP BY c.name ORDER BY count(*) DESC LIMIT 1 \gset

\echo 'Test user id:' :uid '  product id:' :pid '  category:' :cat

-- ---------------------------------------------------------------- 1. Existing indexes
\echo '\n===== Existing indexes ====='
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('users', 'categories', 'products', 'inventory', 'orders', 'order_items', 'reviews')
ORDER BY tablename, indexname;

-- ---------------------------------------------------------------- 2. Order history
\echo '\n===== OrderRepository.findOrderHistory (data query) ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.order_id, o.order_date, o.status, SUM(i.quantity), SUM(i.unit_price * i.quantity)
FROM orders o
         JOIN order_items i ON i.order_id = o.order_id
WHERE o.user_id = :uid
GROUP BY o.order_id, o.order_date, o.status
ORDER BY o.order_date DESC
LIMIT 20;

\echo '\n===== OrderRepository.findOrderHistory (count query) ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM orders WHERE user_id = :uid;

-- ---------------------------------------------------------------- 3. Orders by status
\echo '\n===== OrderRepository.findByStatus (data query) ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.*, u.*
FROM orders o
         JOIN users u ON u.user_id = o.user_id
WHERE o.status = 'PAID'
ORDER BY o.order_date DESC
LIMIT 20;

\echo '\n===== OrderRepository.findByStatus (count query) ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM orders WHERE status = 'PAID';

-- ---------------------------------------------------------------- 4. Reviews of a product
\echo '\n===== ReviewRepository.findByProductId ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT r.*, u.*
FROM reviews r
         JOIN users u ON u.user_id = r.user_id
WHERE r.product_id = :pid
ORDER BY r.created_at DESC
LIMIT 20;

-- ---------------------------------------------------------------- 5. Products
\echo '\n===== ProductRepository.findByCategoryNameIgnoreCase ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*, c.*
FROM products p
         JOIN categories c ON c.category_id = p.category_id
WHERE upper(c.name) = upper(:'cat')
ORDER BY p.product_id
LIMIT 20;

\echo '\n===== ProductRepository.findByPriceBetween ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*, c.*
FROM products p
         JOIN categories c ON c.category_id = p.category_id
WHERE p.price BETWEEN 100 AND 200
ORDER BY p.product_id
LIMIT 20;

-- ---------------------------------------------------------------- 6. Reports
\echo '\n===== OrderRepository.findMonthlyRevenue (last 3 months) ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT to_char(date_trunc('month', o.order_date), 'YYYY-MM'),
       COUNT(DISTINCT o.order_id),
       SUM(oi.quantity * oi.unit_price)
FROM orders o
         JOIN order_items oi ON oi.order_id = o.order_id
WHERE o.status <> 'CANCELLED'
  AND o.order_date >= now() - interval '3 months'
  AND o.order_date <  now()
GROUP BY 1
ORDER BY 1;

\echo '\n===== OrderItemRepository.findTopSellingProducts ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.product_id, p.name, SUM(oi.quantity), SUM(oi.quantity * oi.unit_price)
FROM order_items oi
         JOIN orders   o ON o.order_id   = oi.order_id
         JOIN products p ON p.product_id = oi.product_id
WHERE o.status <> 'CANCELLED'
GROUP BY p.product_id, p.name
ORDER BY 3 DESC
LIMIT 10;