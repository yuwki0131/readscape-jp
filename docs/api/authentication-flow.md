# Readscape-JP 認証フロー ガイド

## 🔐 概要

Readscape-JP APIはJWT（JSON Web Token）ベースの認証システムを採用しています。このドキュメントでは、認証フローの詳細と実装方法について説明します。

## 🏗️ 認証アーキテクチャ

```
┌─────────────┐    ┌─────────────────┐    ┌──────────────────┐
│   Client    │    │ Consumer API    │    │ Inventory API    │
│ (Frontend)  │    │ (Port: 8080)    │    │ (Port: 8081)     │
└─────────────┘    └─────────────────┘    └──────────────────┘
       │                     │                        │
       ├─── 1. Login ────────►│                        │
       ◄──── Access/Refresh ──┤                        │
       │                     │                        │
       ├─── 2. API Call ──────►│                        │
       │   (with JWT)         │                        │
       │                     │                        │
       ├─── 3. Admin API ──────────────────────────────►│
       │   (with JWT)         │                        │
```

## 🚀 基本的な認証フロー

### 1️⃣ ユーザー登録

```bash
POST /api/users/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com", 
  "password": "SecurePassword123!",
  "fullName": "John Doe",
  "phoneNumber": "090-1234-5678"
}
```

#### レスポンス

```json
{
  "success": true,
  "message": "ユーザー登録が完了しました",
  "userId": 12345,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2️⃣ ログイン（トークン取得）

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePassword123!"
}
```

