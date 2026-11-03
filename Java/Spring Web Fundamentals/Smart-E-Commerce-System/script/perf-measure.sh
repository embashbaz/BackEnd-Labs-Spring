#!/usr/bin/env bash
# =============================================================================
# Performance measurement: database query plans + endpoint response times.
#
# Usage (app must be running with the dev profile):
#   ./script/perf-measure.sh before
#   ./script/perf-measure.sh after-indexes
#   ./script/perf-measure.sh after-caching
#
# Saves to perf-results/ (next to the script/ folder):
#   endpoint-timings.csv            all runs, one row per endpoint per run (compare labels)
#   endpoints-<label>-<time>.txt    readable summary of this run
#   explain-<label>-<time>.txt      EXPLAIN ANALYZE output of this run
#
# Optional settings (environment variables):
#   BASE_URL=http://localhost:8080   REQUESTS=50   WARMUP=5
#   DB_USER=admin   DB_NAME=smart_ecommerce
#   USER_ID=...  PRODUCT_ID=...      fix the test IDs so every run uses the same ones
#
# Works with the bash 3.2 that ships with macOS.
# =============================================================================
set -euo pipefail

LABEL="${1:-}"
if [ -z "$LABEL" ]; then
    echo "Usage: $0 <label>   e.g. before, after-indexes, after-caching"
    exit 1
fi

BASE_URL="${BASE_URL:-http://localhost:8080}"
REQUESTS="${REQUESTS:-50}"
WARMUP="${WARMUP:-5}"
DB_USER="${DB_USER:-admin}"
DB_NAME="${DB_NAME:-smart_ecommerce}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../perf-results"
EXPLAIN_SQL="$SCRIPT_DIR/baseline-explain.sql"

RUN_AT="$(date +%Y-%m-%dT%H:%M:%S)"
STAMP="$(date +%Y%m%d-%H%M%S)"
CSV="$RESULTS_DIR/endpoint-timings.csv"
SUMMARY="$RESULTS_DIR/endpoints-$LABEL-$STAMP.txt"
EXPLAIN_OUT="$RESULTS_DIR/explain-$LABEL-$STAMP.txt"

mkdir -p "$RESULTS_DIR"

# ----------------------------------------------------------------- checks
for cmd in curl psql awk; do
    command -v "$cmd" > /dev/null || { echo "Missing required command: $cmd"; exit 1; }
done

code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/categories" < /dev/null)" || true
if [ "$code" != "200" ]; then
    echo "App not reachable at $BASE_URL (HTTP $code). Start it with the dev profile first."
    exit 1
fi

psql_value() {
    psql -U "$DB_USER" -d "$DB_NAME" -tAc "$1" < /dev/null
}

# Test IDs: the user with the most orders and the product with the most reviews.
# Override with USER_ID / PRODUCT_ID to keep them identical across runs.
USER_ID="${USER_ID:-$(psql_value "SELECT user_id FROM orders GROUP BY user_id ORDER BY count(*) DESC, user_id LIMIT 1")}"
PRODUCT_ID="${PRODUCT_ID:-$(psql_value "SELECT product_id FROM reviews GROUP BY product_id ORDER BY count(*) DESC, product_id LIMIT 1")}"

if [ -z "$USER_ID" ] || [ -z "$PRODUCT_ID" ]; then
    echo "No orders or reviews found. Run script/seed-large-data.sql first."
    exit 1
fi

# ----------------------------------------------------------------- part A: database
if [ -f "$EXPLAIN_SQL" ]; then
    echo "Part A: EXPLAIN ANALYZE (run twice, keeping the second, warm run)..."
    psql -U "$DB_USER" -d "$DB_NAME" -f "$EXPLAIN_SQL" > /dev/null < /dev/null
    psql -U "$DB_USER" -d "$DB_NAME" -f "$EXPLAIN_SQL" > "$EXPLAIN_OUT" < /dev/null
    echo "  saved: $EXPLAIN_OUT"
    echo "  quick view (query name, then its plan's scans and execution time):"
    grep -E "=====|Seq Scan|Index Scan|Index Only Scan|Bitmap Heap Scan|Execution Time" "$EXPLAIN_OUT" \
        | grep -v "Existing indexes" | sed 's/^/    /'
    echo
