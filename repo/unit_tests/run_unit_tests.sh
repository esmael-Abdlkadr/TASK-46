#!/bin/bash
###############################################################################
# Unit Test Runner - Workforce & Talent Operations Hub
#
# Runs all JUnit 5 unit tests via Maven inside a Docker container using the
# same network as the running docker-compose services (H2 in-memory DB).
#
# Tests cover: services, security, crypto, masking, controllers
###############################################################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REPORT_FILE="$SCRIPT_DIR/unit_test_results.txt"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo "============================================================"
echo "  UNIT TEST SUITE - Workforce & Talent Operations Hub"
echo "  Started: $TIMESTAMP"
echo "============================================================"
echo ""

> "$REPORT_FILE"
echo "Unit Test Report - $TIMESTAMP" >> "$REPORT_FILE"
echo "============================================================" >> "$REPORT_FILE"

###############################################################################
# Run JUnit tests via Maven in a Docker container
###############################################################################
echo -e "${CYAN}[INFO] Running JUnit 5 unit tests via Maven...${NC}"
echo ""

cd "$PROJECT_ROOT"

# MSYS_NO_PATHCONV prevents Git Bash from converting /workspace to C:/Program Files/Git/workspace
export MSYS_NO_PATHCONV=1

docker run --rm \
    -v "$(pwd)":/workspace \
    -w /workspace \
    --network host \
    maven:3.9-eclipse-temurin-17 \
    mvn test -B \
        -Dspring.profiles.active=test \
        -Dsurefire.useFile=false \
        2>&1 | tee "$SCRIPT_DIR/maven_output.log"

MVN_EXIT=${PIPESTATUS[0]}

###############################################################################
# Parse results (portable grep - no -P flag)
###############################################################################
echo "" >> "$REPORT_FILE"

TOTAL=0
PASSED=0
FAILED=0
ERRORS=0
SKIPPED=0

# Extract numbers from surefire summary lines
extract_num() {
    echo "$1" | sed "s/.*$2: *\([0-9]*\).*/\1/"
}

while IFS= read -r line; do
    t=$(extract_num "$line" "Tests run")
    f=$(extract_num "$line" "Failures")
    e=$(extract_num "$line" "Errors")
    s=$(extract_num "$line" "Skipped")
    if [ -n "$t" ] && [ "$t" -gt 0 ] 2>/dev/null; then
        TOTAL=$((TOTAL + t))
        FAILED=$((FAILED + f))
        ERRORS=$((ERRORS + e))
        SKIPPED=$((SKIPPED + s))
    fi
done < <(grep "Tests run:" "$SCRIPT_DIR/maven_output.log" | grep -v "\[INFO\] Tests run: 0")

PASSED=$((TOTAL - FAILED - ERRORS - SKIPPED))

echo ""
echo "============================================================"
echo "  UNIT TEST RESULTS SUMMARY"
echo "============================================================"
echo ""

echo -e "${CYAN}Test Classes:${NC}"
echo "" >> "$REPORT_FILE"
echo "Test Classes:" >> "$REPORT_FILE"

grep -E "Tests run:.*in " "$SCRIPT_DIR/maven_output.log" | while IFS= read -r line; do
    classname=$(echo "$line" | sed 's/.*in //' | sed 's/ *$//')
    runs=$(extract_num "$line" "Tests run")
    fails=$(extract_num "$line" "Failures")
    errs=$(extract_num "$line" "Errors")
    if [ "$fails" = "0" ] && [ "$errs" = "0" ]; then
        echo -e "  ${GREEN}PASS${NC}  $classname ($runs tests)"
        echo "  PASS  $classname ($runs tests)" >> "$REPORT_FILE"
    else
        echo -e "  ${RED}FAIL${NC}  $classname ($runs tests, $fails failures, $errs errors)"
        echo "  FAIL  $classname ($runs tests, $fails failures, $errs errors)" >> "$REPORT_FILE"
    fi
done

echo ""
echo "------------------------------------------------------------"
echo "  Total:    $TOTAL"
echo "  Passed:   $PASSED"
echo "  Failed:   $FAILED"
echo "  Errors:   $ERRORS"
echo "  Skipped:  $SKIPPED"
echo "------------------------------------------------------------"

echo "" >> "$REPORT_FILE"
echo "------------------------------------------------------------" >> "$REPORT_FILE"
echo "  Total:    $TOTAL" >> "$REPORT_FILE"
echo "  Passed:   $PASSED" >> "$REPORT_FILE"
echo "  Failed:   $FAILED" >> "$REPORT_FILE"
echo "  Errors:   $ERRORS" >> "$REPORT_FILE"
echo "  Skipped:  $SKIPPED" >> "$REPORT_FILE"
echo "------------------------------------------------------------" >> "$REPORT_FILE"

if [ "$FAILED" -eq 0 ] && [ "$ERRORS" -eq 0 ] && [ "$MVN_EXIT" -eq 0 ]; then
    echo ""
    echo -e "${GREEN}=== ALL UNIT TESTS PASSED ===${NC}"
    echo "" >> "$REPORT_FILE"
    echo "=== ALL UNIT TESTS PASSED ===" >> "$REPORT_FILE"
    exit 0
else
    echo ""
    echo -e "${RED}=== UNIT TESTS FAILED ===${NC}"
    echo "" >> "$REPORT_FILE"
    echo "=== UNIT TESTS FAILED ===" >> "$REPORT_FILE"
    if grep -q "<<< FAIL" "$SCRIPT_DIR/maven_output.log"; then
        echo ""
        echo -e "${RED}Failure Details:${NC}"
        echo "Failure Details:" >> "$REPORT_FILE"
        grep -B2 "<<< FAIL\|<<< ERROR" "$SCRIPT_DIR/maven_output.log" | while IFS= read -r line; do
            echo "  $line"
            echo "  $line" >> "$REPORT_FILE"
        done
    fi
    exit 1
fi
