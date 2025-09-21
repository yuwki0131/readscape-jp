# Readscape-JP API トラブルシューティング

## 概要

このガイドでは、Readscape-JP APIを使用する際によく発生する問題と解決方法を説明します。

## 認証関連の問題

### 1. 401 Unauthorized エラー

#### 症状
```json
{
  "error": "AUTHENTICATION_ERROR",
  "message": "認証が必要です",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/users/profile"
}
```

#### 原因と解決方法

**原因1**: トークンが設定されていない
```javascript
// 問題のあるコード
fetch('/api/users/profile');

// 正しいコード
fetch('/api/users/profile', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});
```

**原因2**: トークンの形式が正しくない
```javascript
// 問題のあるコード
'Authorization': `${token}`

// 正しいコード
'Authorization': `Bearer ${token}`
```

**原因3**: トークンの有効期限切れ
```javascript
// 解決方法: リフレッシュトークンを使用
async function refreshToken() {
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ 
      refreshToken: localStorage.getItem('refreshToken') 
    })
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('token', data.token);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data.token;
  }
  
  throw new Error('トークンの更新に失敗しました');
}
```

### 2. 403 Forbidden エラー

#### 症状
```json
{
  "error": "AUTHORIZATION_ERROR",
  "message": "この操作を実行する権限がありません",
  "status": 403,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/admin/books/1"
}
```

#### 解決方法

1. **Consumer API**: 適切なユーザー認証が必要
2. **Inventory Management API**: ADMIN または MANAGER 権限が必要

```javascript
// 権限確認の例
function checkUserRole(requiredRole) {
  const token = localStorage.getItem('token');
  if (!token) return false;
  
  const payload = JSON.parse(atob(token.split('.')[1]));
  return payload.roles?.includes(requiredRole);
}

if (!checkUserRole('ADMIN')) {
  alert('管理者権限が必要です');
  return;
}
```

## ネットワーク関連の問題

### 3. CORS エラー

#### 症状
```
Access to fetch at 'http://localhost:8080/api/books' from origin 'http://localhost:3000' 
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present 
on the requested resource.
```

#### 解決方法

**開発環境での対処**:
1. プロキシ設定を使用（React）
```json
// package.json
{
  "name": "your-app",
  "version": "0.1.0",
  "proxy": "http://localhost:8080",
  ...
}
```

2. または、APIサーバーのCORS設定を確認

**本番環境での対処**:
- 同一オリジンからのリクエストを行う
- APIサーバーで適切なCORS設定を行う

### 4. ネットワーク接続エラー

#### 症状
```javascript
TypeError: Failed to fetch
```

#### 解決方法

```javascript
async function apiCallWithRetry(url, options, retries = 3) {
  for (let i = 0; i < retries; i++) {
    try {
      const response = await fetch(url, options);
      
      if (response.ok) {
        return response;
      }
      
      if (response.status >= 500) {
        throw new Error(`Server error: ${response.status}`);
      }
      
      // 4xx エラーの場合はリトライしない
      return response;
      
    } catch (error) {
      if (i === retries - 1) throw error;
      
      // 指数バックオフでリトライ
      await new Promise(resolve => 
        setTimeout(resolve, Math.pow(2, i) * 1000)
      );
    }
  }
}
```

## データ関連の問題

### 5. 400 Bad Request - バリデーションエラー

#### 症状
```json
{
  "error": "VALIDATION_ERROR",
  "message": "入力値に誤りがあります",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/cart",
  "fieldErrors": [
    {
      "field": "quantity",
      "message": "数量は1以上99以下で入力してください"
    }
  ]
}
```

#### 解決方法

```javascript
// バリデーション関数の実装
function validateCartItem(bookId, quantity) {
  const errors = [];
  
  if (!bookId || !Number.isInteger(bookId) || bookId <= 0) {
    errors.push('有効な書籍IDを指定してください');
  }
  
  if (!quantity || !Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
    errors.push('数量は1以上99以下で入力してください');
  }
  
  return errors;
}

// 使用例
function addToCart(bookId, quantity) {
  const errors = validateCartItem(bookId, quantity);
  
  if (errors.length > 0) {
    alert(errors.join('\n'));
    return;
  }
  
  // API呼び出し
  // ...
}
```