else
    echo "Part A skipped: $EXPLAIN_SQL not found."
    echo
fi

# ----------------------------------------------------------------- part B: endpoints
if [ ! -f "$CSV" ]; then
    echo "run_at,label,endpoint,path,requests,avg_ms,min_ms,p95_ms,max_ms,errors" > "$CSV"
fi

{
    echo "Endpoint timings"
    echo "  label: $LABEL   run at: $RUN_AT"
    echo "  base url: $BASE_URL   requests: $REQUESTS (after $WARMUP warm-up)"
    echo "  test user id: $USER_ID   test product id: $PRODUCT_ID"
    echo
    printf "%-24s %8s %8s %8s %8s %6s\n" "endpoint" "avg ms" "min ms" "p95 ms" "max ms" "errors"
    printf "%-24s %8s %8s %8s %8s %6s\n" "--------" "------" "------" "------" "------" "------"
} | tee "$SUMMARY"

# Times one endpoint: warm-up requests (ignored), then REQUESTS measured requests.
measure() {
    local name="$1" path="$2"
    local url="$BASE_URL$path"
    local times_file errors=0 i out code t stats
    times_file="$(mktemp)"

    for i in $(seq 1 "$WARMUP"); do
        curl -s -o /dev/null "$url" < /dev/null || true
    done

    for i in $(seq 1 "$REQUESTS"); do
        out="$(curl -s -o /dev/null -w "%{http_code} %{time_total}" "$url" < /dev/null)" || true
        code="${out%% *}"
        t="${out##* }"
        if [ "$code" != "200" ]; then
            errors=$((errors + 1))
        fi
        echo "$t" >> "$times_file"
    done

    # avg, min, p95, max in milliseconds (p95 = 95% of requests were at least this fast)
    stats="$(sort -n "$times_file" | awk '
        { v[NR] = $1 * 1000; sum += $1 * 1000 }
        END {
            p = int(NR * 0.95 + 0.999); if (p < 1) p = 1; if (p > NR) p = NR
            printf "%.1f %.1f %.1f %.1f", sum / NR, v[1], v[p], v[NR]
        }')"
    rm -f "$times_file"

    set -- $stats
    printf "%-24s %8s %8s %8s %8s %6s\n" "$name" "$1" "$2" "$3" "$4" "$errors" | tee -a "$SUMMARY"
    echo "$RUN_AT,$LABEL,$name,\"$path\",$REQUESTS,$1,$2,$3,$4,$errors" >> "$CSV"
}

echo "Part B: timing endpoints..." >&2

# name|path   (first group: query optimization, second group: caching targets)
while IFS='|' read -r name path; do
    [ -z "$name" ] && continue
    measure "$name" "$path"
done << EOF
order-history|/api/users/$USER_ID/orders
orders-by-status|/api/orders?status=PAID
product-reviews|/api/products/$PRODUCT_ID/reviews
product-rating|/api/products/$PRODUCT_ID/rating
report-monthly-revenue|/api/reports/monthly-revenue
report-top-products|/api/reports/top-products?limit=10
report-low-stock|/api/reports/low-stock
products-list|/api/products?page=0&size=20
product-by-id|/api/products/$PRODUCT_ID
categories|/api/categories
user-by-id|/api/users/$USER_ID
EOF

{
    echo
    echo "Errors = responses that were not HTTP 200. Any non-zero value means that row's timing is not valid."
} | tee -a "$SUMMARY"

echo
echo "Saved:"
echo "  $SUMMARY"
echo "  $CSV  (all runs; compare rows by label)"
[ -f "$EXPLAIN_OUT" ] && echo "  $EXPLAIN_OUT"