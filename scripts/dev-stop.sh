#!/bin/bash

# Readscape-JP 開発環境停止スクリプト
set -e

# 色付きメッセージ用の設定
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🛑 Readscape-JP 開発環境を停止しています...${NC}"

# 引数の確認
CLEAN=${1:-""}

if [ "$CLEAN" = "--clean" ] || [ "$CLEAN" = "-c" ]; then
    echo -e "${YELLOW}🗑️  完全クリーンアップモードで停止中...${NC}"
    
    # コンテナとボリュームを完全削除
    docker-compose down --volumes --remove-orphans
    
    # 未使用のイメージとネットワークをクリーンアップ
    echo -e "${YELLOW}🧹 未使用のDockerリソースをクリーンアップ中...${NC}"
    docker system prune -f
    
    echo -e "${GREEN}✅ 開発環境の完全クリーンアップが完了しました${NC}"
    echo -e "${YELLOW}💡 次回起動時はすべてのイメージが再ビルドされます${NC}"
else
    echo -e "${YELLOW}⏹️  通常停止中...${NC}"
    
    # コンテナを停止（ボリュームは保持）
    docker-compose down --remove-orphans
    
    echo -e "${GREEN}✅ 開発環境が停止されました${NC}"
    echo -e "${GREEN}💾 データは保持されています${NC}"
fi

echo ""
echo -e "${BLUE}📋 停止オプション:${NC}"
echo -e "  通常停止:         ./scripts/dev-stop.sh"
echo -e "  完全クリーンアップ: ./scripts/dev-stop.sh --clean"
echo ""
echo -e "${BLUE}🚀 再起動コマンド:${NC}"
echo -e "  ./scripts/dev-start.sh"