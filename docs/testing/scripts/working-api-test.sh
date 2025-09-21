#!/bin/bash

# Working API Test Script - 動作確認済み機能のみテスト

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Colors
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
    local headers="$4"

    ((TOTAL_TESTS++))
    log_info "Testing: $test_name"

    local cmd="curl -s -w \"%{http_code}\" -o /dev/null \"$BASE_URL$url\""
    if [[ -n "$headers" ]]; then
        cmd="curl -s -w \"%{http_code}\" -o /dev/null $headers \"$BASE_URL$url\""
    fi

    local status=$(eval "$cmd" 2>/dev/null || echo "000")

    if [[ "$status" == "$expected_status" ]]; then
        log_success "$test_name (Status: $status)"
    else
        log_error "$test_name - Expected: $expected_status, Got: $status"
    fi
}

main() {
    echo "========================================"
    echo "Readscape-JP Working API Test"
    echo "========================================"
    echo "Base URL: $BASE_URL"
    echo "Started at: $(date)"
    echo "========================================"

    # Check if server is running
    if ! curl -s "$BASE_URL/health" >/dev/null 2>&1; then
        log_error "Server is not running at $BASE_URL"
        exit 1
    fi

    log_info "Server is running. Starting tests..."
    echo ""

    # 1. Health check
    test_api "Health check" "/health" "200"

    # 2. Books API (動作確認済み)
    test_api "Get all books" "/books" "200"
    test_api "Get book by ID" "/books/1" "200"
    test_api "Get non-existent book" "/books/999" "404"
    test_api "Get books by category" "/books?category=%E6%8A%80%E8%A1%93%E6%9B%B8" "200"

    # 3. Authentication (動作確認済み)
    log_info "Testing authentication with working credentials..."
    response=$(curl -s -X POST "$BASE_URL/api/auth/login" -H "Content-Type: application/json" -d '{"usernameOrEmail": "testuser@example.com", "password": "TestPass123"}')
    if echo "$response" | jq -e '.accessToken' >/dev/null 2>&1; then
        log_success "User authentication"
        token=$(echo "$response" | jq -r '.accessToken')
    else
        log_error "User authentication failed"
        token=""
    fi

    # 4. Test protected endpoints without auth (should return 401/403)
    test_api "Profile without auth" "/api/users/profile" "403"

    # 5. Test protected endpoints with auth (if token available)
    if [[ -n "$token" ]]; then
        test_api "Profile with auth" "/api/users/profile" "200" "-H \"Authorization: Bearer $token\""
    fi

    # Results summary
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