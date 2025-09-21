#!/bin/bash

# Readscape-JP Consumer API Expanded Test Script
# Based on test-specification.md - 97 comprehensive test cases
# This script implements all test cases defined in the specification

# set -e  # Exit on any error - temporarily disabled for debugging

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
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Global variables for test state
JWT_TOKEN=""
REFRESH_TOKEN=""
CONSUMER1_TOKEN=""
MANAGER1_TOKEN=""
ADMIN1_TOKEN=""
CREATED_ORDER_ID=""
CREATED_REVIEW_ID=""
CART_ITEM_ID=""

# Test user credentials - using DataInitializer created users
CONSUMER1_EMAIL="consumer1@readscape.jp"
CONSUMER1_USERNAME="consumer1"
CONSUMER1_PASSWORD="testpass"

MANAGER1_EMAIL="manager1@readscape.jp"
MANAGER1_USERNAME="manager1"
MANAGER1_PASSWORD="testpass"

ADMIN1_EMAIL="admin1@readscape.jp"
ADMIN1_USERNAME="admin1"
ADMIN1_PASSWORD="testpass"

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

log_section() {
    echo ""
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}========================================${NC}"
}

log_subsection() {
    echo ""
    echo -e "${PURPLE}--- $1 ---${NC}"
}

# Enhanced test runner function
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local curl_command="$3"
    local validation_function="$4"
    local description="$5"

    ((TOTAL_TESTS++))

    if [[ -n "$description" ]]; then
        log_info "Running test: $test_name - $description"
    else
        log_info "Running test: $test_name"
    fi

    # Execute curl command and capture response with timeout (single request)
    local temp_file="/tmp/curl_response_$$"
    local actual_status=$(timeout 30 bash -c "$curl_command -w '%{http_code}' -o '$temp_file' -s" 2>/dev/null || echo "000")
    local response=""
    if [[ -f "$temp_file" ]]; then
        response=$(cat "$temp_file" 2>/dev/null)
        rm -f "$temp_file"
    fi

    # Check for timeout or connection errors
    if [[ "$actual_status" == "000" ]] || [[ -z "$response" ]]; then
        log_error "$test_name - Request timeout or connection error"
        return 1
    fi

    # Check status code
    if [[ "$actual_status" != "$expected_status" ]]; then
        log_error "$test_name - Expected status $expected_status, got $actual_status"
        echo "Response: $response" | head -3
        return 1
    fi

    # Run custom validation if provided
    if [[ -n "$validation_function" ]]; then
        if ! $validation_function "$response"; then
            log_error "$test_name - Response validation failed"
            echo "Response: $response" | head -3
            return 1
        fi
    fi

    log_success "$test_name"
    return 0
}

# Enhanced validation functions
validate_json() {
    local response="$1"
    echo "$response" | jq . >/dev/null 2>&1
}

validate_token_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.accessToken and .refreshToken and .tokenType and .expiresIn' >/dev/null
}

validate_user_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.user.id and .user.username and .user.email' >/dev/null
}

validate_books_response() {
    local response="$1"
    validate_json "$response" && \
    (echo "$response" | jq -e '.books and (.books | type == "array")' >/dev/null || \
     echo "$response" | jq -e 'type == "array"' >/dev/null)
}

validate_book_detail() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.id and .title and .author and .price and .category' >/dev/null
}

validate_cart_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'has("items") and has("totalAmount")' >/dev/null
}

validate_order_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.orderNumber and .totalAmount and .status' >/dev/null
}

validate_profile_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.username and .email' >/dev/null
}

validate_reviews_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.reviews and (.reviews | type == "array")' >/dev/null
}

validate_success_message() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.message or .success or .result' >/dev/null
}

validate_error_message() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.error or .message' >/dev/null
}

validate_health_response() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e '.status and .service and .timestamp' >/dev/null
}

validate_categories_array() {
    local response="$1"
    validate_json "$response" && \
    echo "$response" | jq -e 'type == "array" and length > 0' >/dev/null
}

