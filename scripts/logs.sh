#!/bin/bash

# ログ監視スクリプト
set -e

# 色付きメッセージ用の設定
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 引数の処理
SERVICE=${1:-"all"}

echo -e "${BLUE}📋 Readscape-JP ログ監視${NC}"
echo -e "${BLUE}監視対象: ${SERVICE}${NC}"
echo ""

case "$SERVICE" in
    "consumer" | "consumer-api")
        echo -e "${GREEN}🔍 Consumer APIのログを監視中...${NC}"
        docker-compose logs -f consumer-api
        ;;
    "inventory" | "inventory-api")
        echo -e "${GREEN}🔍 Inventory Management APIのログを監視中...${NC}"
        docker-compose logs -f inventory-api
        ;;
    "postgres" | "db")
        echo -e "${GREEN}🔍 PostgreSQLのログを監視中...${NC}"
        docker-compose logs -f postgres
        ;;
    "pgadmin")
        echo -e "${GREEN}🔍 pgAdminのログを監視中...${NC}"
        docker-compose logs -f pgadmin
        ;;
    "all" | "")
        echo -e "${GREEN}🔍 全サービスのログを監視中...${NC}"
        docker-compose logs -f
        ;;
    *)
        echo -e "${RED}❌ 不明なサービス: $SERVICE${NC}"
        echo ""
        echo -e "${YELLOW}使用方法:${NC}"
        echo -e "  $0 [service]"
        echo ""
        echo -e "${YELLOW}利用可能なサービス:${NC}"
        echo -e "  consumer     Consumer APIのログ"
        echo -e "  inventory    Inventory Management APIのログ"
        echo -e "  postgres     PostgreSQLのログ"
        echo -e "  pgadmin      pgAdminのログ"
        echo -e "  all          全サービスのログ（デフォルト）"
        exit 1
        ;;
esac