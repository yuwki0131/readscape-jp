# Readscape-JP API 利用ガイド

## 📚 概要

Readscape-JPは日本語対応の書籍販売システムです。マイクロサービスアーキテクチャで構築された2つのAPIを提供します。

### サービス構成

| サービス | ポート | 目的 | 対象ユーザー |
|---------|-------|------|-------------|
| **Consumer API** | 8080 | 書籍閲覧・購入機能 | 一般消費者 |
| **Inventory Management API** | 8081 | 在庫・注文管理機能 | 管理者・マネージャー |

## 🚀 クイックスタート

### 1. 環境セットアップ

```bash
# リポジトリクローン
git clone https://github.com/your-org/readscape-jp.git
cd readscape-jp

# Docker環境起動
docker-compose up -d

# API動作確認
curl http://localhost:8080/books
curl http://localhost:8081/api/admin/health
```

### 2. 基本的な API 利用フロー

#### 📖 書籍の閲覧（認証不要）

```bash
# 書籍一覧取得
curl http://localhost:8080/books

# 書籍詳細取得
curl http://localhost:8080/books/1

# 書籍検索
curl "http://localhost:8080/books/search?q=Spring"
```

#### 🔐 ユーザー登録・ログイン

```bash
# ユーザー登録
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!",
    "fullName": "テストユーザー"
  }'

# ログイン（JWTトークン取得）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!"
  }'
```

#### 🛒 ショッピング機能（認証必要）

```bash
# カートに商品追加
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": 1,
    "quantity": 2
  }'

# 注文作成
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddress": "東京都渋谷区1-1-1",
    "shippingPhone": "090-1234-5678",
    "paymentMethod": "CREDIT_CARD"
  }'
```

## 🔒 認証・認可

### JWT トークン認証

Readscape-JPはJWT（JSON Web Token）ベースの認証を使用します。

#### トークンの取得

```bash
POST /api/auth/login
{
  "username": "your-username",
  "password": "your-password"
}
```

#### レスポンス例

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "role": "CONSUMER"
  }
}
```

#### トークンの使用

```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8080/api/cart
```

### ロールベースアクセス制御

| ロール | 権限 | アクセス可能API |
|-------|------|----------------|
| **CONSUMER** | 一般消費者 | 書籍閲覧、カート、注文 |
| **MANAGER** | 店舗管理者 | 在庫管理、注文管理 |
| **ADMIN** | システム管理者 | 全機能 + ユーザー管理 |
| **ANALYST** | 分析担当者 | 売上分析、レポート閲覧 |

## 📊 API エンドポイント一覧

### Consumer API (Port: 8080)

#### 書籍API
- `GET /books` - 書籍一覧取得
- `GET /books/{id}` - 書籍詳細取得
- `GET /books/search` - 書籍検索
- `GET /books/popular` - 人気書籍一覧
- `GET /books/categories` - カテゴリ一覧

#### 認証API
- `POST /api/auth/login` - ログイン
- `POST /api/auth/refresh` - トークン更新
- `POST /api/auth/logout` - ログアウト

#### ユーザーAPI
- `POST /api/users/register` - ユーザー登録
- `GET /api/users/profile` - プロフィール取得
- `PUT /api/users/profile` - プロフィール更新

#### ショッピングAPI
- `GET /api/cart` - カート取得
- `POST /api/cart/items` - カート追加
- `PUT /api/cart/items/{bookId}` - カート更新
- `DELETE /api/cart/items/{bookId}` - カート削除
- `POST /api/orders` - 注文作成
- `GET /api/orders` - 注文履歴取得

### Inventory Management API (Port: 8081)

#### 管理者書籍API
- `GET /api/admin/books` - 管理者書籍一覧
- `POST /api/admin/books` - 書籍作成
- `PUT /api/admin/books/{id}` - 書籍更新
- `DELETE /api/admin/books/{id}` - 書籍削除

#### 在庫管理API
- `GET /api/admin/inventory` - 在庫一覧
- `POST /api/admin/inventory/{id}/stock` - 在庫更新
- `GET /api/admin/inventory/low-stock` - 低在庫一覧

#### 注文管理API
- `GET /api/admin/orders` - 注文管理一覧
- `PUT /api/admin/orders/{id}/status` - 注文ステータス更新
- `GET /api/admin/orders/pending` - 保留中注文一覧

## 🎯 レスポンス形式

### 成功レスポンス

```json
{
  "success": true,
  "data": { ... },
  "message": "処理が完了しました",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### エラーレスポンス

```json
{
  "error": "BOOK_NOT_FOUND",
  "message": "指定された書籍が見つかりません",
  "status": 404,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/books/999"
}
```

### バリデーションエラー

```json
{
  "error": "VALIDATION_ERROR",
  "message": "入力値に誤りがあります",
  "status": 400,
  "fieldErrors": {
    "email": "有効なメールアドレスを入力してください",
    "password": "パスワードは8文字以上である必要があります"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## ⚡ レート制限

| 認証状態 | 制限 | 期間 |
|---------|------|------|
| 未認証 | 100リクエスト | 1時間 |
| 認証済み | 1,000リクエスト | 1時間 |
| ADMIN | 10,000リクエスト | 1時間 |

## 🔗 Swagger UI

インタラクティブなAPI仕様書は以下のURLで確認できます：

- **Consumer API**: http://localhost:8080/swagger-ui.html
- **Inventory Management API**: http://localhost:8081/swagger-ui.html

## 🐛 トラブルシューティング

### よくある問題

#### 1. 認証エラー (401 Unauthorized)

```bash
# 原因: トークンが無効または期限切れ
# 解決: 再ログインしてトークンを更新
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN"}'
```

#### 2. 在庫不足エラー (400 Bad Request)

```json
{
  "error": "INSUFFICIENT_STOCK",
  "message": "在庫が不足しています。現在在庫: 5, 要求数量: 10"
}
```

#### 3. 接続エラー

```bash
# データベース接続確認
curl http://localhost:8080/actuator/health

# コンテナ状態確認
docker-compose ps
```

## 📞 サポート

- **技術サポート**: api-support@readscape.jp
- **GitHub Issues**: https://github.com/readscape-jp/api-issues
- **API仕様書**: https://docs.readscape.jp/api

---

**Last Updated**: 2024年1月15日  
**API Version**: v1.0.0