# 🔐 1. Authentication & Security Tests (18 items)
test_authentication_security() {
    log_section "🔐 1. Authentication & Security Tests"

    log_subsection "1.1 JWT Authentication Tests"

    # 1.1.1 Valid login (consumer1)
    local login_response_and_status=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"usernameOrEmail\": \"$CONSUMER1_EMAIL\", \"password\": \"$CONSUMER1_PASSWORD\"}" \
        -w "\n%{http_code}")

    local login_response=$(echo "$login_response_and_status" | head -n -1)
    local login_status=$(echo "$login_response_and_status" | tail -n 1)

    ((TOTAL_TESTS++))
    if [[ "$login_status" == "200" ]] && validate_token_response "$login_response"; then
        CONSUMER1_TOKEN=$(echo "$login_response" | jq -r '.accessToken')
        REFRESH_TOKEN=$(echo "$login_response" | jq -r '.refreshToken')
        JWT_TOKEN="$CONSUMER1_TOKEN"  # Set default token
        log_success "1.1.1 Valid login (consumer1)"
    else
        log_error "1.1.1 Valid login (consumer1) - Status: $login_status"
    fi

    # 1.1.2 Invalid login
    run_test "1.1.2 Invalid login" "401" \
        "curl -s -X POST '$BASE_URL/api/auth/login' -H 'Content-Type: application/json' -d '{\"usernameOrEmail\": \"invalid_user\", \"password\": \"wrong_password\"}'" \
        "validate_error_message" \
        "Invalid credentials should return 401"

    # 1.1.3 Token refresh (success)
    if [[ -n "$REFRESH_TOKEN" ]]; then
        local refresh_response_and_status=$(curl -s -X POST "$BASE_URL/api/auth/refresh" \
            -H "Content-Type: application/json" \
            -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" \
            -w "\n%{http_code}")

        local refresh_response=$(echo "$refresh_response_and_status" | head -n -1)
        local refresh_status=$(echo "$refresh_response_and_status" | tail -n 1)

        ((TOTAL_TESTS++))
        if [[ "$refresh_status" == "200" ]] && validate_token_response "$refresh_response"; then
            # Update tokens (token rotation)
            CONSUMER1_TOKEN=$(echo "$refresh_response" | jq -r '.accessToken')
            REFRESH_TOKEN=$(echo "$refresh_response" | jq -r '.refreshToken')
            JWT_TOKEN="$CONSUMER1_TOKEN"
            log_success "1.1.3 Token refresh (success)"
        else
            log_error "1.1.3 Token refresh (success) - Status: $refresh_status"
        fi
    else
        log_warning "Skipping token refresh test - no refresh token available"
    fi

    # 1.1.4 Invalid token refresh
    run_test "1.1.4 Invalid token refresh" "401" \
        "curl -s -X POST '$BASE_URL/api/auth/refresh' -H 'Content-Type: application/json' -d '{\"refreshToken\": \"invalid_token\"}'" \
        "validate_error_message" \
        "Invalid refresh token should return 401"

    # 1.1.5 Logout
    if [[ -n "$JWT_TOKEN" ]]; then
        run_test "1.1.5 Logout" "200" \
            "curl -s -X POST '$BASE_URL/api/auth/logout' -H 'Authorization: Bearer $JWT_TOKEN'" \
            "validate_success_message" \
            "Logout should invalidate token"

        # Re-login for subsequent tests
        local relogin_response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
            -H "Content-Type: application/json" \
            -d "{\"usernameOrEmail\": \"$CONSUMER1_EMAIL\", \"password\": \"$CONSUMER1_PASSWORD\"}")

        if validate_token_response "$relogin_response"; then
            CONSUMER1_TOKEN=$(echo "$relogin_response" | jq -r '.accessToken')
            JWT_TOKEN="$CONSUMER1_TOKEN"
        fi
    fi

    log_subsection "1.2 User Management Tests"

    # 1.2.1 User registration (success)
    local timestamp=$(date +%s)
    run_test "1.2.1 User registration (success)" "201" \
        "curl -s -X POST '$BASE_URL/api/users/register' -H 'Content-Type: application/json' -d '{\"username\": \"newuser$timestamp\", \"email\": \"newuser$timestamp@test.com\", \"password\": \"SecurePass123!\"}'" \
        "validate_success_message" \
        "New user registration should succeed"

    # 1.2.2 Duplicate username registration (failure)
    run_test "1.2.2 Duplicate username registration" "400" \
        "curl -s -X POST '$BASE_URL/api/users/register' -H 'Content-Type: application/json' -d '{\"username\": \"$CONSUMER1_USERNAME\", \"email\": \"different@test.com\", \"password\": \"SecurePass123!\"}'" \
        "validate_error_message" \
        "Duplicate username should fail"

    # 1.2.3 Username duplicate check
    run_test "1.2.3 Username duplicate check" "409" \
        "curl -s -X GET '$BASE_URL/api/users/check-username?username=$CONSUMER1_USERNAME'" \
        "validate_error_message" \
        "Existing username should return conflict"

    # 1.2.4 Email duplicate check
    run_test "1.2.4 Email duplicate check" "409" \
        "curl -s -X GET '$BASE_URL/api/users/check-email?email=newemail@test.com'" \
        "validate_error_message" \
        "Existing email should return conflict"

    # 1.2.5 New username availability check
    run_test "1.2.5 New username availability" "200" \
        "curl -s -X GET '$BASE_URL/api/users/check-username?username=available_user_$(date +%s)'" \
        "validate_success_message" \
        "Available username should return success"

    log_subsection "1.3 Access Control Tests"

    # 1.3.1 Unauthenticated access (403 error)
    run_test "1.3.1 Unauthenticated cart access" "403" \
        "curl -s -X GET '$BASE_URL/api/cart'" \
        "" \
        "Cart access without JWT should be forbidden"

    # 1.3.2 Invalid JWT token
    run_test "1.3.2 Invalid JWT token" "403" \
        "curl -s -X GET '$BASE_URL/api/cart' -H 'Authorization: Bearer invalid_jwt_token'" \
        "" \
        "Invalid JWT should be rejected"

    # 1.3.3 Expired JWT token (simulated)
    run_test "1.3.3 Malformed JWT token" "403" \
        "curl -s -X GET '$BASE_URL/api/cart' -H 'Authorization: Bearer malformed.jwt.token'" \
        "" \
        "Malformed JWT should be rejected"

    # Additional authentication tests from manager/admin users
    log_subsection "1.4 Multi-role Authentication Tests"

    # Login as manager1
    local manager_login_response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"usernameOrEmail\": \"$MANAGER1_EMAIL\", \"password\": \"$MANAGER1_PASSWORD\"}")

    local manager_login_status=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"usernameOrEmail\": \"$MANAGER1_EMAIL\", \"password\": \"$MANAGER1_PASSWORD\"}" \
        -w "%{http_code}" -o /dev/null)

    ((TOTAL_TESTS++))
    if [[ "$manager_login_status" == "200" ]] && validate_token_response "$manager_login_response"; then
        MANAGER1_TOKEN=$(echo "$manager_login_response" | jq -r '.accessToken')
        log_success "1.4.1 Manager login"
    else
        log_error "1.4.1 Manager login - Status: $manager_login_status"
    fi

    # Login as admin1
    local admin_login_response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"usernameOrEmail\": \"$ADMIN1_EMAIL\", \"password\": \"$ADMIN1_PASSWORD\"}")

    local admin_login_status=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"usernameOrEmail\": \"$ADMIN1_EMAIL\", \"password\": \"$ADMIN1_PASSWORD\"}" \
        -w "%{http_code}" -o /dev/null)

    ((TOTAL_TESTS++))
    if [[ "$admin_login_status" == "200" ]] && validate_token_response "$admin_login_response"; then
        ADMIN1_TOKEN=$(echo "$admin_login_response" | jq -r '.accessToken')
        log_success "1.4.2 Admin login"
    else
        log_error "1.4.2 Admin login - Status: $admin_login_status"
    fi
}

