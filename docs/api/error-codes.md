# Readscape-JP API エラーコード仕様

## 📋 概要

このドキュメントではReadscape-JP APIで発生する可能性のあるエラーコード、HTTPステータス、および対処方法を説明します。

## 🔍 エラーレスポンス形式

### 基本構造

```json
{
  "error": "ERROR_CODE",
  "message": "ユーザー向けエラーメッセージ",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/books/999",
  "details": {
    "additionalInfo": "追加情報（オプション）"
  }
}
```

### バリデーションエラー

```json
{
  "error": "VALIDATION_ERROR",
  "message": "入力値に誤りがあります",
  "status": 400,
  "fieldErrors": {
    "fieldName": "フィールド別エラーメッセージ"
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/users/register"
}
```

## 📚 エラーコード一覧

### 1️⃣ 認証・認可エラー (4xx)

| エラーコード | HTTP | メッセージ | 原因・対処法 |
|-------------|------|-----------|-------------|
| `UNAUTHORIZED` | 401 | 認証が必要です | JWTトークンが未提供・無効 |
| `TOKEN_EXPIRED` | 401 | トークンの有効期限が切れています | リフレッシュトークンで更新 |
| `TOKEN_INVALID` | 401 | 無効なトークンです | 再ログイン必要 |
| `INVALID_CREDENTIALS` | 401 | ユーザー名またはパスワードが間違っています | 認証情報を確認 |
| `ACCESS_DENIED` | 403 | このリソースへのアクセス権限がありません | ロール権限を確認 |
| `ROLE_INSUFFICIENT` | 403 | 必要な権限レベルに達していません | 管理者権限が必要 |

#### 例: 認証エラー

```json
{
  "error": "UNAUTHORIZED",
  "message": "認証が必要です",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/cart"
}
```

### 2️⃣ リソース不存在エラー (404)

| エラーコード | HTTP | メッセージ | 原因・対処法 |
|-------------|------|-----------|-------------|
| `BOOK_NOT_FOUND` | 404 | 指定された書籍が見つかりません | 書籍IDを確認 |
| `USER_NOT_FOUND` | 404 | 指定されたユーザーが見つかりません | ユーザーIDを確認 |
| `ORDER_NOT_FOUND` | 404 | 指定された注文が見つかりません | 注文IDを確認 |
| `CART_NOT_FOUND` | 404 | カートが見つかりません | カート初期化が必要 |
| `REVIEW_NOT_FOUND` | 404 | 指定されたレビューが見つかりません | レビューIDを確認 |

#### 例: 書籍不存在エラー

```json
{
  "error": "BOOK_NOT_FOUND",
  "message": "指定された書籍が見つかりません: ID 999",
  "status": 404,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/books/999"
}
```

### 3️⃣ バリデーションエラー (400)

| エラーコード | HTTP | メッセージ | 原因・対処法 |
|-------------|------|-----------|-------------|
| `VALIDATION_ERROR` | 400 | 入力値に誤りがあります | フィールドエラーを確認 |
| `INVALID_PAGE_SIZE` | 400 | 不正なページサイズです | 1-100の範囲で指定 |
| `INVALID_SORT_FIELD` | 400 | 不正なソートフィールドです | 指定可能フィールドを確認 |
| `INVALID_DATE_FORMAT` | 400 | 日付形式が正しくありません | ISO 8601形式で指定 |

#### 例: バリデーションエラー

```json
{
  "error": "VALIDATION_ERROR",
  "message": "入力値に誤りがあります",
  "status": 400,
  "fieldErrors": {
    "email": "有効なメールアドレスを入力してください",
    "password": "パスワードは8文字以上、英数字を含む必要があります",
    "username": "ユーザー名は3文字以上20文字以下である必要があります"
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/users/register"
}
```

### 4️⃣ ビジネスロジックエラー (400/409)

