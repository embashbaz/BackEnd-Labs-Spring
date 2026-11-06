#!/usr/bin/env bash
# =============================================================================
# Sorting and pagination experiment: query plans + endpoint response times.
#
# Usage (app running with the dev profile, indexes added, seed data loaded):
#   ./script/perf-sort-page.sh
#
# Saves to perf-results/ (next to the script/ folder):
#   endpoint-timings.csv              rows appended with the label "sort-page"
#   sort-page-<time>.txt              readable summary of this run
#   explain-sort-page-<time>.txt      EXPLAIN ANALYZE output
#
# Optional settings: BASE_URL, REQUESTS (default 50), WARMUP (default 5), DB_USER, DB_NAME
# Works with the bash 3.2 that ships with macOS.
# =============================================================================
set -euo pipefail

LABEL="sort-page"
BASE_URL="${BASE_URL:-http://localhost:8080}"
REQUESTS="${REQUESTS:-50}"
WARMUP="${WARMUP:-5}"
DB_USER="${DB_USER:-admin}"
DB_NAME="${DB_NAME:-smart_ecommerce}"
PAGE_SIZE=20

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/../perf-results"
EXPLAIN_SQL="$SCRIPT_DIR/sort-pagination-explain.sql"

RUN_AT="$(date +%Y-%m-%dT%H:%M:%S)"
STAMP="$(date +%Y%m%d-%H%M%S)"
CSV="$RESULTS_DIR/endpoint-timings.csv"
SUMMARY="$RESULTS_DIR/sort-page-$STAMP.txt"
EXPLAIN_OUT="$RESULTS_DIR/explain-sort-page-$STAMP.txt"

mkdir -p "$RESULTS_DIR"

# ----------------------------------------------------------------- checks
for cmd in curl psql awk; do
    command -v "$cmd" > /dev/null || { echo "Missing required command: $cmd"; exit 1; }
done
[ -f "$EXPLAIN_SQL" ] || { echo "Missing $EXPLAIN_SQL (keep it next to this script)"; exit 1; }

code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/categories" < /dev/null)" || true
if [ "$code" != "200" ]; then
    echo "App not reachable at $BASE_URL (HTTP $code). Start it with the dev profile first."
    exit 1
fi

psql_value() {
    psql -U "$DB_USER" -d "$DB_NAME" -tAc "$1" < /dev/null
}

PAID_COUNT="$(psql_value "SELECT count(*) FROM orders WHERE status = 'PAID'")"
PRODUCT_COUNT="$(psql_value "SELECT count(*) FROM products")"
PAID_LAST_PAGE=$(( (PAID_COUNT - 1) / PAGE_SIZE ))
PAID_MID_PAGE=$(( PAID_LAST_PAGE / 2 ))
PRODUCT_LAST_PAGE=$(( (PRODUCT_COUNT - 1) / PAGE_SIZE ))

# ----------------------------------------------------------------- part A: database
echo "Part A: EXPLAIN ANALYZE (run twice, keeping the second, warm run)..."
psql -U "$DB_USER" -d "$DB_NAME" -f "$EXPLAIN_SQL" > /dev/null < /dev/null
psql -U "$DB_USER" -d "$DB_NAME" -f "$EXPLAIN_SQL" > "$EXPLAIN_OUT" < /dev/null
echo "  saved: $EXPLAIN_OUT"
echo "  quick view (query, then scans, sorts and execution time):"
grep -E "=====|Scan|Sort Method|Execution Time" "$EXPLAIN_OUT" | sed 's/^/    /'
echo

# ----------------------------------------------------------------- part B: endpoints
if [ ! -f "$CSV" ]; then
    echo "run_at,label,endpoint,path,requests,avg_ms,min_ms,p95_ms,max_ms,errors" > "$CSV"
fi

{
    echo "Sorting and pagination: endpoint timings"
    echo "  run at: $RUN_AT   base url: $BASE_URL   requests: $REQUESTS (after $WARMUP warm-up)"
    echo "  PAID orders: $PAID_COUNT (pages 0..$PAID_LAST_PAGE)   products: $PRODUCT_COUNT (pages 0..$PRODUCT_LAST_PAGE)   page size: $PAGE_SIZE"
    echo
    printf "%-28s %8s %8s %8s %8s %6s\n" "endpoint" "avg ms" "min ms" "p95 ms" "max ms" "errors"
    printf "%-28s %8s %8s %8s %8s %6s\n" "--------" "------" "------" "------" "------" "------"
} | tee "$SUMMARY"

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

    stats="$(sort -n "$times_file" | awk '
        { v[NR] = $1 * 1000; sum += $1 * 1000 }
        END {
            p = int(NR * 0.95 + 0.999); if (p < 1) p = 1; if (p > NR) p = NR
            printf "%.1f %.1f %.1f %.1f", sum / NR, v[1], v[p], v[NR]
        }')"
    rm -f "$times_file"

    set -- $stats
    printf "%-28s %8s %8s %8s %8s %6s\n" "$name" "$1" "$2" "$3" "$4" "$errors" | tee -a "$SUMMARY"
    echo "$RUN_AT,$LABEL,$name,\"$path\",$REQUESTS,$1,$2,$3,$4,$errors" >> "$CSV"
}

echo "Part B: timing endpoints..." >&2

while IFS='|' read -r name path; do
    [ -z "$name" ] && continue
    measure "$name" "$path"
done << EOF
orders-page-first|/api/orders?status=PAID&page=0&size=$PAGE_SIZE
orders-page-middle|/api/orders?status=PAID&page=$PAID_MID_PAGE&size=$PAGE_SIZE
orders-page-last|/api/orders?status=PAID&page=$PAID_LAST_PAGE&size=$PAGE_SIZE
products-sort-id|/api/products?page=0&size=$PAGE_SIZE&sort=id
products-sort-price|/api/products?page=0&size=$PAGE_SIZE&sort=price
products-sort-name|/api/products?page=0&size=$PAGE_SIZE&sort=name
products-last-page-id|/api/products?page=$PRODUCT_LAST_PAGE&size=$PAGE_SIZE&sort=id
products-last-page-price|/api/products?page=$PRODUCT_LAST_PAGE&size=$PAGE_SIZE&sort=price
EOF

{
    echo
    echo "Errors = responses that were not HTTP 200. Any non-zero value means that row's timing is not valid."
} | tee -a "$SUMMARY"

echo
echo "Saved:"
echo "  $SUMMARY"
echo "  $EXPLAIN_OUT"
echo "  $CSV  (rows labelled '$LABEL')"