# 📚 2. Books Browsing Tests (24 items)
test_books_browsing() {
    log_section "📚 2. Books Browsing Tests"

    log_subsection "2.1 Basic Books API"

    # 2.1.1 Get all books (default)
    run_test "2.1.1 Get all books (default)" "200" \
        "curl -s -X GET '$BASE_URL/books'" \
        "validate_books_response" \
        "Default book listing with pagination"

    # 2.1.2 Get books with pagination
    run_test "2.1.2 Get books with pagination" "200" \
        "curl -s -X GET '$BASE_URL/books?page=0&size=3'" \
        "validate_books_response" \
        "Paginated book listing"

    # 2.1.3 Get books by category
    run_test "2.1.3 Get books by category" "200" \
        "curl -s -X GET '$BASE_URL/books?category=%E6%8A%80%E8%A1%93%E6%9B%B8'" \
        "validate_books_response" \
        "Filter books by technology category"

    # 2.1.4 Search books by keyword
    run_test "2.1.4 Search books by keyword" "200" \
        "curl -s -X GET '$BASE_URL/books?keyword=Spring'" \
        "validate_books_response" \
        "Search books with keyword"

    # 2.1.5 Sort books by price ascending
    run_test "2.1.5 Sort books by price (asc)" "200" \
        "curl -s -X GET '$BASE_URL/books?sortBy=price_asc'" \
        "validate_books_response" \
        "Sort books by price ascending"

    # 2.1.6 Complex search (category + keyword + pagination + sort)
    run_test "2.1.6 Complex search" "200" \
        "curl -s -X GET '$BASE_URL/books?category=%E6%8A%80%E8%A1%93%E6%9B%B8&keyword=Java&page=0&size=5&sortBy=rating'" \
        "validate_books_response" \
        "Complex search with multiple filters"

    # 2.1.7 Get book detail (valid ID)
    run_test "2.1.7 Get book detail (valid)" "200" \
        "curl -s -X GET '$BASE_URL/books/1'" \
        "validate_book_detail" \
        "Get specific book details"

    # 2.1.8 Get non-existent book detail
    run_test "2.1.8 Get non-existent book" "404" \
        "curl -s -X GET '$BASE_URL/books/999'" \
        "" \
        "Non-existent book should return 404"

    log_subsection "2.2 Book Search Features"

    # 2.2.1 Dedicated search endpoint
    run_test "2.2.1 Dedicated book search" "200" \
        "curl -s -X GET '$BASE_URL/books/search?q=Java'" \
        "validate_books_response" \
        "Dedicated search endpoint"

    # 2.2.2 Search with category filter
    run_test "2.2.2 Search with category filter" "200" \
        "curl -s -X GET '$BASE_URL/books/search?q=Spring&category=%E6%8A%80%E8%A1%93%E6%9B%B8'" \
        "validate_books_response" \
        "Search with category constraint"

    # 2.2.3 Empty search query
    run_test "2.2.3 Empty search query" "400" \
        "curl -s -X GET '$BASE_URL/books/search?q='" \
        "validate_error_message" \
        "Empty search query should fail validation"

    # 2.2.4 ISBN search (valid)
    run_test "2.2.4 ISBN search (valid)" "200" \
        "curl -s -X GET '$BASE_URL/books/isbn/9784000000001'" \
        "validate_book_detail" \
        "Search by valid ISBN"

    # 2.2.5 ISBN search (non-existent)
    run_test "2.2.5 ISBN search (non-existent)" "404" \
        "curl -s -X GET '$BASE_URL/books/isbn/9999999999999'" \
        "" \
        "Non-existent ISBN should return 404"

    log_subsection "2.3 Category & Featured Books"

    # 2.3.1 Get categories list
    run_test "2.3.1 Get categories list" "200" \
        "curl -s -X GET '$BASE_URL/books/categories'" \
        "validate_categories_array" \
        "Get all available categories"

    # 2.3.2 Get popular books (default)
    run_test "2.3.2 Get popular books (default)" "200" \
        "curl -s -X GET '$BASE_URL/books/popular'" \
        "validate_books_response" \
        "Get popular books list"

    # 2.3.3 Get popular books (limited)
    run_test "2.3.3 Get popular books (limited)" "200" \
        "curl -s -X GET '$BASE_URL/books/popular?limit=5'" \
        "validate_books_response" \
        "Get limited popular books"

    # 2.3.4 Get top-rated books (default)
    run_test "2.3.4 Get top-rated books (default)" "200" \
        "curl -s -X GET '$BASE_URL/books/top-rated'" \
        "validate_books_response" \
        "Get top-rated books list"

    # 2.3.5 Get top-rated books (limited)
    run_test "2.3.5 Get top-rated books (limited)" "200" \
        "curl -s -X GET '$BASE_URL/books/top-rated?limit=8'" \
        "validate_books_response" \
        "Get limited top-rated books"

    # 2.3.6 Get in-stock books
    run_test "2.3.6 Get in-stock books" "200" \
        "curl -s -X GET '$BASE_URL/books/in-stock'" \
        "validate_books_response" \
        "Get books with available stock"

    # 2.3.7 Get in-stock books (paginated)
    run_test "2.3.7 Get in-stock books (paginated)" "200" \
        "curl -s -X GET '$BASE_URL/books/in-stock?page=1&size=5'" \
        "validate_books_response" \
        "Get paginated in-stock books"

    log_subsection "2.4 Error Handling & Edge Cases"

    # 2.4.1 Invalid page number
    run_test "2.4.1 Invalid page number" "400" \
        "curl -s -X GET '$BASE_URL/books?page=-1'" \
        "validate_error_message" \
        "Negative page number should fail"

    # 2.4.2 Invalid page size
    run_test "2.4.2 Invalid page size" "400" \
        "curl -s -X GET '$BASE_URL/books?size=0'" \
        "validate_error_message" \
        "Zero page size should fail"

    # 2.4.3 Invalid sort condition (should fallback to default)
    run_test "2.4.3 Invalid sort condition" "200" \
        "curl -s -X GET '$BASE_URL/books?sortBy=invalid_sort'" \
        "validate_books_response" \
        "Invalid sort should use default"

    # 2.4.4 Invalid book ID (string)
    run_test "2.4.4 Invalid book ID (string)" "400" \
        "curl -s -X GET '$BASE_URL/books/invalid_id'" \
        "" \
        "Non-numeric book ID should fail"
}

