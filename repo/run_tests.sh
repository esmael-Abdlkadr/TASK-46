#!/bin/bash
###############################################################################
# Unified Test Runner - Workforce & Talent Operations Hub
#
# One-click execution of all unit tests and API functional tests.
# Outputs clear pass/fail summary for acceptance review.
#
# Usage:
#   ./run_tests.sh              # Run all tests (unit + API + frontend Vitest + Playwright E2E in Docker)
#   ./run_tests.sh unit         # Run only unit tests (JUnit via Docker Maven)
#   ./run_tests.sh api          # Run only API tests (curl)
#   ./run_tests.sh js           # Run only Vitest + Playwright (Docker; stack is started first)
#
# Optional:
#   RUN_TESTS_FRESH=1     docker compose down first (keeps DB volume)
#   RUN_TESTS_RESET_DB=1  docker compose down -v first (recreates MySQL; fixes Access denied / stale grants)
#
# Prerequisites: Docker and Docker Compose only. Modes all|api|js run
#   docker compose up -d --build --remove-orphans (no separate docker commands needed).
###############################################################################

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
FINAL_REPORT="$SCRIPT_DIR/test_report.txt"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

RUN_MODE="${1:-all}"

> "$FINAL_REPORT"

echo -e "${BOLD}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Workforce & Talent Operations Hub - Test Suite          ║"
echo "║   $TIMESTAMP                              ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

cat >> "$FINAL_REPORT" << EOF
================================================================
  WORKFORCE & TALENT OPERATIONS HUB - TEST REPORT
  Generated: $TIMESTAMP
================================================================

EOF

###############################################################################
# Pre-flight checks + Docker Compose (single-command UX)
###############################################################################
echo -e "${CYAN}[PRE-FLIGHT] Checking prerequisites...${NC}"

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[ERROR] Docker not found. Install Docker and retry.${NC}"
    exit 1
fi
echo "  Docker:       OK"

# Check Docker Compose
if ! docker compose version &> /dev/null; then
    echo -e "${RED}[ERROR] Docker Compose not found.${NC}"
    exit 1
fi
echo "  Compose:      OK"

# All compose commands run from this script's directory (works if user runs ./run_tests.sh from elsewhere)
cd "$SCRIPT_DIR" || exit 1

START_STACK=0
case "$RUN_MODE" in all|api|js) START_STACK=1 ;; esac

RUNNING=0
if [ "$START_STACK" -eq 1 ]; then
    echo ""
    echo -e "${CYAN}[DOCKER] Resetting DB volume to prevent stale Flyway state...${NC}"
    docker compose down -v --remove-orphans 2>/dev/null || true
    echo -e "${CYAN}[DOCKER] Building images & starting stack (compose up --build)...${NC}"
    echo -e "${YELLOW}         First run may take several minutes (Maven + container pulls).${NC}"
    if ! docker compose up -d --build --remove-orphans; then
        echo -e "${RED}[ERROR] docker compose up failed.${NC}"
        exit 1
    fi
    echo -e "${CYAN}[DOCKER] Waiting for all services healthy (db + face + backend)...${NC}"
    RETRIES=0
    while [ "$RETRIES" -lt 72 ]; do
        RUNNING=$(docker compose ps --format "{{.Name}} {{.Status}}" 2>/dev/null | grep -c "healthy" || true)
        if [ "$RUNNING" -ge 3 ]; then
            break
        fi
        sleep 5
        RETRIES=$((RETRIES + 1))
    done
    if [ "$RUNNING" -lt 3 ]; then
        echo -e "${RED}[ERROR] Services failed to become healthy within timeout (have $RUNNING/3 healthy).${NC}"
        docker compose ps
        echo ""
        echo -e "${YELLOW}[DOCKER] workforce-backend logs (last 40 lines):${NC}"
        docker logs workforce-backend --tail 40 2>&1 || true
        exit 1
    fi
    echo -e "${GREEN}  Services:     $RUNNING/3 healthy${NC}"
    echo ""
    docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null
    echo ""
