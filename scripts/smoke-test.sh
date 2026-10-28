#!/usr/bin/env sh
set -eu

BASE_URL="${1:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"

echo "Checking health at ${BASE_URL} ..."
curl --fail --silent --show-error "${BASE_URL}/actuator/health"
echo

echo "Creating a demo task ..."
IDEMPOTENCY_KEY="smoke-$(date +%s)-$$"
curl --fail --silent --show-error \
  -X POST "${BASE_URL}/api/tasks" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  -d '{"type":"DEMO","payload":"hello from shell smoke test"}'
echo

echo "Smoke test passed."