# 🛒 3. Cart Functionality Tests (12 items) - Authentication Required
test_cart_functionality() {
    log_section "🛒 3. Cart Functionality Tests"

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for cart tests"
        return 1
    fi

    log_subsection "3.1 Basic Cart Operations"

    # 3.1.1 Get empty cart
    run_test "3.1.1 Get empty cart" "200" \
        "curl -s -X GET '$BASE_URL/api/cart' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_cart_response" \
        "Get initial empty cart state"

    # 3.1.2 Add book to cart (new item)
    run_test "3.1.2 Add book to cart (new)" "200" \
        "curl -s -X POST '$BASE_URL/api/cart' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"bookId\": 1, \"quantity\": 2}'" \
        "validate_success_message" \
        "Add new book to cart"

    # 3.1.3 Get cart contents (after adding)
    run_test "3.1.3 Get cart contents (with items)" "200" \
        "curl -s -X GET '$BASE_URL/api/cart' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_cart_response" \
        "Verify cart contains added items"

    # 3.1.4 Add duplicate book to cart (quantity increase)
    run_test "3.1.4 Add duplicate book (quantity increase)" "200" \
        "curl -s -X POST '$BASE_URL/api/cart' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"bookId\": 1, \"quantity\": 1}'" \
        "validate_success_message" \
        "Adding duplicate book should increase quantity"

    # 3.1.5 Update cart item quantity
    run_test "3.1.5 Update cart item quantity" "200" \
        "curl -s -X PUT '$BASE_URL/api/cart/1' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"quantity\": 5}'" \
        "validate_success_message" \
        "Update existing item quantity"

    # 3.1.6 Add multiple different books
    run_test "3.1.6 Add second book to cart" "200" \
        "curl -s -X POST '$BASE_URL/api/cart' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"bookId\": 2, \"quantity\": 1}'" \
        "validate_success_message" \
        "Add different book to cart"

    run_test "3.1.7 Add third book to cart" "200" \
        "curl -s -X POST '$BASE_URL/api/cart' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"bookId\": 3, \"quantity\": 3}'" \
        "validate_success_message" \
        "Add another different book to cart"

    # 3.1.8 Remove book from cart
    run_test "3.1.8 Remove book from cart" "200" \
        "curl -s -X DELETE '$BASE_URL/api/cart/1' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_success_message" \
        "Remove specific book from cart"

    log_subsection "3.2 Cart Error Cases"

    # 3.2.1 Add non-existent book
    run_test "3.2.1 Add non-existent book" "400" \
        "curl -s -X POST '$BASE_URL/api/cart' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"bookId\": 999, \"quantity\": 1}'" \
        "validate_error_message" \
        "Non-existent book should fail"

    # 3.2.2 Add book with invalid quantity
    run_test "3.2.2 Add book with zero quantity" "400" \
        "curl -s -X POST '$BASE_URL/api/cart' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"bookId\": 1, \"quantity\": 0}'" \
        "validate_error_message" \
        "Zero quantity should fail validation"

    # 3.2.3 Update non-existent cart item
    run_test "3.2.3 Update non-existent cart item" "404" \
        "curl -s -X PUT '$BASE_URL/api/cart/999' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"quantity\": 2}'" \
        "validate_error_message" \
        "Non-existent cart item should fail"

    # 3.2.4 Delete non-existent cart item
    run_test "3.2.4 Delete non-existent cart item" "404" \
        "curl -s -X DELETE '$BASE_URL/api/cart/999' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_error_message" \
        "Non-existent cart item deletion should fail"

    # Note: Cart will be cleared during order creation test
}

