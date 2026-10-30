#!/usr/bin/env bash
# REST vs GraphQL performance test for the Smart E-Commerce API.
# Usage:   ./perf-test.sh [iterations]        (default 50)
# Needs:   the app running on localhost:8080 with some products in the database.

BASE=${BASE:-http://localhost:8080}
N=${1:-50}
PRODUCT_ID=${PRODUCT_ID:-1}

# Runs a request 5 times untimed (warm-up), then N times timed.
# Prints the average response time and the response size.
measure() {
  local name="$1"; shift
  for i in 1 2 3 4 5; do curl -s -o /dev/null "$@"; done
  for ((i = 1; i <= N; i++)); do
    curl -s -o /dev/null -w "%{time_total} %{size_download}\n" "$@"
  done | awk -v name="$name" '{ t += $1; b = $2 } END { printf "%-52s %9.2f ms %9d bytes\n", name, t / NR * 1000, b }'
}


echo "Resetting AOP metrics..."
curl -s -X DELETE "$BASE/api/metrics/performance" > /dev/null

echo
echo "Running each scenario $N times (after 5 warm-up calls)"
printf "%-52s %12s %15s\n" "SCENARIO" "AVG TIME" "RESPONSE SIZE"
printf '%.0s-' {1..81}; echo

# 1. Same data, all fields
measure "1. List 20 products, all fields      | REST" \
  "$BASE/api/products?size=20"
measure "1. List 20 products, all fields      | GraphQL" \
  -X POST -H "Content-Type: application/json" \
  -d '{"query":"{ products(size: 20) { content { id name description price createdAt updatedAt category { id name } } } }"}' \
  "$BASE/graphql"

# 2. Client only needs name + price (over-fetching)
measure "2. List 20 products, name+price only | REST" \
  "$BASE/api/products?size=20"
measure "2. List 20 products, name+price only | GraphQL" \
  -X POST -H "Content-Type: application/json" \
  -d '{"query":"{ products(size: 20) { content { name price } } }"}' \
  "$BASE/graphql"

# 3. Client needs products + full category details (under-fetching)
measure "3. Products + category details       | REST (call 1)" \
  "$BASE/api/products?size=20"
measure "3. Products + category details       | REST (call 2)" \
  "$BASE/api/categories?size=100"
measure "3. Products + category details       | GraphQL" \
  -X POST -H "Content-Type: application/json" \
  -d '{"query":"{ products(size: 20) { content { name price category { name description } } } }"}' \
  "$BASE/graphql"

# 4. Single product by id
measure "4. Single product by id              | REST" \
  "$BASE/api/products/$PRODUCT_ID"
measure "4. Single product by id              | GraphQL" \
  -X POST -H "Content-Type: application/json" \
  -d "{\"query\":\"{ product(id: $PRODUCT_ID) { id name description price createdAt updatedAt category { id name } } }\"}" \
  "$BASE/graphql"

echo
echo "Saving server-side timings from the AOP aspect to perf-metrics.json..."
curl -s "$BASE/api/metrics/performance" > perf-metrics.json
echo "Done."