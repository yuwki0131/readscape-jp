#!/bin/bash

# データベース初期化スクリプト
set -e

# 色付きメッセージ用の設定
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🗄️  データベースを初期化しています...${NC}"

# PostgreSQLコンテナが起動しているか確認
if ! docker-compose ps postgres | grep -q "Up"; then
    echo -e "${RED}❌ PostgreSQLコンテナが起動していません。まず 'docker-compose up -d postgres' を実行してください。${NC}"
    exit 1
fi

# データベースの存在確認と作成
echo -e "${YELLOW}📊 データベースの存在確認中...${NC}"

# 開発用データベース
docker-compose exec -T postgres psql -U readscape_user -lqt | cut -d \| -f 1 | grep -qw readscape_dev || {
    echo -e "${YELLOW}  開発用データベースを作成中...${NC}"
    docker-compose exec -T postgres psql -U readscape_user -c "CREATE DATABASE readscape_dev;"
}

# テスト用データベース
docker-compose exec -T postgres psql -U readscape_user -lqt | cut -d \| -f 1 | grep -qw readscape_test || {
    echo -e "${YELLOW}  テスト用データベースを作成中...${NC}"
    docker-compose exec -T postgres psql -U readscape_user -c "CREATE DATABASE readscape_test;"
}

# マイグレーションの実行
echo -e "${YELLOW}🔄 Flywayマイグレーションを実行中...${NC}"
cd consumer-api
./gradlew flywayMigrate
cd ..

# データベース接続テスト
echo -e "${YELLOW}🔍 データベース接続をテスト中...${NC}"
docker-compose exec -T postgres psql -U readscape_user -d readscape -c "SELECT COUNT(*) FROM readscape.users;" > /dev/null

echo -e "${GREEN}✅ データベースの初期化が完了しました！${NC}"
echo ""
echo -e "${BLUE}📋 データベース情報:${NC}"
echo -e "  メインDB:     readscape"
echo -e "  開発用DB:     readscape_dev"
echo -e "  テスト用DB:   readscape_test"
echo -e "  ユーザー:     readscape_user"
echo ""
echo -e "${BLUE}🔍 データベース確認コマンド:${NC}"
echo -e "  docker-compose exec postgres psql -U readscape_user -d readscape"