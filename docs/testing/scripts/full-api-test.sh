#!/bin/bash

# Full API Test Script - Comprehensive Testing
set -e

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
JWT_TOKEN=""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
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

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

run_test() {
    local test_name="$1"
    local expected_status="$2"
    local curl_command="$3"
    local validation_function="$4"

    ((TOTAL_TESTS++))
    log_info "Running: $test_name"

    # Execute curl command with timeout
    local response=$(timeout 10 eval "$curl_command" 2>/dev/null || echo "ERROR")
    local actual_status=$(timeout 10 eval "$curl_command -w '%{http_code}' -o /dev/null -s" 2>/dev/null || echo "000")

    if [[ "$response" == "ERROR" ]] || [[ "$actual_status" == "000" ]]; then
        log_error "$test_name - Request timeout or connection error"
        return 1
    fi

    # Check status code
    if [[ "$actual_status" != "$expected_status" ]]; then
        log_error "$test_name - Expected status $expected_status, got $actual_status"
        echo "Response: $response"
        return 1
    fi

    # Run custom validation if provided
    if [[ -n "$validation_function" ]]; then
        if ! $validation_function "$response"; then
            log_error "$test_name - Response validation failed"
            echo "Response: $response"
            return 1
        fi
    fi

    log_success "$test_name"
    return 0
}

# Validation functions
validate_json() {
    local response="$1"
    echo "$response" | jq . >/dev/null 2>&1
}

validate_token_response() {
    local response="$1"
    validate_json "$response" && echo "$response" | jq -e '.accessToken and .refreshToken and .tokenType and .expiresIn' >/dev/null
}

validate_books_array() {
    local response="$1"
    validate_json "$response" && echo "$response" | jq -e '.books and (.books | type == "array") and (.books | length > 0)' >/dev/null
}

validate_book_detail() {
    local response="$1"
    validate_json "$response" && echo "$response" | jq -e 'has("id") and has("title") and has("author") and has("price")' >/dev/null
}

test_authentication() {
    log_info "=== 1. Authentication Tests ==="

    # 1.1 Valid login
    local login_response=$(timeout 10 curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usernameOrEmail": "testuser@example.com", "password": "TestPass123"}' 2>/dev/null || echo "ERROR")

    local login_status=$(timeout 10 curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usernameOrEmail": "testuser@example.com", "password": "TestPass123"}' \
        -w "%{http_code}" -o /dev/null 2>/dev/null || echo "000")

    ((TOTAL_TESTS++))
    if [[ "$login_response" != "ERROR" ]] && [[ "$login_status" == "200" ]] && validate_token_response "$login_response"; then
        JWT_TOKEN=$(echo "$login_response" | jq -r '.accessToken')
        log_success "1.1 Valid user login"
    else
        log_error "1.1 Valid user login - Status: $login_status"
        echo "Response: $login_response"
        return 1
    fi

    # 1.2 Invalid login
    run_test "1.2 Invalid user login" "401" \
        "curl -s -X POST '$BASE_URL/api/auth/login' -H 'Content-Type: application/json' -d '{\"usernameOrEmail\": \"invalid@test.com\", \"password\": \"WrongPass123\"}'"
}

test_books_api() {
    log_info "=== 2. Books API Tests ==="

    # 2.1 Get all books
    run_test "2.1 Get all books" "200" \
        "curl -s -X GET '$BASE_URL/books'" \
        "validate_books_array"

    # 2.2 Get books by category (URL encoded)
    run_test "2.2 Get books by category" "200" \
        "curl -s -X GET '$BASE_URL/books?category=%E6%8A%80%E8%A1%93%E6%9B%B8'" \
        "validate_books_array"

    # 2.3 Search books by keyword
    run_test "2.3 Search books by keyword" "200" \
        "curl -s -X GET '$BASE_URL/books?keyword=Spring'" \
        "validate_books_array"

    # 2.4 Get books with pagination
    run_test "2.4 Get books with pagination" "200" \
        "curl -s -X GET '$BASE_URL/books?page=0&size=3'" \
        "validate_books_array"

    # 2.5 Get book detail
    run_test "2.5 Get book detail" "200" \
        "curl -s -X GET '$BASE_URL/books/1'" \
        "validate_book_detail"

    # 2.6 Get non-existent book
    run_test "2.6 Get non-existent book" "404" \
        "curl -s -X GET '$BASE_URL/books/999'"
}

test_protected_endpoints() {
    log_info "=== 3. Protected Endpoints Tests ==="

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for protected endpoint tests"
        return 1
    fi

    # 3.1 Profile access with authentication
    run_test "3.1 Get user profile (authenticated)" "200" \
        "curl -s -X GET '$BASE_URL/api/users/profile' -H 'Authorization: Bearer $JWT_TOKEN'"

    # 3.2 Profile access without authentication
    run_test "3.2 Get user profile (unauthenticated)" "403" \
        "curl -s -X GET '$BASE_URL/api/users/profile'"
}

main() {
    echo "========================================"
    echo "Readscape-JP Full API Test Suite"
    echo "========================================"
    echo "Base URL: $BASE_URL"
    echo "Started at: $(date)"
    echo "========================================"

    # Check if server is running
    if ! timeout 5 curl -s "$BASE_URL/health" >/dev/null 2>&1; then
        log_error "Server is not running at $BASE_URL"
        exit 1
    fi

    log_info "Server is running. Starting comprehensive tests..."
    echo ""

    # Run test suites
    if test_authentication; then
        test_books_api
        test_protected_endpoints
    else
        log_error "Authentication failed, skipping remaining tests"
    fi

    # Print summary
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

# Check dependencies
if ! command -v curl >/dev/null 2>&1; then
    log_error "curl is required but not installed"
    exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
    log_error "jq is required but not installed"
    exit 1
fi

# Run main function
main "$@"