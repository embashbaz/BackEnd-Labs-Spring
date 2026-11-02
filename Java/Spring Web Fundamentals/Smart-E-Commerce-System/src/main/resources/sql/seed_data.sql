-- ============================================================================
-- Large test-data seed for performance measurements (PostgreSQL).
-- Adds ~2,000 users, 5,000 products (+ inventory), 100,000 orders,
-- ~300,000 order items and up to 30,000 reviews.
--
-- Requirements: at least one row in categories.
-- Back up first:  pg_dump -U admin smart_ecommerce > backup.sql
-- Run:            psql -U admin -d smart_ecommerce -f script/seed-large-data.sql
--
-- Random picks use arrays of existing IDs: array[1 + floor(random() * n)]
-- picks a random element. random() in a SELECT list runs once per row.
-- ON CONFLICT DO NOTHING skips rows that would break a unique constraint
-- (duplicate emails, duplicate order lines, second review by same user).
-- ============================================================================

BEGIN;

-- 1. Users (password_hash is a placeholder; these accounts are for data volume only)
INSERT INTO users (full_name, email, password_hash, role, created_at)
SELECT 'Seed User ' || g,
       'seed.user' || g || '@example.com',
       'seed-placeholder-hash',
       'CUSTOMER',
       now() - random() * interval '730 days'
FROM generate_series(1, 2000) AS g
ON CONFLICT DO NOTHING;

-- 2. Products in random existing categories
WITH c AS (SELECT array_agg(category_id) AS ids, count(*)::int AS n FROM categories)
INSERT INTO products (category_id, name, description, price, created_at, updated_at)
SELECT c.ids[1 + floor(random() * c.n)::int],
       'Seed Product ' || g,
       'Generated product for performance testing',
       round((5 + random() * 995)::numeric, 2),
       now(),
       now()
FROM c, generate_series(1, 5000) AS g;

-- 3. Inventory row for every product that does not have one yet
INSERT INTO inventory (product_id, quantity_on_hand, reorder_level, updated_at)
SELECT p.product_id, floor(random() * 500)::int, 5, now()
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.product_id);

-- 4. Orders spread over the last two years with random statuses
WITH u AS (SELECT array_agg(user_id) AS ids, count(*)::int AS n FROM users)
INSERT INTO orders (user_id, order_date, status)
SELECT u.ids[1 + floor(random() * u.n)::int],
       now() - random() * interval '730 days',
       (ARRAY['PENDING','PAID','SHIPPED','DELIVERED','CANCELLED'])[1 + floor(random() * 5)::int]
FROM u, generate_series(1, 100000);

-- 5. Up to 3 lines per order that has no items yet (duplicates are skipped)
WITH p AS (SELECT array_agg(product_id) AS ids, count(*)::int AS n FROM products),
     lines AS (
         SELECT o.order_id,
                p.ids[1 + floor(random() * p.n)::int] AS product_id
         FROM orders o
         CROSS JOIN generate_series(1, 3)
         CROSS JOIN p
         WHERE NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id)
     )
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT l.order_id, l.product_id, 1 + floor(random() * 3)::int, pr.price
FROM lines l
JOIN products pr ON pr.product_id = l.product_id
ON CONFLICT DO NOTHING;

-- 6. Reviews (one per user per product; duplicates are skipped)
WITH p AS (SELECT array_agg(product_id) AS ids, count(*)::int AS n FROM products),
     u AS (SELECT array_agg(user_id)    AS ids, count(*)::int AS n FROM users)
INSERT INTO reviews (product_id, user_id, rating, comment, created_at)
SELECT p.ids[1 + floor(random() * p.n)::int],
       u.ids[1 + floor(random() * u.n)::int],
       (1 + floor(random() * 5))::smallint,
       'Seed review',
       now() - random() * interval '365 days'
FROM p, u, generate_series(1, 30000)
ON CONFLICT DO NOTHING;

COMMIT;

-- Refresh planner statistics so EXPLAIN ANALYZE reflects the new data volume
ANALYZE;

-- Quick check
SELECT 'users' AS table_name, count(*) FROM users
UNION ALL SELECT 'products',    count(*) FROM products
UNION ALL SELECT 'orders',      count(*) FROM orders
UNION ALL SELECT 'order_items', count(*) FROM order_items
UNION ALL SELECT 'reviews',     count(*) FROM reviews;