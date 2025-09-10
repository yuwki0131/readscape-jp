# Readscape-JP API 利用ガイド

## 概要

Readscape-JPは日本語対応の書籍販売システムです。このAPIドキュメントでは、開発者がReadscape-JPの機能を活用するために必要な情報を提供します。

### システム構成

- **Consumer API**: 一般消費者向けのAPIサービス
- **Inventory Management API**: 管理者・マネージャー向けの在庫管理APIサービス

## API概要

### Consumer API

一般消費者が利用する書籍販売サイトの機能を提供します。

#### 主要機能
- 書籍の検索・閲覧
- ユーザー登録・認証
- ショッピングカート管理
- 注文処理
- レビュー投稿・閲覧

#### エンドポイント
- 開発環境: `http://localhost:8080/api`
- テスト環境: `https://consumer-api-dev.readscape.jp`
- 本番環境: `https://consumer-api.readscape.jp`

### Inventory Management API

管理者・マネージャーが利用する在庫管理システムの機能を提供します。

#### 主要機能
- 書籍の登録・編集・削除
- 在庫管理・追跡
- 注文管理
- 売上分析・レポート

#### エンドポイント
- 開発環境: `http://localhost:8081/api`
- テスト環境: `https://inventory-api-dev.readscape.jp/api`
- 本番環境: `https://inventory-api.readscape.jp/api`

## 認証について

### Consumer API認証

Consumer APIでは、ユーザーの会員登録・ログインにJWT（JSON Web Token）認証を使用しています。

#### ログインフロー

1. **ユーザー登録**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "name": "山田太郎"
  }'
```

2. **ログイン**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'
```

レスポンス:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "山田太郎"
  }
}
```

3. **認証が必要なAPIリクエスト**
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://localhost:8080/api/users/profile
```

### Inventory Management API認証

Inventory Management APIでは、管理者・マネージャーのアカウント認証にJWT認証を使用しています。

#### ログインフロー

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "adminPassword123"
  }'
```

レスポンス:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@readscape.jp",
    "role": "ADMIN"
  }
}
```

## 基本的な使用例

### 1. 書籍検索機能

#### 全書籍一覧の取得
```bash
curl http://localhost:8080/api/books
```

#### カテゴリー別検索
```bash
curl "http://localhost:8080/api/books?category=技術書&page=0&size=20"
```

#### キーワード検索
```bash
curl "http://localhost:8080/api/books/search?q=Java&page=0&size=10"
```

#### 書籍詳細の取得
```bash
curl http://localhost:8080/api/books/1
```

### 2. ショッピング機能

#### カート内容の確認
```bash
curl -H "Authorization: Bearer <your-token>" \
  http://localhost:8080/api/cart
```

#### カートに書籍を追加
```bash
curl -X POST http://localhost:8080/api/cart \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": 1,
    "quantity": 2
  }'
```

#### 注文の作成
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "credit_card",
    "shippingAddress": "東京都渋谷区1-1-1"
  }'
```

### 3. 管理者向け書籍管理

#### 書籍一覧の取得
```bash
curl -H "Authorization: Bearer <admin-token>" \
  http://localhost:8081/api/admin/books
```

#### 新規書籍の登録
```bash
curl -X POST http://localhost:8081/api/admin/books \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot実践入門",
    "author": "山田太郎",
    "isbn": "9784000000001",
    "category": "技術書",
    "price": 3200,
    "description": "Spring Bootの実践的な内容を扱った書籍です。",
    "publicationDate": "2024-01-15",
    "publisher": "技術出版社",
    "pages": 450,
    "initialStock": 100
  }'
```

#### 在庫更新
```bash
curl -X POST http://localhost:8081/api/admin/inventory/1/stock \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INBOUND",
    "quantity": 50,
    "reason": "新規入荷"
  }'