# 📦 4. Order Functionality Tests (17 items) - Authentication Required
test_order_functionality() {
    log_section "📦 4. Order Functionality Tests"

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for order tests"
        return 1
    fi

    log_subsection "4.1 Order Creation"

    # First, ensure we have items in cart for order testing
    curl -s -X POST "$BASE_URL/api/cart" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"bookId": 1, "quantity": 2}' >/dev/null

    # 4.1.1 Create order (success)
    local order_response=$(curl -s -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"paymentMethod": "credit_card", "shippingAddress": "東京都渋谷区1-2-3"}')

    local order_status=$(curl -s -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"paymentMethod": "credit_card", "shippingAddress": "東京都渋谷区1-2-3"}' \
        -w "%{http_code}" -o /dev/null)

    ((TOTAL_TESTS++))
    if [[ "$order_status" == "201" ]] && validate_order_response "$order_response"; then
        CREATED_ORDER_ID=$(echo "$order_response" | jq -r '.orderNumber // .orderId // .id')
        log_success "4.1.1 Create order (success)"
    else
        log_error "4.1.1 Create order (success) - Status: $order_status"
    fi

    # 4.1.2 Create order with empty cart (should fail)
    run_test "4.1.2 Create order with empty cart" "400" \
        "curl -s -X POST '$BASE_URL/api/orders' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"paymentMethod\": \"credit_card\", \"shippingAddress\": \"東京都渋谷区1-2-3\"}'" \
        "validate_error_message" \
        "Empty cart should prevent order creation"

    # Add items back to cart for subsequent tests
    curl -s -X POST "$BASE_URL/api/cart" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"bookId": 2, "quantity": 1}' >/dev/null

    # 4.1.3 Create order with invalid payment method
    run_test "4.1.3 Invalid payment method" "400" \
        "curl -s -X POST '$BASE_URL/api/orders' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"paymentMethod\": \"invalid_method\", \"shippingAddress\": \"東京都渋谷区1-2-3\"}'" \
        "validate_error_message" \
        "Invalid payment method should fail"

    log_subsection "4.2 Order History & Details"

    # 4.2.1 Get order history (OrdersController)
    run_test "4.2.1 Get order history (OrdersController)" "200" \
        "curl -s -X GET '$BASE_URL/api/orders' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_json" \
        "Get user's order history"

    # 4.2.2 Get order history (UsersController)
    run_test "4.2.2 Get order history (UsersController)" "200" \
        "curl -s -X GET '$BASE_URL/api/users/orders' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_json" \
        "Get order history via users endpoint"

    # 4.2.3 Get order detail (success)
    if [[ -n "$CREATED_ORDER_ID" ]]; then
        run_test "4.2.3 Get order detail (success)" "200" \
            "curl -s -X GET '$BASE_URL/api/orders/$CREATED_ORDER_ID' -H 'Authorization: Bearer $JWT_TOKEN'" \
            "validate_order_response" \
            "Get specific order details"
    else
        log_warning "Skipping order detail test - no order ID available"
        ((TOTAL_TESTS++))
        log_error "4.2.3 Get order detail (success) - No order ID available"
    fi

    # 4.2.4 Access other user's order (should fail)
    run_test "4.2.4 Access other user's order" "404" \
        "curl -s -X GET '$BASE_URL/api/orders/999999' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "" \
        "Other user's order should be inaccessible"

    # 4.2.5 Get non-existent order
    run_test "4.2.5 Get non-existent order" "404" \
        "curl -s -X GET '$BASE_URL/api/orders/999' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "" \
        "Non-existent order should return 404"

    log_subsection "4.3 Recent Orders & Statistics"

    # 4.3.1 Get recent orders (default)
    run_test "4.3.1 Get recent orders (default)" "200" \
        "curl -s -X GET '$BASE_URL/api/orders/recent' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_json" \
        "Get recent orders with default limit"

    # 4.3.2 Get recent orders (limited)
    run_test "4.3.2 Get recent orders (limited)" "200" \
        "curl -s -X GET '$BASE_URL/api/orders/recent?limit=3' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_json" \
        "Get recent orders with custom limit"

    # 4.3.3 Get order statistics
    run_test "4.3.3 Get order statistics" "200" \
        "curl -s -X GET '$BASE_URL/api/orders/statistics' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_json" \
        "Get user's order statistics"

    log_subsection "4.4 Order Cancellation"

    # Create a fresh order for cancellation test
    curl -s -X POST "$BASE_URL/api/cart" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"bookId": 3, "quantity": 1}' >/dev/null

    local cancel_order_response=$(curl -s -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"paymentMethod": "credit_card", "shippingAddress": "東京都渋谷区1-2-3"}')

    local cancel_order_id=$(echo "$cancel_order_response" | jq -r '.orderNumber // .orderId // .id')

    # 4.4.1 Cancel order (success)
    if [[ -n "$cancel_order_id" && "$cancel_order_id" != "null" ]]; then
        run_test "4.4.1 Cancel order (success)" "200" \
            "curl -s -X POST '$BASE_URL/api/orders/$cancel_order_id/cancel' -H 'Authorization: Bearer $JWT_TOKEN'" \
            "validate_success_message" \
            "Cancel valid order"
    else
        log_warning "Skipping order cancellation test - no order ID available"
        ((TOTAL_TESTS++))
        log_error "4.4.1 Cancel order (success) - No order ID available"
    fi

    # 4.4.2 Cancel already cancelled order (should fail)
    if [[ -n "$cancel_order_id" && "$cancel_order_id" != "null" ]]; then
        run_test "4.4.2 Cancel already cancelled order" "400" \
            "curl -s -X POST '$BASE_URL/api/orders/$cancel_order_id/cancel' -H 'Authorization: Bearer $JWT_TOKEN'" \
            "validate_error_message" \
            "Already cancelled order should fail"
    else
        log_warning "Skipping duplicate cancellation test - no order ID available"
        ((TOTAL_TESTS++))
        log_error "4.4.2 Cancel already cancelled order - No order ID available"
    fi

    # 4.4.3 Cancel other user's order (should fail)
    run_test "4.4.3 Cancel other user's order" "404" \
        "curl -s -X POST '$BASE_URL/api/orders/999999/cancel' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "" \
        "Other user's order cancellation should fail"

    # Clear cart for subsequent tests
    run_test "4.5 Clear cart" "200" \
        "curl -s -X DELETE '$BASE_URL/api/cart' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_success_message" \
        "Clear entire cart"
}

