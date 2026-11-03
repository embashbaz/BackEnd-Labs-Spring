-- ============================================================================
-- Indexes chosen from the "before" EXPLAIN ANALYZE results.
-- Only queries that showed a Seq Scan on a large table get an index:
-- every index also slows down INSERT/UPDATE slightly and uses disk space.
--
-- Run:   psql -U admin -d smart_ecommerce -f script/add-indexes.sql
-- Then:  USER_ID=1362 PRODUCT_ID=3153 ./script/perf-measure.sh after-indexes
-- ============================================================================

-- 1. Order history: WHERE o.user_id = ?
--    Before: Seq Scan on orders (100,000 rows read to find 74).
--    PostgreSQL does not index foreign key columns automatically.
CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON orders (user_id);

-- 2. Orders by status: WHERE o.status = ? ORDER BY o.order_date DESC LIMIT 20
--    Before: Seq Scan on orders, then sort ~20,000 PAID rows to keep 20.
--    With (status, order_date) the matching rows are already stored in date order,
--    so PostgreSQL can read the newest 20 and stop.
CREATE INDEX IF NOT EXISTS idx_orders_status_order_date
    ON orders (status, order_date);

-- 3. Monthly revenue: WHERE o.order_date >= ? AND o.order_date < ?
--    Expected to help narrow date ranges (a few months) but not wide ones
--    (12 months = about half the table, where a full scan is cheaper).
CREATE INDEX IF NOT EXISTS idx_orders_order_date
    ON orders (order_date);

-- Not indexed, on purpose (already fast in the baseline):
--   reviews by product  -> the unique constraint (product_id, user_id) already provides an index
--   products by category / price -> under 0.05 ms
-- Not fixable with an index:
--   top-selling products -> must read every order line; candidate for caching instead

-- Refresh planner statistics so the new indexes are considered right away
ANALYZE orders;

-- Show the result
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'orders'
ORDER BY indexname;

-- To undo (e.g. to re-measure "before"):
-- DROP INDEX IF EXISTS idx_orders_user_id;
-- DROP INDEX IF EXISTS idx_orders_status_order_date;
-- DROP INDEX IF EXISTS idx_orders_order_date;