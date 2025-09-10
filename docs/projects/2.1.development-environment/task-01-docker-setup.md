# タスク01: Docker開発環境構築

## タスク概要
Docker と Docker Compose を使用した一貫性のある開発環境の構築を行います。

## 実装内容

### 1. マルチサービス docker-compose.yml
```yaml
version: '3.8'
services:
  # Consumer API (Port: 8080)
  consumer-api:
    build: 
      context: ./consumer-api
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - JWT_SECRET=docker-development-jwt-secret-key
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - ./consumer-api/src:/app/src
      - gradle_cache:/home/gradle/.gradle
    networks:
      - readscape_network

  # Inventory Management API (Port: 8081)
  inventory-api:
    build:
      context: ./inventory-management-api
      dockerfile: Dockerfile
    ports:
      - "8081:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - JWT_SECRET=docker-development-jwt-secret-key
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - ./inventory-management-api/src:/app/src
      - gradle_cache_inventory:/home/gradle/.gradle
    networks:
      - readscape_network

  # PostgreSQL Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: readscape
      POSTGRES_USER: readscape_user
      POSTGRES_PASSWORD: readscape_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./infrastructure/config/postgres:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U readscape_user -d readscape"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - readscape_network

  # pgAdmin4 (Port: 8082)
  pgadmin:
    image: dpage/pgadmin4:latest
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@readscape.jp
      PGADMIN_DEFAULT_PASSWORD: admin123
    ports:
      - "8082:80"
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - pgadmin_data:/var/lib/pgadmin
    networks:
      - readscape_network

volumes:
  postgres_data:
  pgadmin_data:
  gradle_cache:
  gradle_cache_inventory:

networks:
  readscape_network:
    driver: bridge
```

### 2. 最適化された Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

# 必要なパッケージのインストール
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Gradle Wrapperファイルのコピー
COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .

# 実行権限の付与
RUN chmod +x ./gradlew

# 依存関係の事前ダウンロード（キャッシュ効率化）
RUN ./gradlew dependencies --no-daemon || return 0

# ソースコードのコピー
COPY src src/

# アプリケーションのビルド
RUN ./gradlew build --no-daemon -x test

# ヘルスチェック用のエンドポイント設定
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# ポートの公開
EXPOSE 8080

# アプリケーションの起動
CMD ["./gradlew", "bootRun", "--no-daemon"]
```

### 3. 包括的な開発用スクリプト

#### 環境起動スクリプト (`scripts/dev-start.sh`)
- 既存コンテナのクリーンアップ
- Dockerイメージのビルド
- サービスの起動とヘルスチェック
- 自動マイグレーション実行
- アクセス情報の表示

#### 環境停止スクリプト (`scripts/dev-stop.sh`)
- 通常停止モード（データ保持）
- 完全クリーンアップモード（`--clean`オプション）

#### ログ監視スクリプト (`scripts/logs.sh`)
- サービス別ログ監視
- 全サービス一括監視

#### DB初期化スクリプト (`scripts/db-init.sh`)
- PostgreSQL接続確認
- マイグレーション実行
- 初期データ投入

## 受け入れ条件
- [x] Docker環境が正常に起動する
- [x] 複数のAPIサービスが同時稼働する
- [x] アプリケーションにアクセスできる
- [x] データベースが正常に動作する
- [x] ヘルスチェックが機能する
- [x] ホットリロード（Volume Mount）が機能する
- [x] ポート競合が発生しない（8080, 8081, 8082, 5432）
- [x] 環境変数が正しく設定される
- [x] pgAdmin4でDB管理ができる
- [x] 開発用スクリプトが完備されている

## サービス構成

| サービス | ポート | 用途 | アクセスURL |
|----------|--------|------|-------------|
| consumer-api | 8080 | Consumer API | http://localhost:8080/api |
| inventory-api | 8081 | Inventory Management API | http://localhost:8081/api |
| postgres | 5432 | PostgreSQL Database | localhost:5432 |
| pgadmin | 8082 | DB管理ツール | http://localhost:8082 |

## 使用方法

### 環境起動
```bash
./scripts/dev-start.sh
```

### 環境停止
```bash
# 通常停止（データ保持）
./scripts/dev-stop.sh

# 完全クリーンアップ
./scripts/dev-stop.sh --clean
```

### ログ監視
```bash
# 全ログ監視
./scripts/logs.sh

# 特定サービス
./scripts/logs.sh consumer
./scripts/logs.sh inventory
./scripts/logs.sh postgres
```

## 関連ファイル
- `docker-compose.yml`
- `consumer-api/Dockerfile`
- `inventory-management-api/Dockerfile`
- `scripts/dev-start.sh`
- `scripts/dev-stop.sh`
- `scripts/logs.sh`
- `scripts/db-init.sh`