#!/bin/bash

# Simplified API Test Script for Readscape-JP Consumer API
# Tests basic functionality without authentication requirements

set -e

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED_TESTS++))
}

test_api() {
    local test_name="$1"
    local url="$2"
    local expected_status="$3"

    ((TOTAL_TESTS++))
    log_info "Testing: $test_name"

    local status=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL$url" 2>/dev/null || echo "000")

    if [[ "$status" == "$expected_status" ]]; then
        log_success "$test_name (Status: $status)"
    else
        log_error "$test_name - Expected: $expected_status, Got: $status"
    fi
}

main() {
    echo "========================================"
    echo "Readscape-JP Simple API Test"
    echo "========================================"
    echo "Base URL: $BASE_URL"
    echo "Started at: $(date)"
    echo "========================================"

    # Check if server is running
    if ! curl -s "$BASE_URL/health" >/dev/null 2>&1; then
        log_error "Server is not running at $BASE_URL"
        exit 1
    fi

    log_info "Server is running. Starting basic tests..."
    echo ""

    # Test health endpoint
    test_api "Health check" "/health" "200"

    # Test books endpoints (should not require authentication)
    test_api "Get all books" "/api/books" "200"
    test_api "Get book by ID" "/api/books/1" "200"
    test_api "Get non-existent book" "/api/books/999" "404"

    # Test authentication endpoints
    test_api "Login endpoint (should fail with bad data)" "/api/auth/login" "400"

    # Test protected endpoints without auth (should return 401)
    test_api "Cart without auth" "/api/cart" "401"
    test_api "Profile without auth" "/api/users/profile" "401"

    echo ""
    echo "========================================"
    echo "Test Results Summary"
    echo "========================================"
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
    echo "Success Rate: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
    echo "Completed at: $(date)"
    echo "========================================"

    if [[ $FAILED_TESTS -eq 0 ]]; then
        echo -e "${GREEN}All tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}Some tests failed!${NC}"
        exit 1
    fi
}

main "$@"