else
    echo -e "${YELLOW}[INFO] Mode '$RUN_MODE' — skipping full stack (JUnit runs in isolated Maven container).${NC}"
    echo ""
fi

UNIT_EXIT=0
API_EXIT=0
JS_EXIT=0
RAN_JS=0
UNIT_TOTAL=0; UNIT_PASSED=0; UNIT_FAILED=0
API_TOTAL=0; API_PASSED=0; API_FAILED=0

###############################################################################
# Unit Tests
###############################################################################
if [ "$RUN_MODE" = "all" ] || [ "$RUN_MODE" = "unit" ]; then
    echo -e "${BOLD}┌──────────────────────────────────────────────────────────┐${NC}"
    echo -e "${BOLD}│  PHASE 1: UNIT TESTS (JUnit 5 via Maven)                │${NC}"
    echo -e "${BOLD}└──────────────────────────────────────────────────────────┘${NC}"
    echo ""

    bash "$SCRIPT_DIR/unit_tests/run_unit_tests.sh" || true
    UNIT_EXIT=${PIPESTATUS[0]:-$?}

    # Parse unit results
    if [ -f "$SCRIPT_DIR/unit_tests/unit_test_results.txt" ]; then
        UNIT_TOTAL=$(grep "Total:" "$SCRIPT_DIR/unit_tests/unit_test_results.txt" | head -1 | grep -oE '[0-9]+' || echo "0")
        UNIT_PASSED=$(grep "Passed:" "$SCRIPT_DIR/unit_tests/unit_test_results.txt" | head -1 | grep -oE '[0-9]+' || echo "0")
        UNIT_FAILED=$(grep "Failed:" "$SCRIPT_DIR/unit_tests/unit_test_results.txt" | head -1 | grep -oE '[0-9]+' || echo "0")
        cat "$SCRIPT_DIR/unit_tests/unit_test_results.txt" >> "$FINAL_REPORT"
    fi
    echo ""
fi

###############################################################################
# API Tests
###############################################################################
if [ "$RUN_MODE" = "all" ] || [ "$RUN_MODE" = "api" ]; then
    echo -e "${BOLD}┌──────────────────────────────────────────────────────────┐${NC}"
    echo -e "${BOLD}│  PHASE 2: API FUNCTIONAL TESTS (HTTP/curl)              │${NC}"
    echo -e "${BOLD}└──────────────────────────────────────────────────────────┘${NC}"
    echo ""

    bash "$SCRIPT_DIR/API_tests/run_api_tests.sh" || true
    API_EXIT=${PIPESTATUS[0]:-$?}

    # Parse API results
    if [ -f "$SCRIPT_DIR/API_tests/api_test_results.txt" ]; then
        API_TOTAL=$(grep "Total:" "$SCRIPT_DIR/API_tests/api_test_results.txt" | head -1 | grep -oE '[0-9]+' || echo "0")
        API_PASSED=$(grep "Passed:" "$SCRIPT_DIR/API_tests/api_test_results.txt" | head -1 | grep -oE '[0-9]+' || echo "0")
        API_FAILED=$(grep "Failed:" "$SCRIPT_DIR/API_tests/api_test_results.txt" | head -1 | grep -oE '[0-9]+' || echo "0")
        echo "" >> "$FINAL_REPORT"
        cat "$SCRIPT_DIR/API_tests/api_test_results.txt" >> "$FINAL_REPORT"
    fi
    echo ""
fi