### 6. 404 Not Found エラー

#### 症状
```json
{
  "error": "RESOURCE_NOT_FOUND",
  "message": "書籍が見つかりません",
  "status": 404,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/books/999"
}
```

#### 解決方法

```javascript
async function getBook(bookId) {
  try {
    const response = await fetch(`/api/books/${bookId}`);
    
    if (response.status === 404) {
      // ユーザーフレンドリーなエラーハンドリング
      showNotification('指定された書籍は見つかりませんでした', 'warning');
      return null;
    }
    
    if (response.ok) {
      return await response.json();
    }
    
    throw new Error('書籍の取得に失敗しました');
    
  } catch (error) {
    console.error('API Error:', error);
    showNotification('書籍情報の取得中にエラーが発生しました', 'error');
    return null;
  }
}
```

### 7. 409 Conflict エラー

#### 症状
```json
{
  "error": "DUPLICATE_RESOURCE",
  "message": "このISBNは既に登録されています",
  "status": 409,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/admin/books"
}
```

#### 解決方法

```javascript
async function createBook(bookData) {
  try {
    const response = await fetch('/api/admin/books', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(bookData)
    });
    
    if (response.status === 409) {
      const error = await response.json();
      alert(`登録エラー: ${error.message}`);
      return null;
    }
    
    if (response.ok) {
      return await response.json();
    }
    
    throw new Error('書籍の登録に失敗しました');
    
  } catch (error) {
    console.error('Book creation error:', error);
    throw error;
  }
}
```

## パフォーマンス関連の問題

### 8. レスポンス時間が遅い

#### 原因と解決方法

**原因1**: 大量のデータを一度に取得している
```javascript
// 問題のあるコード
const books = await fetch('/api/books?size=1000');

// 改善されたコード
const books = await fetch('/api/books?size=20&page=0');
```

**原因2**: 不必要な重複リクエスト
```javascript
// リクエストキャッシュの実装
class ApiCache {
  constructor(ttl = 300000) { // 5分間キャッシュ
    this.cache = new Map();
    this.ttl = ttl;
  }
  
  async get(key, fetcher) {
    const cached = this.cache.get(key);
    
    if (cached && Date.now() - cached.timestamp < this.ttl) {
      return cached.data;
    }
    
    const data = await fetcher();
    this.cache.set(key, {
      data: data,
      timestamp: Date.now()
    });
    
    return data;
  }
  
  invalidate(key) {
    this.cache.delete(key);
  }
}

// 使用例
const apiCache = new ApiCache();

async function getCachedBooks(category) {
  return apiCache.get(`books-${category}`, async () => {
    const response = await fetch(`/api/books?category=${category}`);
    return response.json();
  });
}
```

## データ形式の問題

### 9. 日付形式の問題

#### 問題
```javascript
// 問題: タイムゾーンの不一致
const date = new Date('2024-01-15');
console.log(date); // 環境によって異なる結果
```

#### 解決方法

```javascript
// 日付処理のユーティリティ
class DateUtils {
  // ISO形式の日付文字列を日本時間で表示
  static formatDateJST(isoString) {
    const date = new Date(isoString);
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Asia/Tokyo'
    }).format(date);
  }
  
  // 日付のみを表示
  static formatDateOnly(isoString) {
    const date = new Date(isoString);
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(date);
  }
  
  // 相対時間の表示
  static formatRelativeTime(isoString) {
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now - date;
    
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffMinutes < 60) {
      return `${diffMinutes}分前`;
    } else if (diffHours < 24) {
      return `${diffHours}時間前`;
    } else if (diffDays < 30) {
      return `${diffDays}日前`;
    } else {
      return this.formatDateOnly(isoString);
    }
  }
}
```

## デバッグのためのユーティリティ

