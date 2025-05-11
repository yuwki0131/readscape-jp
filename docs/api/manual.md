
# **Readscape-JP エンドユーザー向けAPI 仕様書**

## **1. 書籍閲覧と検索機能**

### **GET /api/books**
書籍の一覧を取得します。

- **クエリパラメータ**:
  - `category` (オプション): カテゴリでフィルタリングします。
  - `keyword` (オプション): 書籍名または著者名で検索します。
  - `page` (オプション, デフォルト: 0): ページ番号。
  - `size` (オプション, デフォルト: 10): ページあたりの書籍数。

- **レスポンス例**:
  ```json
  [
    {
      "id": 1,
      "title": "Example Book",
      "author": "Author Name",
      "price": 1200,
      "category": "Fiction",
      "rating": 4.5
    }
  ]
  ```

---

### **GET /api/books/{id}**
指定した書籍の詳細情報を取得します。

- **パスパラメータ**:
  - `id` (必須): 書籍のID。

- **レスポンス例**:
  ```json
  {
    "id": 1,
    "title": "Example Book",
    "author": "Author Name",
    "price": 1200,
    "category": "Fiction",
    "description": "This is an example book description.",
    "rating": 4.5,
    "reviews": [
      {
        "id": 101,
        "user": "User1",
        "rating": 5,
        "comment": "Great book!"
      }
    ]
  }
  ```

---

## **2. カート機能**

### **GET /api/cart**
カートの内容を取得します。

- **レスポンス例**:
  ```json
  {
    "items": [
      {
        "bookId": 1,
        "title": "Example Book",
        "price": 1200,
        "quantity": 2
      }
    ],
    "totalPrice": 2400
  }
  ```

---

### **POST /api/cart**
カートに書籍を追加します。

- **リクエストボディ例**:
  ```json
  {
    "bookId": 1,
    "quantity": 2
  }
  ```

- **レスポンス例**:
  ```json
  {
    "message": "Book added to cart successfully."
  }
  ```

---

### **PUT /api/cart/{bookId}**
カート内の書籍の数量を変更します。

- **パスパラメータ**:
  - `bookId` (必須): 書籍のID。

- **リクエストボディ例**:
  ```json
  {
    "quantity": 3
  }
  ```

- **レスポンス例**:
  ```json
  {
    "message": "Book quantity updated successfully."
  }
  ```

---

### **DELETE /api/cart/{bookId}**
カートから書籍を削除します。

- **パスパラメータ**:
  - `bookId` (必須): 書籍のID。

- **レスポンス例**:
  ```json
  {
    "message": "Book removed from cart successfully."
  }
  ```

---

## **3. 注文機能**

### **POST /api/orders**
カート内の商品を基に注文を作成します。

- **リクエストボディ例**:
  ```json
  {
    "paymentMethod": "credit_card",
    "address": "123 Main Street, Tokyo"
  }
  ```

- **レスポンス例**:
  ```json
  {
    "orderId": 1001,
    "message": "Order placed successfully."
  }
  ```

---

### **GET /api/orders**
ログイン中のユーザーの注文履歴を取得します。

- **レスポンス例**:
  ```json
  [
    {
      "orderId": 1001,
      "date": "2024-11-17",
      "totalPrice": 2400,
      "status": "Processing"
    }
  ]
  ```

---

### **GET /api/orders/{id}**
指定した注文の詳細を取得します。

- **パスパラメータ**:
  - `id` (必須): 注文ID。

- **レスポンス例**:
  ```json
  {
    "orderId": 1001,
    "date": "2024-11-17",
    "totalPrice": 2400,
    "status": "Processing",
    "items": [
      {
        "bookId": 1,
        "title": "Example Book",
        "price": 1200,
        "quantity": 2
      }
    ]
  }
  ```

---

## **4. ユーザー認証・管理**

### **POST /api/users/register**
新しいユーザーを登録します。

- **リクエストボディ例**:
  ```json
  {
    "username": "example_user",
    "email": "user@example.com",
    "password": "password123"
  }
  ```

- **レスポンス例**:
  ```json
  {
    "message": "User registered successfully."
  }
  ```

---

### **POST /api/users/login**
ユーザーを認証します。

- **リクエストボディ例**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```

- **レスポンス例**:
  ```json
  {
    "token": "jwt-token-string"
  }
  ```

---

### **GET /api/users/profile**
ログイン中のユーザーのプロフィール情報を取得します。

- **レスポンス例**:
  ```json
  {
    "username": "example_user",
    "email": "user@example.com"
  }
  ```

---

### **PUT /api/users/profile**
ユーザーのプロフィール情報を更新します。

- **リクエストボディ例**:
  ```json
  {
    "username": "updated_user",
    "email": "updated_user@example.com"
  }
  ```

- **レスポンス例**:
  ```json
  {
    "message": "Profile updated successfully."
  }
  ```

---

## **5. レビュー機能**

### **GET /api/books/{bookId}/reviews**
指定した書籍のレビュー一覧を取得します。

- **パスパラメータ**:
  - `bookId` (必須): 書籍のID。

- **レスポンス例**:
  ```json
  [
    {
      "id": 101,
      "user": "User1",
      "rating": 5,
      "comment": "Great book!"
    }
  ]
  ```

---

### **POST /api/books/{bookId}/reviews**
書籍にレビューを投稿します。

- **パスパラメータ**:
  - `bookId` (必須): 書籍のID。

- **リクエストボディ例**:
  ```json
  {
    "rating": 5,
    "comment": "Amazing book!"
  }
  ```

- **レスポンス例**:
  ```json
  {
    "message": "Review submitted successfully."
  }
  ```