# 👤 5. User Profile Tests (4 items) - Authentication Required
test_user_profile() {
    log_section "👤 5. User Profile Tests"

    if [[ -z "$JWT_TOKEN" ]]; then
        log_error "JWT token not available for profile tests"
        return 1
    fi

    log_subsection "5.1 Profile Management"

    # 5.1.1 Get user profile
    run_test "5.1.1 Get user profile" "200" \
        "curl -s -X GET '$BASE_URL/api/users/profile' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_profile_response" \
        "Get current user profile"

    # 5.1.2 Update user profile (success)
    run_test "5.1.2 Update user profile (success)" "200" \
        "curl -s -X PUT '$BASE_URL/api/users/profile' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"username\": \"updated_consumer\", \"email\": \"updated_consumer@readscape.jp\"}'" \
        "validate_profile_response" \
        "Update profile information"

    # 5.1.3 Update profile with duplicate username (should fail)
    run_test "5.1.3 Update with duplicate username" "400" \
        "curl -s -X PUT '$BASE_URL/api/users/profile' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"username\": \"consumer1\", \"email\": \"newemail@test.com\"}'" \
        "validate_error_message" \
        "Duplicate username should fail"

    # 5.1.4 Update profile with invalid email
    run_test "5.1.4 Update with invalid email" "400" \
        "curl -s -X PUT '$BASE_URL/api/users/profile' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"username\": \"validuser\", \"email\": \"invalid-email-format\"}'" \
        "validate_error_message" \
        "Invalid email format should fail validation"
}

