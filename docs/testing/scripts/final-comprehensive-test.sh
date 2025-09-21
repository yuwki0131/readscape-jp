#!/bin/bash

# Final Comprehensive API Test Script
BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
JWT_TOKEN=""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; ((PASSED_TESTS++)); }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; ((FAILED_TESTS++)); }

run_test() {
    local test_name="$1"
    local expected_status="$2"
    local url="$3"
    local extra_args="$4"

    ((TOTAL_TESTS++))
    log_info "Testing: $test_name"

    local cmd="curl -s -w %{http_code} -o /tmp/response $extra_args \"$BASE_URL$url\""
    local status=$(eval "$cmd" 2>/dev/null || echo "000")
    local response=$(cat /tmp/response 2>/dev/null || echo "")

    if [[ "$status" == "$expected_status" ]]; then
        log_success "$test_name (Status: $status)"
        return 0
    else
        log_error "$test_name - Expected: $expected_status, Got: $status"
        [[ -n "$response" ]] && echo "Response: $response"
        return 1
    fi
}

test_auth() {
    log_info "=== Authentication Tests ==="

    # Valid login
    local response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usernameOrEmail": "testuser@example.com", "password": "TestPass123"}')

    if echo "$response" | jq -e '.accessToken' >/dev/null 2>&1; then
        JWT_TOKEN=$(echo "$response" | jq -r '.accessToken')
        log_success "Valid login authentication"
        ((PASSED_TESTS++))
    else
        log_error "Valid login authentication"
        echo "Response: $response"
        ((FAILED_TESTS++))
        return 1
    fi

    # Invalid login (password validation triggers 400)
    run_test "Invalid login" "400" "/api/auth/login" \
        "-X POST -H 'Content-Type: application/json' -d '{\"usernameOrEmail\": \"invalid@test.com\", \"password\": \"wrong\"}'"

    ((TOTAL_TESTS++))
}

test_books() {
    log_info "=== Books API Tests ==="

    run_test "Get all books" "200" "/books"
    run_test "Get book detail" "200" "/books/1"
    run_test "Get non-existent book" "404" "/books/999"
    run_test "Search by category" "200" "/books?category=%E6%8A%80%E8%A1%93%E6%9B%B8"
    run_test "Search by keyword" "200" "/books?keyword=Spring"
}

test_protected() {
    log_info "=== Protected Endpoints Tests ==="

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "No JWT token available"
        ((FAILED_TESTS++))
        ((TOTAL_TESTS++))
        return 1
    fi

    run_test "Profile with auth" "200" "/api/users/profile" "-H 'Authorization: Bearer $JWT_TOKEN'"
    run_test "Profile without auth" "403" "/api/users/profile"
}

test_extended_books() {
    log_info "=== Extended Books API Tests ==="

    run_test "Books pagination" "200" "/books?page=0&size=2"
    run_test "Books categories" "200" "/books/categories"
}

test_reviews() {
    log_info "=== Reviews API Tests ==="

    run_test "Get book reviews" "200" "/books/1/reviews"

    if [[ -n "$JWT_TOKEN" ]]; then
        log_info "Testing authenticated review post..."
        local review_response=$(curl -s -X POST "$BASE_URL/books/1/reviews" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -d '{"rating": 5, "comment": "Test review"}')

        ((TOTAL_TESTS++))
        if echo "$review_response" | jq -e '.success // .message' >/dev/null 2>&1; then
            log_success "Post book review"
        else
            log_error "Post book review failed"
        fi
    fi
}

main() {
    echo "========================================"
    echo "Readscape-JP Comprehensive API Test"
    echo "========================================"
    echo "Base URL: $BASE_URL"
    echo "Started at: $(date)"
    echo "========================================"

    # Check server
    if ! curl -s "$BASE_URL/health" >/dev/null 2>&1; then
        log_error "Server not running at $BASE_URL"
        exit 1
    fi

    log_info "Server is running. Starting tests..."
    echo ""

    # Run tests
    test_auth && test_books && test_protected && test_extended_books && test_reviews

    # Summary
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

# Create temp directory for responses
mkdir -p /tmp

# Run main function
main "$@"