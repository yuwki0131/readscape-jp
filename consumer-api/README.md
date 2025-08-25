# Readscape Consumer API

一般消費者向けの書籍販売APIサービスです。

## 主な機能

- 書籍閲覧・検索
- ショッピングカート管理
- ユーザー登録・認証
- 注文処理
- レビューシステム

## 技術スタック

- Java 21
- Spring Boot 3.2.0
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT認証
- OpenAPI 3.0

## セットアップ

### 前提条件

- Java 21+
- PostgreSQL 15+
- Docker & Docker Compose（推奨）

### 開発環境構築

```bash
# プロジェクトのビルド
./gradlew build

# データベースマイグレーション
./gradlew flywayMigrate

# アプリケーション起動
./gradlew bootRun
```

### Docker環境での起動

```bash
# Docker Composeでの起動（今後実装予定）
docker-compose up -d
```

## API仕様書

アプリケーション起動後、以下のURLでAPI仕様を確認できます：

- Swagger UI: http://localhost:8080/api/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/api-docs

## ヘルスチェック

```bash
curl http://localhost:8080/api/health
```

## 開発ガイド

### プロジェクト構造

```
src/main/java/jp/readscape/consumer/
├── ConsumerApiApplication.java     # メインクラス
├── configurations/                 # 設定クラス
├── controllers/                     # RESTコントローラー
├── services/                       # ビジネスロジック
├── domain/                         # エンティティ・リポジトリ
├── dto/                            # データ転送オブジェクト
└── exceptions/                     # 例外処理
```

### テスト実行

```bash
# 全テスト実行
./gradlew test

# テストカバレッジ確認
./gradlew jacocoTestReport
```

## 設定

### プロファイル

- `dev`: 開発環境
- `test`: テスト環境
- `prod`: 本番環境（今後実装予定）

### 環境変数

| 変数名 | 説明 | デフォルト値 |
|--------|------|-------------|
| `JWT_SECRET` | JWT署名用秘密鍵 | `consumer-api-jwt-secret-key-change-in-production` |

## ライセンス

MIT License