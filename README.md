# Readscape-JP

日本語対応の書籍販売システム - マイクロサービスアーキテクチャによるEC プラットフォーム

## 🌟 概要

Readscape-JPは現代的なマイクロサービスアーキテクチャで構築された書籍販売システムです。一般消費者向けのショッピング機能と管理者向けの在庫・注文管理機能を提供します。

### 主要機能
- 📚 **書籍検索・閲覧**: カテゴリー別検索、人気書籍、レビューシステム
- 🛒 **ショッピング**: カート管理、注文処理、支払い連携
- 👤 **ユーザー管理**: JWT認証、プロフィール管理
- 📊 **管理機能**: 在庫管理、売上分析、注文管理
- 🔒 **セキュリティ**: RBAC、監査ログ、レート制限

## 🏗️ システムアーキテクチャ

```
┌─────────────────┐    ┌──────────────────────┐    ┌─────────────────────────┐
│  フロントエンド    │    │   Consumer API       │    │ Inventory Management   │
│                │    │   (Port: 8080)       │    │ API (Port: 8081)       │
│  React/Vue.js  │◄──►│                      │    │                         │
│  Mobile App    │    │  ・書籍閲覧・検索      │    │  ・書籍管理・在庫管理    │
└─────────────────┘    │  ・ユーザー認証        │    │  ・注文管理・分析        │
                       │  ・カート・注文        │    │  ・管理者機能           │
                       └──────────┬───────────┘    └─────────┬───────────────┘
                                 │                           │
                                 └─────────┬─────────────────┘
                                          │
                       ┌──────────────────▼────────────────────┐
                       │             データ層                   │
                       │  ┌─────────────────┐ ┌──────────────┐  │
                       │  │  PostgreSQL 15  │ │   Redis      │  │
                       │  │  (メインDB)      │ │ (キャッシュ)  │  │
                       │  └─────────────────┘ └──────────────┘  │
                       └───────────────────────────────────────┘
```

## 🚀 クイックスタート

### 前提条件
- **Java 21+** (推奨: OpenJDK 21)
- **Gradle 8.11+**
- **Docker & Docker Compose**
- **PostgreSQL 15+**
- **Redis 7+**

### 開発環境セットアップ

1. **リポジトリのクローン**
```bash
git clone https://github.com/your-org/readscape-jp.git
cd readscape-jp
```

2. **Java・Gradle環境構築** (asdf使用の場合)
```bash
asdf install java adoptopenjdk-21.0.5+11.0.LTS
asdf local java adoptopenjdk-21.0.5+11.0.LTS
asdf install gradle 8.11
asdf local gradle 8.11
```

3. **依存サービス起動**
```bash
# PostgreSQL・Redis起動
docker-compose up -d postgres redis

# データベースマイグレーション
sh infrastructure/db/migrate.sh
```

4. **アプリケーション起動**
```bash
# Consumer API (ポート: 8080)
cd consumer-api
./gradlew bootRun

# 新しいターミナルで Inventory Management API (ポート: 8081)
cd inventory-management-api
./gradlew bootRun
```

### API接続確認

Consumer APIの動作確認:
```bash
curl http://localhost:8080/api/books
```

Inventory Management APIの動作確認:
```bash
curl http://localhost:8081/api/health
```

## 📚 ドキュメント

詳細なドキュメントは [`docs/`](./docs/) ディレクトリに整理されています:

- **[API仕様書](./docs/api/)**: Consumer API・Inventory Management APIの詳細仕様
- **[アーキテクチャ設計](./docs/technical/)**: システム設計・技術選定の説明
- **[開発標準](./docs/standards/)**: コーディング規約・開発ガイドライン
- **[データベース設計](./docs/database/)**: ERD・スキーマ設計
- **[トラブルシューティング](./docs/api/troubleshooting.md)**: よくある問題と解決方法

## 🔧 開発・テスト

### テスト実行
```bash
# 単体テスト
./gradlew test

# 統合テスト (TestContainers使用)
./gradlew integrationTest

# テストカバレッジレポート生成
./gradlew jacocoTestReport
```

### ビルド
```bash
# 各サービスのビルド
cd consumer-api && ./gradlew build
cd inventory-management-api && ./gradlew build

# Dockerイメージビルド
docker-compose build
```

### 品質チェック
```bash
# コードフォーマット確認
./gradlew checkstyleMain

# 脆弱性スキャン
./gradlew dependencyCheckAnalyze

# テストカバレッジ検証 (最低80%必要)
./gradlew jacocoTestCoverageVerification
```

## 🌐 API概要

### Consumer API (Port: 8080)
一般消費者向けの書籍販売機能を提供

| エンドポイント | 説明 |
|---|---|
| `GET /api/books` | 書籍一覧・検索 |
| `GET /api/books/{id}` | 書籍詳細 |
| `POST /api/auth/login` | ユーザーログイン |
| `GET/POST/PUT/DELETE /api/cart` | カート操作 |
| `POST /api/orders` | 注文作成 |

### Inventory Management API (Port: 8081)
管理者・マネージャー向けの在庫・注文管理機能を提供

| エンドポイント | 説明 |
|---|---|
| `GET/POST/PUT/DELETE /api/admin/books` | 書籍管理 |
| `POST /api/admin/inventory/{id}/stock` | 在庫更新 |
| `GET /api/admin/orders` | 注文管理 |
| `GET /api/admin/analytics/*` | 売上分析・レポート |

## 🔒 セキュリティ

### 認証・認可
- **JWT認証**: アクセストークン(1時間) + リフレッシュトークン(30日)
- **ロールベースアクセス制御**: CONSUMER, MANAGER, ADMIN
- **トークンブラックリスト**: ログアウト・セキュリティ違反時の即座無効化

### セキュリティ対策
- HTTPS強制・セキュリティヘッダー設定
- レート制限 (未認証: 100req/h, 認証済み: 1000req/h)
- 入力値バリデーション・SQLインジェクション対策
- 監査ログ・不正アクセス検知

## 📊 運用・監視

### ヘルスチェック
```bash
# Consumer API
curl http://localhost:8080/actuator/health

# Inventory Management API
curl http://localhost:8081/actuator/health
```

### メトリクス監視
- Spring Boot Actuatorによるメトリクス公開
- JaCoCo テストカバレッジレポート
- APMツール連携対応

## 🚢 デプロイ

### Docker Compose (ローカル・開発)
```bash
# 全サービス起動
docker-compose up -d

# ログ確認
docker-compose logs -f consumer-api inventory-api
```

### 本番デプロイ
```bash
# 環境別設定ファイル準備
cp .env.example .env.production

# 本番環境変数設定
docker-compose -f docker-compose.prod.yml up -d
```

## 🤝 コントリビューション

### 開発フロー
1. Issueを作成・確認
2. feature/機能名 ブランチを作成
3. 開発・テスト実装
4. プルリクエスト作成
5. コードレビュー・マージ

### コーディング規約
- [Java コーディング標準](./docs/standards/java-coding-standards.md) に従う
- テストカバレッジ80%以上を維持
- 全APIに適切なOpenAPI仕様を記述
