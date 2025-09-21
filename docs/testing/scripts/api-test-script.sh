#!/bin/bash

# Readscape-JP Consumer API Test Script
# This script tests all API endpoints with comprehensive validation

set -e  # Exit on any error

# Configuration
BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Global variables
JWT_TOKEN=""
CREATED_ORDER_ID=""

# Utility functions
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

# Test runner function
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local curl_command="$3"
    local validation_function="$4"

    ((TOTAL_TESTS++))
    log_info "Running test: $test_name"

    # Execute curl command and capture response with timeout
    local response=$(timeout 10 eval "$curl_command" 2>/dev/null || echo "TIMEOUT")
    local actual_status=$(timeout 10 eval "$curl_command -w '%{http_code}' -o /dev/null -s" 2>/dev/null || echo "000")

    # Check for timeout or connection errors
    if [[ "$response" == "TIMEOUT" ]] || [[ "$actual_status" == "000" ]]; then
        log_error "$test_name - Request timeout or connection error"
        return 1
    fi

    # Check status code
    if [[ "$actual_status" != "$expected_status" ]]; then
        log_error "$test_name - Expected status $expected_status, got $actual_status"
        return 1
    fi

    # Run custom validation if provided
    if [[ -n "$validation_function" ]]; then
        if ! $validation_function "$response"; then
            log_error "$test_name - Response validation failed"
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
    validate_json "$response" && echo "$response" | jq -e '.accessToken' >/dev/null
}

validate_books_array() {
    local response="$1"
    validate_json "$response" && echo "$response" | jq -e 'type == "array"' >/dev/null && \
    echo "$response" | jq -e '.[0] | has("id") and has("title") and has("author") and has("price") and has("category")' >/dev/null 2>&1
}

validate_book_detail() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("id") and has("title") and has("author") and has("price") and has("category") and has("description")' >/dev/null
}

validate_cart_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("items") and has("totalPrice")' >/dev/null
}

validate_success_message() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("message")' >/dev/null
}

validate_profile_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("username") and has("email")' >/dev/null
}

validate_orders_array() {
    local response="$1"
    validate_json "$response" && echo "$response" | jq -e 'type == "array"' >/dev/null
}

validate_order_detail() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("orderId") and has("date") and has("totalPrice") and has("status")' >/dev/null
}

validate_reviews_array() {
    local response="$1"
    validate_json "$response" && echo "$response" | jq -e 'type == "array"' >/dev/null
}

# Validation function for JWT token response
validate_token_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("accessToken") and has("refreshToken") and has("tokenType") and has("expiresIn")' >/dev/null
}

# Validation function for cart response
validate_cart_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("cartId") and has("items") and has("totalAmount")' >/dev/null
}

# Validation function for orders array
validate_orders_array() {
    local response="$1"
    validate_json "$response" && echo "$response" | jq -e 'type == "array"' >/dev/null
}

# Validation function for success message
validate_success_message() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("success") or has("message") or has("result")' >/dev/null
}

# Validation function for profile response
validate_profile_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("username") and has("email")' >/dev/null
}

