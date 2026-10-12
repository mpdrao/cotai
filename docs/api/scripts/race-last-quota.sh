#!/usr/bin/env bash
# Real concurrency check for the "last quota" race (EC01): N parallel reservations for the same item.
# Expected: exactly AVAILABLE responses 201 and the rest 409 QUOTA_UNAVAILABLE.
#
# Usage: BASE=http://localhost:8080 ITEM_ID=1 N=20 ./race-last-quota.sh
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
ITEM_ID="${ITEM_ID:?set ITEM_ID (an item of a PUBLISHED list)}"
N="${N:-20}"

for i in $(seq 1 "$N"); do
  (
    key="$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)"
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BASE/api/public/items/$ITEM_ID/reservations" \
      -H "Content-Type: application/json" \
      -H "Idempotency-Key: $key" \
      -d "{\"guestName\":\"Guest $i\",\"guestEmail\":\"guest$i@example.com\",\"quantity\":1}"
  ) &
done | sort | uniq -c
wait
