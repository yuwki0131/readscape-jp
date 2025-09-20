#!/bin/bash

# Debug version to test individual API calls

BASE_URL="http://localhost:8080"
USER="testuser456"
PASS="MySecure789@"

echo "=== Testing Individual API Calls ==="

echo "1. Testing invalid login..."
response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail": "invalid_user", "password": "wrong_password"}' \
    -w '%{http_code}' -o /tmp/response1)
echo "Status: $response"
echo "Response: $(cat /tmp/response1)"
echo ""

echo "2. Testing books endpoint..."
response=$(curl -s -X GET "$BASE_URL/books" \
    -w '%{http_code}' -o /tmp/response2)
echo "Status: $response"
echo "Response: $(cat /tmp/response2 | head -100)"
echo ""

echo "3. Testing cart without auth..."
response=$(curl -s -X GET "$BASE_URL/api/cart" \
    -w '%{http_code}' -o /tmp/response3)
echo "Status: $response"
echo "Response: $(cat /tmp/response3)"
echo ""

echo "4. Testing user registration..."
timestamp=$(date +%s)
response=$(curl -s -X POST "$BASE_URL/api/users/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"testuser$timestamp\", \"email\": \"testuser$timestamp@test.com\", \"password\": \"TestPass123@\"}" \
    -w '%{http_code}' -o /tmp/response4)
echo "Status: $response"
echo "Response: $(cat /tmp/response4)"
echo ""

echo "=== Debug Complete ==="