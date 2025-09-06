#!/bin/bash

echo "=== Readscape システムヘルスチェック ==="

# Consumer API ヘルスチェック
echo "🔍 Consumer API (8080) チェック中..."
if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ Consumer API: 正常"
    curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"'
else
    echo "❌ Consumer API: 異常"
fi

# Inventory Management API ヘルスチェック
echo "🔍 Inventory Management API (8081) チェック中..."
if curl -f -s http://localhost:8081/actuator/health > /dev/null; then
    echo "✅ Inventory Management API: 正常"
    curl -s http://localhost:8081/actuator/health | grep -o '"status":"[^"]*"'
else
    echo "❌ Inventory Management API: 異常"
fi

# PostgreSQL ヘルスチェック
echo "🔍 PostgreSQL (5432) チェック中..."
if nc -z localhost 5432; then
    echo "✅ PostgreSQL: 接続可能"
else
    echo "❌ PostgreSQL: 接続不可"
fi

# Redis ヘルスチェック
echo "🔍 Redis (6379) チェック中..."
if nc -z localhost 6379; then
    echo "✅ Redis: 接続可能"
else
    echo "❌ Redis: 接続不可"
fi

echo ""
echo "=== API エンドポイントテスト ==="

# Consumer API エンドポイントテスト
echo "📚 書籍一覧取得テスト..."
if curl -f -s http://localhost:8080/books > /dev/null; then
    echo "✅ 書籍API: 正常"
else
    echo "❌ 書籍API: 異常"
fi

echo ""
echo "=== Docker コンテナ状態 ==="
docker-compose ps