#### レスポンス

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshExpiresIn": 2592000,
  "user": {
    "id": 12345,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "CONSUMER",
    "fullName": "John Doe"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 3️⃣ 認証が必要なAPIの呼び出し

```bash
GET /api/cart
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4️⃣ トークン更新

```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 5️⃣ ログアウト

```bash
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## 🎭 ロールベースアクセス制御（RBAC）

### ロール階層

```
ADMIN
  └── MANAGER
      └── ANALYST
          └── CONSUMER
```

### ロール別権限

| ロール | Consumer API | Inventory API | 説明 |
|-------|-------------|---------------|------|
| **CONSUMER** | ✅ 全機能 | ❌ なし | 一般消費者：書籍購入など |
| **ANALYST** | ✅ 全機能 | ✅ 分析API | データ分析担当者 |
| **MANAGER** | ✅ 全機能 | ✅ 管理API | 店舗管理者：在庫・注文管理 |
| **ADMIN** | ✅ 全機能 | ✅ 全機能 | システム管理者：全権限 |

### エンドポイント別アクセス制御

#### Consumer API (8080)

| エンドポイント | CONSUMER | ANALYST | MANAGER | ADMIN |
|--------------|----------|---------|---------|-------|
| `/books/**` | ✅ | ✅ | ✅ | ✅ |
| `/api/auth/**` | ✅ | ✅ | ✅ | ✅ |
| `/api/cart/**` | ✅ | ❌ | ❌ | ✅ |
| `/api/orders/**` | ✅ | ✅ Read | ✅ | ✅ |
| `/api/users/profile` | ✅ Own | ✅ Own | ✅ Own | ✅ All |

#### Inventory API (8081)

| エンドポイント | CONSUMER | ANALYST | MANAGER | ADMIN |
|--------------|----------|---------|---------|-------|
| `/api/admin/books/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/inventory/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/orders/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/analytics/**` | ❌ | ✅ | ✅ | ✅ |

## 🔑 JWT トークンの構造

### Access Token

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "12345",
    "username": "john_doe",
    "role": "CONSUMER",
    "iat": 1642248000,
    "exp": 1642251600,
    "jti": "token-unique-id"
  }
}
```

### Refresh Token

```json
{
  "header": {
    "alg": "HS256", 
    "typ": "JWT"
  },
  "payload": {
    "sub": "12345",
    "type": "refresh",
    "iat": 1642248000,
    "exp": 1644840000,
    "jti": "refresh-unique-id"
  }
}
```

### トークン有効期限

| トークン種別 | 有効期限 | 用途 |
|------------|----------|------|
| **Access Token** | 1時間 | API認証 |
| **Refresh Token** | 30日 | Access Token更新 |

## 🔒 セキュリティ機能

### 1. パスワード要件

- **最小長さ**: 8文字
- **文字種類**: 英大文字・英小文字・数字を含む
- **禁止パターン**: 連続文字、辞書単語、個人情報

#### バリデーション例

```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,128}$
```

### 2. レート制限

| 認証状態 | 制限値 | 期間 | ブロック時間 |
|---------|-------|------|------------|
| 未認証 | 5回失敗 | 15分 | 30分 |
| 認証済み | 10回失敗 | 30分 | 60分 |

### 3. トークンブラックリスト

- ログアウト時にトークンを無効化
- セキュリティ違反検知時の即座無効化
- Redisで高速ブラックリスト管理

### 4. セキュリティヘッダー

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

## 💻 実装例

### JavaScript/Node.js

```javascript
class ReadscapeAuthClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.accessToken = null;
    this.refreshToken = null;
  }

  async login(username, password) {
    const response = await fetch(`${this.baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    if (response.ok) {
      const data = await response.json();
      this.accessToken = data.accessToken;
      this.refreshToken = data.refreshToken;
      return data;
    }
    
    throw new Error('Login failed');
  }

  async apiCall(endpoint, options = {}) {
    let response = await this._makeRequest(endpoint, options);
    
    // Token expired, try refresh
    if (response.status === 401) {
      await this.refreshAccessToken();
      response = await this._makeRequest(endpoint, options);
    }
    
    return response;
  }

  async _makeRequest(endpoint, options) {
    return fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      headers: {
        ...options.headers,
        'Authorization': `Bearer ${this.accessToken}`,
        'Content-Type': 'application/json'
      }
    });
  }

  async refreshAccessToken() {
    const response = await fetch(`${this.baseUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: this.refreshToken })
    });

    if (response.ok) {
      const data = await response.json();
      this.accessToken = data.accessToken;
      return data;
    }
    
    // Refresh failed, need re-login
    this.accessToken = null;
    this.refreshToken = null;
    throw new Error('Token refresh failed');
  }
}

// 使用例
const client = new ReadscapeAuthClient('http://localhost:8080');

try {
  await client.login('john_doe', 'SecurePassword123!');
  const cart = await client.apiCall('/api/cart');
  console.log(cart);
} catch (error) {
  console.error('Authentication failed:', error);
}
```

### Python

```python
import requests
import jwt
from datetime import datetime, timedelta

class ReadscapeAuthClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.access_token = None
        self.refresh_token = None
        
    def login(self, username, password):
        response = requests.post(f"{self.base_url}/api/auth/login", 
                               json={"username": username, "password": password})
        
        if response.status_code == 200:
            data = response.json()
            self.access_token = data["accessToken"]
            self.refresh_token = data["refreshToken"]
            return data
        
        raise Exception("Login failed")
    
    def api_call(self, endpoint, method="GET", **kwargs):
        headers = kwargs.get("headers", {})
        headers["Authorization"] = f"Bearer {self.access_token}"
        
        response = requests.request(method, f"{self.base_url}{endpoint}", 
                                  headers=headers, **kwargs)
        
        # Token expired, try refresh
        if response.status_code == 401:
            self.refresh_access_token()
            headers["Authorization"] = f"Bearer {self.access_token}"
            response = requests.request(method, f"{self.base_url}{endpoint}", 
                                      headers=headers, **kwargs)
        
        return response
    
    def refresh_access_token(self):
        response = requests.post(f"{self.base_url}/api/auth/refresh",
                               json={"refreshToken": self.refresh_token})
        
        if response.status_code == 200:
            data = response.json()
            self.access_token = data["accessToken"]
            return data
        
        # Refresh failed
        self.access_token = None
        self.refresh_token = None
        raise Exception("Token refresh failed")

# 使用例
client = ReadscapeAuthClient("http://localhost:8080")

try:
    client.login("john_doe", "SecurePassword123!")
    cart_response = client.api_call("/api/cart")
    print(cart_response.json())
except Exception as e:
    print(f"Authentication failed: {e}")
```

## 🛠️ トラブルシューティング

### よくある問題

#### 1. Token Expired (401)

```json
{
  "error": "TOKEN_EXPIRED",
  "message": "トークンの有効期限が切れています"
}
```

**解決策**: Refresh Tokenを使用してAccess Tokenを更新

#### 2. Invalid Token Format (401)

```json
{
  "error": "TOKEN_INVALID", 
  "message": "無効なトークンです"
}
```

**解決策**: 
- Bearer形式を確認: `Authorization: Bearer <token>`
- トークンの完全性を確認

#### 3. Insufficient Role (403)

```json
{
  "error": "ACCESS_DENIED",
  "message": "このリソースへのアクセス権限がありません"
}
```

**解決策**: 必要なロール権限を持つユーザーでログイン

### デバッグ方法

#### JWTトークンのデコード

```bash
# オンラインツール: https://jwt.io
# またはコマンドライン
echo "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." | base64 -d
```

#### ネットワーク監視

```bash
# リクエストヘッダーの確認
curl -v -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/cart
```

## 📞 サポート

認証関連の問題については：

- **技術サポート**: auth-support@readscape.jp
- **セキュリティ関連**: security@readscape.jp
- **GitHub Issues**: https://github.com/readscape-jp/auth-issues

---

**Last Updated**: 2024年1月15日  
**Version**: 1.0.0