### APIリクエストロガー

```javascript
// デバッグ用のAPIクライアント
class DebugApiClient {
  constructor(baseUrl, enableLogging = false) {
    this.baseUrl = baseUrl;
    this.enableLogging = enableLogging;
  }
  
  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const startTime = Date.now();
    
    if (this.enableLogging) {
      console.group(`🌐 API Request: ${options.method || 'GET'} ${endpoint}`);
      console.log('URL:', url);
      console.log('Headers:', options.headers);
      console.log('Body:', options.body);
    }
    
    try {
      const response = await fetch(url, options);
      const duration = Date.now() - startTime;
      
      if (this.enableLogging) {
        console.log(`✅ Response: ${response.status} (${duration}ms)`);
        
        if (!response.ok) {
          const errorText = await response.text();
          console.error('Error response:', errorText);
        }
        console.groupEnd();
      }
      
      return response;
      
    } catch (error) {
      const duration = Date.now() - startTime;
      
      if (this.enableLogging) {
        console.error(`❌ Request failed (${duration}ms):`, error);
        console.groupEnd();
      }
      
      throw error;
    }
  }
}

// 使用例
const apiClient = new DebugApiClient(
  'http://localhost:8080/api',
  process.env.NODE_ENV === 'development'
);
```

### エラー監視

```javascript
// エラー監視とレポーティング
class ErrorMonitor {
  constructor() {
    this.errors = [];
  }
  
  logError(error, context = {}) {
    const errorInfo = {
      timestamp: new Date().toISOString(),
      message: error.message,
      stack: error.stack,
      url: window.location.href,
      userAgent: navigator.userAgent,
      context: context
    };
    
    this.errors.push(errorInfo);
    console.error('Logged error:', errorInfo);
    
    // 本番環境では外部サービスに送信
    if (process.env.NODE_ENV === 'production') {
      this.sendToErrorService(errorInfo);
    }
  }
  
  async sendToErrorService(errorInfo) {
    try {
      await fetch('/api/errors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(errorInfo)
      });
    } catch (sendError) {
      console.error('Failed to send error to service:', sendError);
    }
  }
  
  getErrorSummary() {
    const errorCounts = {};
    
    this.errors.forEach(error => {
      const key = error.message;
      errorCounts[key] = (errorCounts[key] || 0) + 1;
    });
    
    return errorCounts;
  }
}

// グローバルエラーハンドラー
const errorMonitor = new ErrorMonitor();

window.addEventListener('error', (event) => {
  errorMonitor.logError(event.error, {
    filename: event.filename,
    lineno: event.lineno,
    colno: event.colno
  });
});

// Promise rejection のキャッチ
window.addEventListener('unhandledrejection', (event) => {
  errorMonitor.logError(event.reason, {
    type: 'unhandledPromiseRejection'
  });
});
```

## サポート情報

### お問い合わせ先

- **技術サポート**: api-support@readscape.jp
- **GitHub Issues**: https://github.com/readscape-jp/api-issues
- **開発者フォーラム**: https://forum.readscape.jp/api

### レポート作成時に含める情報

1. **エラーメッセージ**: 完全なエラーメッセージとスタックトレース
2. **リクエスト情報**: URL、メソッド、ヘッダー、ボディ
3. **環境情報**: ブラウザ、OS、Node.jsバージョンなど
4. **再現手順**: 問題を再現するための詳細な手順
5. **期待する動作**: 正常時に期待される動作

### 例：エラーレポートテンプレート

```
## 問題の概要
[問題の簡潔な説明]

## 環境
- OS: [例: macOS 14.0]
- ブラウザ: [例: Chrome 120.0]
- Node.js: [例: v18.17.0]
- API Version: [例: v1.0.0]

## 再現手順
1. [手順1]
2. [手順2]
3. [手順3]

## 期待する動作
[正常時に期待される動作]

## 実際の動作
[実際に発生している問題]

## エラーメッセージ
```
[完全なエラーメッセージ]
```

## 追加情報
[その他の関連情報]
```