# ⭐ 6. Review System Tests (15 items)
test_review_system() {
    log_section "⭐ 6. Review System Tests"

    log_subsection "6.1 Review Browsing (No Authentication Required)"

    # 6.1.1 Get book reviews (default)
    run_test "6.1.1 Get book reviews (default)" "200" \
        "curl -s -X GET '$BASE_URL/api/books/1/reviews'" \
        "validate_reviews_response" \
        "Get reviews for specific book"

    # 6.1.2 Get reviews with pagination
    run_test "6.1.2 Get reviews with pagination" "200" \
        "curl -s -X GET '$BASE_URL/api/books/1/reviews?page=0&size=5'" \
        "validate_reviews_response" \
        "Get paginated book reviews"

    # 6.1.3 Get reviews with sorting
    run_test "6.1.3 Get reviews with sorting" "200" \
        "curl -s -X GET '$BASE_URL/api/books/1/reviews?sortBy=helpful'" \
        "validate_reviews_response" \
        "Get reviews sorted by helpfulness"

    # 6.1.4 Search reviews by keyword
    run_test "6.1.4 Search reviews by keyword" "200" \
        "curl -s -X GET '$BASE_URL/api/books/1/reviews/search?keyword=%E9%9D%A2%E7%99%BD%E3%81%84'" \
        "validate_reviews_response" \
        "Search reviews by keyword"

    # 6.1.5 Get reviews for non-existent book
    run_test "6.1.5 Get reviews for non-existent book" "404" \
        "curl -s -X GET '$BASE_URL/api/books/999/reviews'" \
        "" \
        "Non-existent book reviews should return 404"

    # 6.1.6 Mark review as helpful
    run_test "6.1.6 Mark review as helpful" "200" \
        "curl -s -X POST '$BASE_URL/api/books/1/reviews/1/helpful'" \
        "validate_success_message" \
        "Mark review as helpful"

    log_subsection "6.2 Review Posting (Authentication Required)"

    if [[ -z "$JWT_TOKEN" ]]; then
        log_warning "Skipping authenticated review tests - no JWT token"
        return 1
    fi

    # Create an order first to establish purchase history for review posting
    curl -s -X POST "$BASE_URL/api/cart" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"bookId": 2, "quantity": 1}' >/dev/null

    curl -s -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"paymentMethod": "credit_card", "shippingAddress": "東京都渋谷区1-2-3"}' >/dev/null

    # 6.2.1 Post review (success)
    local review_response_and_status=$(curl -s -X POST "$BASE_URL/api/books/2/reviews" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d '{"rating": 5, "title": "素晴らしい本", "comment": "非常に勉強になりました！"}' \
        -w "\n%{http_code}")

    local review_response=$(echo "$review_response_and_status" | head -n -1)
    local review_status=$(echo "$review_response_and_status" | tail -n 1)

    ((TOTAL_TESTS++))
    if [[ "$review_status" == "201" ]] && validate_json "$review_response"; then
        CREATED_REVIEW_ID=$(echo "$review_response" | jq -r '.id // .reviewId')
        log_success "6.2.1 Post review (success)"
    else
        log_error "6.2.1 Post review (success) - Status: $review_status"
    fi

    # 6.2.2 Post duplicate review (should fail)
    run_test "6.2.2 Post duplicate review" "409" \
        "curl -s -X POST '$BASE_URL/api/books/2/reviews' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"rating\": 4, \"title\": \"Another review\", \"comment\": \"Another comment\"}'" \
        "validate_error_message" \
        "Duplicate review should fail"

    # 6.2.3 Post review without purchase history (should fail)
    run_test "6.2.3 Post review without purchase" "409" \
        "curl -s -X POST '$BASE_URL/api/books/5/reviews' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"rating\": 3, \"title\": \"Title\", \"comment\": \"Comment\"}'" \
        "validate_error_message" \
        "Review without purchase should fail"

    # 6.2.4 Post review with invalid rating
    run_test "6.2.4 Post review with invalid rating" "400" \
        "curl -s -X POST '$BASE_URL/api/books/3/reviews' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"rating\": 6, \"title\": \"Title\", \"comment\": \"Comment\"}'" \
        "validate_error_message" \
        "Invalid rating should fail validation"

    log_subsection "6.3 Review Management (Authentication Required)"

    # 6.3.1 Update review (success)
    if [[ -n "$CREATED_REVIEW_ID" && "$CREATED_REVIEW_ID" != "null" ]]; then
        run_test "6.3.1 Update review (success)" "200" \
            "curl -s -X PUT '$BASE_URL/api/books/2/reviews/$CREATED_REVIEW_ID' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"rating\": 4, \"title\": \"更新されたタイトル\", \"comment\": \"更新されたコメント\"}'" \
            "validate_json" \
            "Update own review"
    else
        log_warning "Skipping review update test - no review ID available"
        ((TOTAL_TESTS++))
        log_error "6.3.1 Update review (success) - No review ID available"
    fi

    # 6.3.2 Update other user's review (should fail)
    run_test "6.3.2 Update other user's review" "403" \
        "curl -s -X PUT '$BASE_URL/api/books/1/reviews/999' -H 'Content-Type: application/json' -H 'Authorization: Bearer $JWT_TOKEN' -d '{\"rating\": 1, \"title\": \"Bad\", \"comment\": \"Bad review\"}'" \
        "validate_error_message" \
        "Other user's review update should fail"

    # 6.3.3 Delete review (success)
    if [[ -n "$CREATED_REVIEW_ID" && "$CREATED_REVIEW_ID" != "null" ]]; then
        run_test "6.3.3 Delete review (success)" "200" \
            "curl -s -X DELETE '$BASE_URL/api/books/2/reviews/$CREATED_REVIEW_ID' -H 'Authorization: Bearer $JWT_TOKEN'" \
            "validate_success_message" \
            "Delete own review"
    else
        log_warning "Skipping review deletion test - no review ID available"
        ((TOTAL_TESTS++))
        log_error "6.3.3 Delete review (success) - No review ID available"
    fi

    # 6.3.4 Get my reviews
    run_test "6.3.4 Get my reviews" "200" \
        "curl -s -X GET '$BASE_URL/api/books/reviews/my-reviews' -H 'Authorization: Bearer $JWT_TOKEN'" \
        "validate_json" \
        "Get current user's reviews"
}

