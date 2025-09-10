# Readscape-JP API 認証ガイド

## 概要

Readscape-JP APIでは、JWT（JSON Web Token）を使用した認証システムを採用しています。このガイドでは、各APIでの認証方法について詳しく説明します。

## Consumer API 認証

### 認証フロー概要

1. ユーザー登録またはログイン
2. JWTトークンの取得
3. APIリクエスト時にトークンを使用
4. トークン有効期限切れ時にリフレッシュ

### 1. ユーザー登録

新規ユーザーのアカウント作成を行います。

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "name": "山田太郎"
  }'
```

#### リクエストボディ

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| email | string | ✓ | メールアドレス（重複不可） |
| password | string | ✓ | パスワード（8文字以上） |
| name | string | ✓ | ユーザー名 |

#### レスポンス例

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "山田太郎",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### 2. ログイン

登録済みユーザーでのログインを行い、JWTトークンを取得します。

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'
```

#### リクエストボディ

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| email | string | ✓ | 登録済みメールアドレス |
| password | string | ✓ | パスワード |

#### レスポンス例

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwibmFtZSI6IuWxseWglOWkquimjiIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1NDc3NzUwMjJ9.xyz123abcDEF456",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "山田太郎",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
}
```

### 3. 認証付きAPIリクエスト

取得したJWTトークンを`Authorization`ヘッダーに設定してAPIを呼び出します。

```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://localhost:8080/api/users/profile
```

### 4. トークンリフレッシュ

アクセストークンの有効期限が切れた場合、リフレッシュトークンを使用して新しいトークンを取得します。

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

#### レスポンス例

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

### 5. ログアウト

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Inventory Management API 認証

### 認証フロー概要

1. 管理者アカウントでログイン
2. JWTトークンの取得
3. 管理者APIリクエスト時にトークンを使用
4. 権限レベルに応じたアクセス制御

### 1. 管理者ログイン

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "adminPassword123"
  }'
```

#### リクエストボディ

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| username | string | ✓ | 管理者ユーザー名 |
| password | string | ✓ | パスワード |

#### レスポンス例

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@readscape.jp",
    "role": "ADMIN",
    "createdAt": "2024-01-15T10:30:00Z",
    "lastLoginAt": "2024-01-15T10:30:00Z"
  }
}
```

### 2. 管理者APIリクエスト

```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://localhost:8081/api/admin/books
```

## 権限レベル

### Consumer API
- **未認証**: 書籍閲覧、検索（読み取り専用）
- **認証済み**: カート操作、注文作成、レビュー投稿、プロフィール管理

### Inventory Management API
- **MANAGER**: 書籍管理、在庫管理、注文確認
- **ADMIN**: 全機能（書籍削除、一括操作、システム設定）

## 認証エラーのハンドリング

### エラーレスポンス例

#### 401 Unauthorized（認証エラー）
```json
{
  "error": "AUTHENTICATION_ERROR",
  "message": "認証が必要です",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/users/profile"
}
```

#### 403 Forbidden（認可エラー）
```json
{
  "error": "AUTHORIZATION_ERROR", 
  "message": "この操作を実行する権限がありません",
  "status": 403,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/admin/books/1"
}
```

#### Token Expired（トークン有効期限切れ）
```json
{
  "error": "TOKEN_EXPIRED",
  "message": "認証トークンの有効期限が切れています",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/cart"
}
```

## セキュリティのベストプラクティス

### トークン管理

1. **安全な保存**
   - ブラウザ: httpOnly Cookieまたはセキュアなローカルストレージ
   - モバイルアプリ: キーチェーン/キーストア

2. **有効期限管理**
   - アクセストークン: 1時間
   - リフレッシュトークン: 30日
   - 自動リフレッシュの実装

3. **セキュアな通信**
   - HTTPS必須
   - 適切なCORS設定

### 実装例（JavaScript）

```javascript
class AuthManager {
  constructor(baseURL) {
    this.baseURL = baseURL;
    this.token = localStorage.getItem('accessToken');
    this.refreshToken = localStorage.getItem('refreshToken');
  }

  async login(email, password) {
    try {
      const response = await fetch(`${this.baseURL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
      });

      if (response.ok) {
        const data = await response.json();
        this.token = data.token;
        this.refreshToken = data.refreshToken;
        
        localStorage.setItem('accessToken', this.token);
        localStorage.setItem('refreshToken', this.refreshToken);
        
        return data;
      } else {
        throw new Error('ログインに失敗しました');
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }

  async makeAuthenticatedRequest(url, options = {}) {
    if (!this.token) {
      throw new Error('認証が必要です');
    }

    const headers = {
      'Authorization': `Bearer ${this.token}`,
      'Content-Type': 'application/json',
      ...options.headers
    };

    try {
      const response = await fetch(`${this.baseURL}${url}`, {
        ...options,
        headers
      });

      if (response.status === 401) {
        // トークン有効期限切れの場合、リフレッシュを試行
        await this.refreshAccessToken();
        
        // リフレッシュ後に再度リクエスト
        headers['Authorization'] = `Bearer ${this.token}`;
        return await fetch(`${this.baseURL}${url}`, {
          ...options,
          headers
        });
      }

      return response;
    } catch (error) {
      console.error('API request error:', error);
      throw error;
    }
  }

  async refreshAccessToken() {
    if (!this.refreshToken) {
      throw new Error('リフレッシュトークンがありません');
    }

    try {
      const response = await fetch(`${this.baseURL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ refreshToken: this.refreshToken })
      });

      if (response.ok) {
        const data = await response.json();
        this.token = data.token;
        this.refreshToken = data.refreshToken;
        
        localStorage.setItem('accessToken', this.token);
        localStorage.setItem('refreshToken', this.refreshToken);
      } else {
        // リフレッシュ失敗時はログアウト
        this.logout();
        throw new Error('認証の更新に失敗しました');
      }
    } catch (error) {
      console.error('Token refresh error:', error);
      throw error;
    }
  }

  logout() {
    this.token = null;
    this.refreshToken = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }

  isAuthenticated() {
    return !!this.token;
  }
}

// 使用例
const authManager = new AuthManager('http://localhost:8080/api');

// ログイン
await authManager.login('user@example.com', 'password123');

// 認証が必要なAPIの呼び出し
const response = await authManager.makeAuthenticatedRequest('/users/profile');
const userProfile = await response.json();
```

## トラブルシューティング

### よくある問題

1. **トークンが無効です**
   - 原因: トークンの形式が正しくない、または有効期限切れ
   - 解決: 正しい形式でトークンを設定し、有効期限を確認

2. **CORS エラー**
   - 原因: ブラウザのCORSポリシーによる制限
   - 解決: APIサーバーで適切なCORS設定を行う

3. **権限エラー**
   - 原因: 必要な権限レベルが不足
   - 解決: 適切な権限を持つアカウントでログインする

### サポート

認証関連の問題については、以下までお問い合わせください：

- Email: auth-support@readscape.jp
- GitHub Issues: https://github.com/readscape-jp/api-issues