```

## レスポンス形式

### 成功レスポンス例

#### 書籍一覧取得の成功レスポンス
```json
{
  "books": [
    {
      "id": 1,
      "title": "Spring Boot実践入門",
      "author": "山田太郎",
      "price": 3200,
      "category": "技術書",
      "rating": 4.5,
      "reviewCount": 25,
      "imageUrl": "https://images.readscape.jp/books/1.jpg",
      "inStock": true
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 50,
    "size": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### エラーレスポンス例

#### バリデーションエラー
```json
{
  "error": "VALIDATION_ERROR",
  "message": "入力値に誤りがあります",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/cart",
  "fieldErrors": [
    {
      "field": "bookId",
      "message": "書籍IDは必須です"
    },
    {
      "field": "quantity",
      "message": "数量は1以上99以下で入力してください"
    }
  ]
}
```

#### 認証エラー
```json
{
  "error": "AUTHENTICATION_ERROR",
  "message": "認証が必要です",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/users/profile"
}
```

## レート制限

### Consumer API
- 未認証ユーザー: 100リクエスト/時間
- 認証済みユーザー: 1000リクエスト/時間

### Inventory Management API
- 管理者・マネージャー: 10000リクエスト/時間

レート制限に達した場合、HTTP 429 (Too Many Requests) が返されます。

## パラメータ仕様

### ページネーション
- `page`: ページ番号（0から開始、デフォルト: 0）
- `size`: ページサイズ（1-100、デフォルト: 10）

### ソート
- `sortBy`: ソート条件
  - 書籍API: `title`, `author`, `price_asc`, `price_desc`, `rating`, `popularity`, `newest`, `oldest`

### フィルタ
- `category`: カテゴリー名での絞り込み
- `keyword`: タイトル・著者名での検索
- `status`: ステータスでの絞り込み（管理者API）

## エラーコード一覧

| HTTPステータス | エラーコード | 説明 |
|---|---|---|
| 400 | VALIDATION_ERROR | 入力値バリデーションエラー |
| 401 | AUTHENTICATION_ERROR | 認証エラー |
| 403 | AUTHORIZATION_ERROR | 認可エラー |
| 404 | RESOURCE_NOT_FOUND | リソースが見つからない |
| 409 | DUPLICATE_RESOURCE | リソースの重複 |
| 429 | RATE_LIMIT_EXCEEDED | レート制限超過 |
| 500 | INTERNAL_SERVER_ERROR | サーバー内部エラー |

## サポートされるデータ形式

### Content-Type
- リクエスト: `application/json`
- レスポンス: `application/json`

### 文字エンコーディング
- UTF-8

### 日時形式
- ISO 8601形式: `YYYY-MM-DDTHH:mm:ssZ`
- 日付のみ: `YYYY-MM-DD`

## 開発者向けリソース

### Swagger UI
- Consumer API: `http://localhost:8080/api/swagger-ui.html`
- Inventory Management API: `http://localhost:8081/api/swagger-ui.html`

### OpenAPI仕様書
- Consumer API: `http://localhost:8080/api/api-docs`
- Inventory Management API: `http://localhost:8081/api/api-docs`

### SDKとライブラリ

現在、公式SDKは提供されていませんが、以下のライブラリを使用することを推奨します：

#### JavaScript/TypeScript
```javascript
// axios使用例
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

// 認証トークンの設定
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 書籍一覧取得
const getBooks = async (params = {}) => {
  const response = await api.get('/books', { params });
  return response.data;
};
```

## トラブルシューティング

### よくある問題

#### 1. CORS エラー
**問題**: ブラウザから直接APIを呼び出す際にCORSエラーが発生

**解決法**: 
- 開発環境では、APIサーバーの設定でCORSを許可
- 本番環境では、同一オリジンからのリクエストまたは適切なCORS設定を使用

#### 2. 認証トークンの有効期限切れ
**問題**: APIリクエスト時に401エラーが返される

**解決法**:
- リフレッシュトークンを使用してトークンを更新
- 自動的にトークンを更新するライブラリを使用

#### 3. レート制限超過
**問題**: 429エラーが返される

**解決法**:
- リクエスト頻度を調整
- 必要に応じて複数のAPIキーを使用（将来実装予定）

### お問い合わせ

技術的な質問やサポートが必要な場合は、以下までご連絡ください：

- Email: api-support@readscape.jp
- 開発者フォーラム: https://forum.readscape.jp/api
- GitHub Issues: https://github.com/readscape-jp/api-issues