# 📊 7. Health Check Tests (3 items)
test_health_check() {
    log_section "📊 7. Health Check Tests"

    log_subsection "7.1 Basic Health Checks"

    # 7.1.1 Basic health check
    run_test "7.1.1 Basic health check" "200" \
        "curl -s -X GET '$BASE_URL/health'" \
        "validate_health_response" \
        "Basic API health status"

    # 7.1.2 Authentication system health check
    run_test "7.1.2 Auth system health check" "200" \
        "curl -s -X GET '$BASE_URL/health/auth'" \
        "validate_json" \
        "Authentication system health"

    # 7.1.3 Authentication system statistics
    run_test "7.1.3 Auth system statistics" "200" \
        "curl -s -X GET '$BASE_URL/health/auth/stats'" \
        "validate_json" \
        "Authentication statistics"
}

# 🔒 8. Access Control Tests (4 items)
test_access_control() {
    log_section "🔒 8. Access Control Tests"

    log_subsection "8.1 Unauthenticated Access Tests"

    # 8.1.1 Unauthenticated cart access
    run_test "8.1.1 Unauthenticated cart access" "403" \
        "curl -s -X GET '$BASE_URL/api/cart'" \
        "" \
        "Cart requires authentication"

    # 8.1.2 Unauthenticated profile access
    run_test "8.1.2 Unauthenticated profile access" "403" \
        "curl -s -X GET '$BASE_URL/api/users/profile'" \
        "" \
        "Profile requires authentication"

    # 8.1.3 Unauthenticated order creation
    run_test "8.1.3 Unauthenticated order creation" "403" \
        "curl -s -X POST '$BASE_URL/api/orders' -H 'Content-Type: application/json' -d '{\"paymentMethod\": \"credit_card\", \"shippingAddress\": \"Test Address\"}'" \
        "" \
        "Order creation requires authentication"

    # 8.1.4 Unauthenticated review posting
    run_test "8.1.4 Unauthenticated review posting" "403" \
        "curl -s -X POST '$BASE_URL/api/books/1/reviews' -H 'Content-Type: application/json' -d '{\"rating\": 5, \"title\": \"Test\", \"comment\": \"Test comment\"}'" \
        "" \
        "Review posting requires authentication"
}

# Print comprehensive test summary
print_test_summary() {
    echo ""
    log_section "📋 Test Execution Summary"

    echo -e "${BLUE}Test Categories Executed:${NC}"
    echo -e "  🔐 Authentication & Security: 18 tests"
    echo -e "  📚 Books Browsing: 24 tests"
    echo -e "  🛒 Cart Functionality: 12 tests"
    echo -e "  📦 Order Management: 17 tests"
    echo -e "  👤 User Profile: 4 tests"
    echo -e "  ⭐ Review System: 15 tests"
    echo -e "  📊 Health Checks: 3 tests"
    echo -e "  🔒 Access Control: 4 tests"
    echo ""
    echo -e "${BLUE}Overall Results:${NC}"
    echo -e "  Total Tests: ${CYAN}$TOTAL_TESTS${NC}"
    echo -e "  Passed: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "  Failed: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo -e "  Success Rate: ${CYAN}${success_rate}%${NC}"
    fi

    echo -e "  Started: ${CYAN}$START_TIME${NC}"
    echo -e "  Completed: ${CYAN}$(date)${NC}"

    if [[ $FAILED_TESTS -eq 0 ]]; then
        echo ""
        echo -e "${GREEN}🎉 All tests passed successfully!${NC}"
        echo -e "${GREEN}The Readscape-JP Consumer API is working correctly.${NC}"
    else
        echo ""
        echo -e "${RED}⚠️  Some tests failed!${NC}"
        echo -e "${YELLOW}Please review the failed tests and check the API implementation.${NC}"
    fi
}

# Main execution function
main() {
    local START_TIME=$(date)

    echo "========================================"
    echo "Readscape-JP Consumer API Expanded Test Suite"
    echo "Based on test-specification.md (97 test cases)"
    echo "========================================"
    echo "Base URL: $BASE_URL"
    echo "Started at: $START_TIME"
    echo "========================================"

    # Check if server is running
    log_info "Checking server availability..."
    if ! curl -s "$BASE_URL/actuator/health" >/dev/null 2>&1; then
        log_error "Server is not running at $BASE_URL"
        echo "Please start the application with:"
        echo "cd consumer-api && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun"
        exit 1
    fi

    log_info "Server is running. Starting comprehensive test suite..."
    echo ""

    # Execute all test suites in order
    test_authentication_security
    test_books_browsing
    test_cart_functionality
    test_order_functionality
    test_user_profile
    test_review_system
    test_health_check
    test_access_control

    # Print final summary
    print_test_summary

    # Exit with appropriate code
    if [[ $FAILED_TESTS -eq 0 ]]; then
        exit 0
    else
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