###############################################################################
# Frontend unit + browser E2E (Vitest + Playwright — Docker only, no host npm)
###############################################################################
if [ "$RUN_MODE" = "all" ] || [ "$RUN_MODE" = "js" ]; then
    echo -e "${BOLD}┌──────────────────────────────────────────────────────────┐${NC}"
    echo -e "${BOLD}│  PHASE 3: FRONTEND + E2E (Vitest + Playwright in Docker) │${NC}"
    echo -e "${BOLD}└──────────────────────────────────────────────────────────┘${NC}"
    echo ""

    RAN_JS=1
    bash "$SCRIPT_DIR/docker/js-tests/run-js-tests.sh" 2>&1 | tee -a "$FINAL_REPORT"
    JS_EXIT=${PIPESTATUS[0]}
    echo ""
fi

###############################################################################
# Final Summary
###############################################################################
GRAND_TOTAL=$((UNIT_TOTAL + API_TOTAL))
GRAND_PASSED=$((UNIT_PASSED + API_PASSED))
GRAND_FAILED=$((UNIT_FAILED + API_FAILED))

echo -e "${BOLD}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║              FINAL TEST RESULTS SUMMARY                   ║"
echo "╠════════════════════════════════════════════════════════════╣"
printf "║  %-20s %8s %8s %8s  ║\n" "Category" "Total" "Passed" "Failed"
echo "╠════════════════════════════════════════════════════════════╣"
printf "║  %-20s %8s %8s %8s  ║\n" "Unit Tests" "$UNIT_TOTAL" "$UNIT_PASSED" "$UNIT_FAILED"
printf "║  %-20s %8s %8s %8s  ║\n" "API Tests" "$API_TOTAL" "$API_PASSED" "$API_FAILED"
if [ "$RAN_JS" -eq 1 ]; then
    if [ "$JS_EXIT" -eq 0 ]; then
        printf "║  %-20s %8s %8s %8s  ║\n" "JS Vitest+E2E" "1" "1" "0"
    else
        printf "║  %-20s %8s %8s %8s  ║\n" "JS Vitest+E2E" "1" "0" "1"
    fi
fi
echo "╠════════════════════════════════════════════════════════════╣"
printf "║  %-20s %8s %8s %8s  ║\n" "GRAND TOTAL" "$GRAND_TOTAL" "$GRAND_PASSED" "$GRAND_FAILED"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

cat >> "$FINAL_REPORT" << EOF

================================================================
  FINAL SUMMARY
================================================================
  Unit Tests:    $UNIT_TOTAL total, $UNIT_PASSED passed, $UNIT_FAILED failed
  API Tests:     $API_TOTAL total, $API_PASSED passed, $API_FAILED failed
EOF
if [ "$RAN_JS" -eq 1 ]; then
    if [ "$JS_EXIT" -eq 0 ]; then
        echo "  JS Vitest+E2E: PASSED (Docker)" >> "$FINAL_REPORT"
    else
        echo "  JS Vitest+E2E: FAILED (Docker, exit $JS_EXIT)" >> "$FINAL_REPORT"
    fi
fi
cat >> "$FINAL_REPORT" << EOF
  GRAND TOTAL:   $GRAND_TOTAL total, $GRAND_PASSED passed, $GRAND_FAILED failed
================================================================
EOF

if [ "$GRAND_FAILED" -eq 0 ] && [ "$UNIT_EXIT" -eq 0 ] && [ "$API_EXIT" -eq 0 ] \
    && { [ "$RAN_JS" -eq 0 ] || [ "$JS_EXIT" -eq 0 ]; }; then
    echo -e "${GREEN}${BOLD}  ✓ ALL TESTS PASSED - SYSTEM ACCEPTANCE CRITERIA MET${NC}"
    echo "" >> "$FINAL_REPORT"
    echo "  RESULT: ALL TESTS PASSED - SYSTEM ACCEPTANCE CRITERIA MET" >> "$FINAL_REPORT"
    exit 0
else
    echo -e "${RED}${BOLD}  ✗ SOME TESTS FAILED - REVIEW REQUIRED${NC}"
    echo "" >> "$FINAL_REPORT"
    echo "  RESULT: SOME TESTS FAILED - REVIEW REQUIRED" >> "$FINAL_REPORT"
    exit 1
fi
