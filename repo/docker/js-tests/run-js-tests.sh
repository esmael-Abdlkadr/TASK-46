#!/bin/bash
###############################################################################
# Runs Vitest + Playwright entirely inside Docker (no host Node/npm required).
#
# Usage (from repo root):
#   bash docker/js-tests/run-js-tests.sh
#
# Env:
#   E2E_BASE_URL — default http://host.docker.internal:8081 (backend on host)
###############################################################################
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

PLAYWRIGHT_IMAGE="${PLAYWRIGHT_IMAGE:-mcr.microsoft.com/playwright:v1.59.1-jammy}"

if ! command -v docker &>/dev/null; then
  echo "[ERROR] Docker is required."
  exit 1
fi

cd "$REPO_ROOT"

if [ ! -f package-lock.json ]; then
  echo "[ERROR] package-lock.json missing. Generate once with:"
  echo "  docker run --rm -v \"\$PWD:/app\" -w /app node:20-bookworm-slim npm install"
  exit 1
fi

EXTRA_HOST=(--add-host=host.docker.internal:host-gateway)
E2E_BASE_URL="${E2E_BASE_URL:-http://host.docker.internal:8081}"

echo "[INFO] JS tests via ${PLAYWRIGHT_IMAGE}"
echo "[INFO] E2E_BASE_URL=${E2E_BASE_URL}"

docker run --rm \
  "${EXTRA_HOST[@]}" \
  -v "$REPO_ROOT:/app" \
  -w /app \
  -e CI=1 \
  -e E2E_BASE_URL="$E2E_BASE_URL" \
  "$PLAYWRIGHT_IMAGE" \
  bash -ce "
    npm ci
    npm run test:fe
    npx playwright test
  "