# Test functions
test_user_login() {
    log_info "=== 1. Authentication Tests ==="

    # 1.1 Valid login
    local login_response=$(timeout 10 curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usernameOrEmail": "testuser@example.com", "password": "TestPass123"}' 2>/dev/null || echo "TIMEOUT")

    local login_status=$(timeout 10 curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usernameOrEmail": "testuser@example.com", "password": "TestPass123"}' \
        -w "%{http_code}" -o /dev/null 2>/dev/null || echo "000")

    ((TOTAL_TESTS++))
    if [[ "$login_response" == "TIMEOUT" ]] || [[ "$login_status" == "000" ]]; then
        log_error "1.1 Valid user login - Request timeout or connection error"
        return 1
    elif [[ "$login_status" == "200" ]] && validate_token_response "$login_response"; then
        JWT_TOKEN=$(echo "$login_response" | jq -r '.accessToken')
        log_success "1.1 Valid user login"
    else
        log_error "1.1 Valid user login - Status: $login_status"
        return 1
    fi

    # 1.2 Invalid login
    run_test "1.2 Invalid user login" "400" \
        "curl -s -X POST '$BASE_URL/api/auth/login' -H 'Content-Type: application/json' -d '{\"usernameOrEmail\": \"invalid@test.com\", \"password\": \"wrong\"}'"
}

test_books_api() {
    log_info "=== 2. Books API Tests ==="

    # 2.1 Get all books
    run_test "2.1 Get all books" "200" \
        "curl -s -X GET '$BASE_URL/books'" \
        "validate_books_array"

    # 2.2 Get books by category
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

test_cart_api() {
    log_info "=== 3. Cart API Tests ==="

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for cart tests"
        return 1
    fi

    # 3.1 Get cart contents
    run_test "3.1 Get cart contents" "200" \
        "curl -s -X GET '$BASE_URL/api/carts' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_cart_response"

    # 3.2 Add item to cart
    run_test "3.2 Add item to cart" "200" \
        "curl -s -X POST '$BASE_URL/api/carts/items' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"bookId\": 1, \"quantity\": 2}'" \
        "validate_success_message"

    # 3.3 Update cart item quantity
    run_test "3.3 Update cart item quantity" "200" \
        "curl -s -X PUT '$BASE_URL/api/carts/items/1' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"quantity\": 3}'" \
        "validate_success_message"

    # 3.4 Remove item from cart (save for after order test)
    # This will be tested after order creation
}

test_orders_api() {
    log_info "=== 4. Orders API Tests ==="

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for orders tests"
        return 1
    fi

    # 4.1 Create order
    local order_response=$(curl -s -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"shippingAddress": "123 Main Street, Tokyo", "shippingPhone": "090-1234-5678", "paymentMethod": "CREDIT_CARD", "notes": "Test order"}')

    local order_status=$(curl -s -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"shippingAddress": "123 Main Street, Tokyo", "shippingPhone": "090-1234-5678", "paymentMethod": "CREDIT_CARD", "notes": "Test order"}' \
        -w "%{http_code}" -o /dev/null)

    ((TOTAL_TESTS++))
    if [[ "$order_status" == "200" ]] && validate_json "$order_response"; then
        CREATED_ORDER_ID=$(echo "$order_response" | jq -r '.orderId // empty')
        log_success "4.1 Create order"
    else
        log_error "4.1 Create order - Status: $order_status"
    fi

    # 4.2 Get order history
    run_test "4.2 Get order history" "200" \
        "curl -s -X GET '$BASE_URL/api/orders' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_orders_array"

    # 4.3 Get order detail (if order was created)
    if [[ -n "$CREATED_ORDER_ID" ]]; then
        run_test "4.3 Get order detail" "200" \
            "curl -s -X GET '$BASE_URL/api/orders/$CREATED_ORDER_ID' -H 'Authorization: Bearer $JWT_TOKEN'" \
            "validate_order_detail"
    else
        log_warning "Skipping order detail test - no order ID available"
    fi
}

test_cart_cleanup() {
    log_info "=== 3.4 Cart Cleanup Test ==="

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for cart cleanup"
        return 1
    fi

    # 3.4 Remove item from cart
    run_test "3.4 Remove item from cart" "200" \
        "curl -s -X DELETE '$BASE_URL/api/carts/items/1' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_success_message"
}

test_profile_api() {
    log_info "=== 5. User Profile API Tests ==="

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for profile tests"
        return 1
    fi

    # 5.1 Get profile
    run_test "5.1 Get user profile" "200" \
        "curl -s -X GET '$BASE_URL/api/users/profile' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_profile_response"

    # 5.2 Update profile
    run_test "5.2 Update user profile" "200" \
        "curl -s -X PUT '$BASE_URL/api/users/profile' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"username\": \"updated_consumer1\", \"email\": \"consumer1@readscape.jp\"}'" \
        "validate_success_message"
}

test_reviews_api() {
    log_info "=== 6. Reviews API Tests ==="

    # 6.1 Get reviews for book
    run_test "6.1 Get book reviews" "200" \
        "curl -s -X GET '$BASE_URL/api/books/1/reviews'" \
        "validate_reviews_array"

    # 6.2 Post review (requires authentication)
    if [[ -n "$JWT_TOKEN" ]]; then
        run_test "6.2 Post book review" "200" \
            "curl -s -X POST '$BASE_URL/api/books/1/reviews' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"rating\": 5, \"comment\": \"Amazing book!\"}'" \
            "validate_success_message"
    else
        log_warning "Skipping review post test - no JWT token"
    fi
}

test_authentication_required() {
    log_info "=== 7. Authentication Required Tests ==="

    # 7.1 Access cart without authentication
    run_test "7.1 Unauthenticated cart access" "401" \
        "curl -s -X GET '$BASE_URL/api/carts'"

    # 7.2 Access profile without authentication
    run_test "7.2 Unauthenticated profile access" "401" \
        "curl -s -X GET '$BASE_URL/api/users/profile'"
}

# Main execution
main() {
    echo "========================================"
    echo "Readscape-JP Consumer API Test Suite"
    echo "========================================"
    echo "Base URL: $BASE_URL"
    echo "Started at: $(date)"
    echo "========================================"

    # Check if server is running
    if ! curl -s "$BASE_URL/health" >/dev/null 2>&1; then
        log_error "Server is not running at $BASE_URL"
        echo "Please start the application with: cd consumer-api && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun"
        exit 1
    fi

    log_info "Server is running. Starting tests..."
    echo ""

    # Run all test suites
    test_user_login
    test_books_api
    test_cart_api
    test_orders_api
    test_cart_cleanup
    test_profile_api
    test_reviews_api
    test_authentication_required

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

    # Exit with appropriate code
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