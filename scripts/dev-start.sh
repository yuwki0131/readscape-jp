#!/bin/bash

# Readscape-JP 開発環境起動スクリプト
set -e

# 色付きメッセージ用の設定
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Readscape-JP 開発環境を起動しています...${NC}"

# 既存のコンテナを停止・削除
echo -e "${YELLOW}📦 既存のコンテナをクリーンアップ中...${NC}"
docker-compose down --remove-orphans

# イメージをビルド
echo -e "${YELLOW}🔨 Dockerイメージをビルド中...${NC}"
docker-compose build

# サービスを起動
echo -e "${YELLOW}🏃 サービスを起動中...${NC}"
docker-compose up -d

# PostgreSQLの起動待ち
echo -e "${YELLOW}⏳ PostgreSQLの起動を待機中...${NC}"
while ! docker-compose exec -T postgres pg_isready -U readscape_user -d readscape > /dev/null 2>&1; do
  echo -e "${YELLOW}  PostgreSQLの起動を待機中...${NC}"
  sleep 2
done
echo -e "${GREEN}✅ PostgreSQLが起動しました${NC}"

# データベースマイグレーション実行
echo -e "${YELLOW}🗄️  データベースマイグレーションを実行中...${NC}"
cd consumer-api
./gradlew flywayMigrate
cd ..

echo -e "${GREEN}🎉 開発環境の起動が完了しました！${NC}"
echo ""
echo -e "${BLUE}📋 アクセス情報:${NC}"
echo -e "  Consumer API:          http://localhost:8080/api"
echo -e "  Inventory Management:  http://localhost:8081/api"
echo -e "  pgAdmin:               http://localhost:8082"
echo -e "    Email: admin@readscape.jp"
echo -e "    Password: admin123"
echo -e "  PostgreSQL:            localhost:5432"
echo -e "    Database: readscape"
echo -e "    User: readscape_user"
echo -e "    Password: readscape_pass"
echo ""
echo -e "${BLUE}🔍 ログ確認コマンド:${NC}"
echo -e "  全ログ: docker-compose logs -f"
echo -e "  Consumer API: docker-compose logs -f consumer-api"
echo -e "  Inventory API: docker-compose logs -f inventory-api"
echo -e "  PostgreSQL: docker-compose logs -f postgres"
echo ""
echo -e "${BLUE}🛑 停止コマンド:${NC}"
echo -e "  docker-compose down"