| エラーコード | HTTP | メッセージ | 原因・対処法 |
|-------------|------|-----------|-------------|
| `INSUFFICIENT_STOCK` | 400 | 在庫が不足しています | 在庫数を確認・調整 |
| `DUPLICATE_USERNAME` | 409 | ユーザー名が既に使用されています | 別のユーザー名を選択 |
| `DUPLICATE_EMAIL` | 409 | メールアドレスが既に登録されています | 別のメールアドレスを使用 |
| `DUPLICATE_ISBN` | 409 | ISBNが既に登録されています | 既存書籍を確認 |
| `ORDER_ALREADY_SHIPPED` | 400 | 既に発送済みの注文です | キャンセル不可 |
| `CART_EMPTY` | 400 | カートが空です | 商品を追加してから注文 |

#### 例: 在庫不足エラー

```json
{
  "error": "INSUFFICIENT_STOCK",
  "message": "在庫が不足しています。現在在庫: 3, 要求数量: 5",
  "status": 400,
  "details": {
    "bookId": 123,
    "requestedQuantity": 5,
    "availableQuantity": 3
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/cart/items"
}
```

### 5️⃣ サーバーエラー (5xx)

| エラーコード | HTTP | メッセージ | 原因・対処法 |
|-------------|------|-----------|-------------|
| `INTERNAL_SERVER_ERROR` | 500 | 内部サーバーエラーが発生しました | サーバー管理者に連絡 |
| `DATABASE_CONNECTION_ERROR` | 500 | データベース接続エラー | 一時的な障害、再試行 |
| `EXTERNAL_SERVICE_ERROR` | 502 | 外部サービスエラー | 決済・配送サービス障害 |
| `SERVICE_UNAVAILABLE` | 503 | サービスが一時的に利用できません | メンテナンス中 |

#### 例: サーバーエラー

```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "内部サーバーエラーが発生しました",
  "status": 500,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/orders",
  "traceId": "abc123def456"
}
```

## 🛠️ エラーハンドリング ベストプラクティス

### 1. クライアント側での処理

```javascript
// JavaScript例
async function apiRequest(url, options) {
  try {
    const response = await fetch(url, options);
    
    if (!response.ok) {
      const error = await response.json();
      
      switch (error.error) {
        case 'TOKEN_EXPIRED':
          await refreshToken();
          return apiRequest(url, options); // リトライ
          
        case 'INSUFFICIENT_STOCK':
          showStockError(error.details);
          break;
          
        case 'VALIDATION_ERROR':
          showValidationErrors(error.fieldErrors);
          break;
          
        default:
          showGenericError(error.message);
      }
      
      throw new Error(error.message);
    }
    
    return await response.json();
  } catch (error) {
    console.error('API Request failed:', error);
    throw error;
  }
}
```

### 2. リトライ戦略

| エラータイプ | リトライ推奨 | 待機時間 | 最大回数 |
|-------------|-------------|----------|----------|
| `TOKEN_EXPIRED` | ✅ 即時 | なし | 1回 |
| `INTERNAL_SERVER_ERROR` | ✅ 指数バックオフ | 1秒〜 | 3回 |
| `INSUFFICIENT_STOCK` | ❌ なし | - | - |
| `VALIDATION_ERROR` | ❌ なし | - | - |

### 3. ログ記録推奨

```json
{
  "level": "ERROR",
  "timestamp": "2024-01-15T10:30:00Z",
  "error_code": "INSUFFICIENT_STOCK",
  "user_id": 12345,
  "request_id": "req_abc123",
  "path": "/api/cart/items",
  "method": "POST",
  "details": {
    "book_id": 789,
    "requested_quantity": 10,
    "available_quantity": 3
  }
}
```

## 🔍 デバッグ情報

### レスポンスヘッダー

本番環境では以下のヘッダーが含まれます：

```
X-Request-ID: req_abc123def456
```

### トレースID

エラー発生時、サポートに以下の情報を提供してください：

- **Request ID**: レスポンスの `traceId` フィールド
- **Timestamp**: エラー発生時刻
- **User ID**: 認証済みユーザーのID
- **API Path**: 実行したエンドポイント

## 📞 サポート連絡先

エラーが解決しない場合：

- **技術サポート**: api-support@readscape.jp
- **緊急時**: emergency@readscape.jp
- **GitHub Issues**: https://github.com/readscape-jp/api-issues

---

**Last Updated**: 2024年1月15日  
**Version**: 1.0.0