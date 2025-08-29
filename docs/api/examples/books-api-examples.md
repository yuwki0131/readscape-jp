# 書籍API利用例

## 概要

書籍APIは、Readscape-JPの書籍情報の検索・取得機能を提供します。認証は不要で、一般に公開されているAPIです。

## エンドポイント一覧

| HTTP メソッド | エンドポイント | 説明 |
|---|---|---|
| GET | `/books` | 書籍一覧取得 |
| GET | `/books/{id}` | 書籍詳細取得 |
| GET | `/books/search` | 書籍検索 |
| GET | `/books/categories` | カテゴリー一覧取得 |
| GET | `/books/popular` | 人気書籍一覧取得 |
| GET | `/books/top-rated` | 高評価書籍一覧取得 |
| GET | `/books/in-stock` | 在庫有り書籍一覧取得 |
| GET | `/books/isbn/{isbn}` | ISBN検索 |

## 1. 書籍一覧取得

### 基本的な使用例

```bash
# 全書籍一覧の取得（デフォルト：最初の10件）
curl http://localhost:8080/api/books
```

### パラメータを指定した検索

```bash
# カテゴリーで絞り込み
curl "http://localhost:8080/api/books?category=技術書"

# ページング指定
curl "http://localhost:8080/api/books?page=1&size=20"

# ソート指定（価格の安い順）
curl "http://localhost:8080/api/books?sortBy=price_asc"

# 複数条件の組み合わせ
curl "http://localhost:8080/api/books?category=小説&page=0&size=5&sortBy=rating"
```

### JavaScript例

```javascript
// 書籍一覧取得関数
async function getBooks(params = {}) {
  const queryString = new URLSearchParams(params).toString();
  const response = await fetch(`http://localhost:8080/api/books?${queryString}`);
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('書籍一覧の取得に失敗しました');
  }
}

// 使用例
const books = await getBooks({
  category: '技術書',
  page: 0,
  size: 10,
  sortBy: 'newest'
});

console.log(`${books.pagination.totalElements}件の書籍が見つかりました`);
books.books.forEach(book => {
  console.log(`${book.title} - ${book.author} (¥${book.price})`);
});
```

### レスポンス例

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
    },
    {
      "id": 2,
      "title": "Java設計パターン",
      "author": "佐藤花子",
      "price": 2800,
      "category": "技術書",
      "rating": 4.2,
      "reviewCount": 18,
      "imageUrl": "https://images.readscape.jp/books/2.jpg",
      "inStock": false
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 47,
    "size": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## 2. 書籍詳細取得

### 基本的な使用例

```bash
# ID指定での書籍詳細取得
curl http://localhost:8080/api/books/1
```

### JavaScript例

```javascript
async function getBookDetail(bookId) {
  const response = await fetch(`http://localhost:8080/api/books/${bookId}`);
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 404) {
    throw new Error('書籍が見つかりませんでした');
  } else {
    throw new Error('書籍詳細の取得に失敗しました');
  }
}

// 使用例
try {
  const book = await getBookDetail(1);
  console.log(`書籍名: ${book.title}`);
  console.log(`著者: ${book.author}`);
  console.log(`価格: ¥${book.price}`);
  console.log(`在庫: ${book.stockQuantity}冊`);
} catch (error) {
  console.error('エラー:', error.message);
}
```

### レスポンス例

```json
{
  "id": 1,
  "title": "Spring Boot実践入門",
  "author": "山田太郎",
  "price": 3200,
  "category": "技術書",
  "rating": 4.5,
  "reviewCount": 25,
  "imageUrl": "https://images.readscape.jp/books/1.jpg",
  "inStock": true,
  "isbn": "9784000000001",
  "description": "Spring Bootの基礎から実践的な使い方まで詳しく解説した書籍です。初心者から中級者まで幅広く対応しています。",
  "publicationDate": "2024-01-15",
  "publisher": "技術出版社",
  "pages": 450,
  "stockQuantity": 15,
  "tags": ["Spring", "Java", "フレームワーク", "Web開発"]
}
```

## 3. 書籍検索

### キーワード検索

```bash
# タイトル・著者名での検索
curl "http://localhost:8080/api/books/search?q=Java"

# カテゴリーを絞り込んでの検索
curl "http://localhost:8080/api/books/search?q=Spring&category=技術書"

# 検索結果のソート（関連度順）
curl "http://localhost:8080/api/books/search?q=プログラミング&sortBy=relevance"
```

### JavaScript例

```javascript
async function searchBooks(keyword, options = {}) {
  const params = {
    q: keyword,
    page: options.page || 0,
    size: options.size || 10,
    sortBy: options.sortBy || 'relevance'
  };
  
  if (options.category) {
    params.category = options.category;
  }
  
  const queryString = new URLSearchParams(params).toString();
  const response = await fetch(`http://localhost:8080/api/books/search?${queryString}`);
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('書籍検索に失敗しました');
  }
}

// 使用例
const searchResults = await searchBooks('Java', {
  category: '技術書',
  sortBy: 'rating'
});

console.log(`"Java"の検索結果: ${searchResults.pagination.totalElements}件`);
```

## 4. カテゴリー一覧取得

### 基本的な使用例

```bash
curl http://localhost:8080/api/books/categories
```

### JavaScript例

```javascript
async function getCategories() {
  const response = await fetch('http://localhost:8080/api/books/categories');
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('カテゴリー一覧の取得に失敗しました');
  }
}

// 使用例
const categories = await getCategories();
console.log('利用可能なカテゴリー:', categories.join(', '));

// HTMLセレクトボックスに動的に追加
const selectElement = document.getElementById('categorySelect');
categories.forEach(category => {
  const option = document.createElement('option');
  option.value = category;
  option.textContent = category;
  selectElement.appendChild(option);
});
```

### レスポンス例

```json
[
  "技術書",
  "小説",
  "ビジネス書",
  "歴史",
  "科学",
  "料理",
  "旅行",
  "趣味・実用",
  "健康",
  "教育"
]
```

## 5. 人気書籍一覧取得

### 基本的な使用例

```bash
# デフォルト（10件）
curl http://localhost:8080/api/books/popular

# 件数指定
curl "http://localhost:8080/api/books/popular?limit=20"
```

### JavaScript例

```javascript
async function getPopularBooks(limit = 10) {
  const response = await fetch(`http://localhost:8080/api/books/popular?limit=${limit}`);
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('人気書籍一覧の取得に失敗しました');
  }
}

// 人気書籍ランキング表示
const popularBooks = await getPopularBooks(5);
console.log('人気書籍トップ5:');
popularBooks.forEach((book, index) => {
  console.log(`${index + 1}. ${book.title} (レビュー${book.reviewCount}件)`);
});
```

## 6. 高評価書籍一覧取得

### 基本的な使用例

```bash
curl "http://localhost:8080/api/books/top-rated?limit=15"
```

### JavaScript例

```javascript
async function getTopRatedBooks(limit = 10) {
  const response = await fetch(`http://localhost:8080/api/books/top-rated?limit=${limit}`);
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('高評価書籍一覧の取得に失敗しました');
  }
}

// おすすめ書籍の表示
const topRatedBooks = await getTopRatedBooks(5);
console.log('評価の高いおすすめ書籍:');
topRatedBooks.forEach(book => {
  console.log(`${book.title} - 評価: ${book.rating}/5.0`);
});
```

## 7. 在庫有り書籍一覧取得

### 基本的な使用例

```bash
curl "http://localhost:8080/api/books/in-stock?page=0&size=20"
```

### JavaScript例

```javascript
async function getBooksInStock(page = 0, size = 10) {
  const response = await fetch(`http://localhost:8080/api/books/in-stock?page=${page}&size=${size}`);
  
  if (response.ok) {
    return await response.json();
  } else {
    throw new Error('在庫書籍一覧の取得に失敗しました');
  }
}

// 即座に購入可能な書籍の表示
const availableBooks = await getBooksInStock();
console.log(`現在購入可能な書籍: ${availableBooks.pagination.totalElements}冊`);
```

## 8. ISBN検索

### 基本的な使用例

```bash
curl http://localhost:8080/api/books/isbn/9784000000001
```

### JavaScript例

```javascript
async function getBookByIsbn(isbn) {
  const response = await fetch(`http://localhost:8080/api/books/isbn/${isbn}`);
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 404) {
    throw new Error('指定されたISBNの書籍は見つかりませんでした');
  } else {
    throw new Error('ISBN検索に失敗しました');
  }
}

// バーコードスキャナーでの書籍検索
async function handleBarcodeScanned(scannedIsbn) {
  try {
    const book = await getBookByIsbn(scannedIsbn);
    console.log(`書籍が見つかりました: ${book.title}`);
    // 書籍詳細ページに移動など
  } catch (error) {
    console.error('書籍検索エラー:', error.message);
  }
}
```

## 複合的な利用例

### React.jsでの書籍検索コンポーネント

```jsx
import React, { useState, useEffect } from 'react';

const BookSearch = () => {
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [sortBy, setSortBy] = useState('newest');
  const [loading, setLoading] = useState(false);

  // カテゴリー一覧を取得
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/books/categories');
        const data = await response.json();
        setCategories(data);
      } catch (error) {
        console.error('カテゴリー取得エラー:', error);
      }
    };
    fetchCategories();
  }, []);

  // 書籍検索
  const searchBooks = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        sortBy: sortBy
      });
      
      if (searchTerm) {
        params.append('q', searchTerm);
      }
      if (selectedCategory) {
        params.append('category', selectedCategory);
      }

      const endpoint = searchTerm ? '/books/search' : '/books';
      const response = await fetch(`http://localhost:8080/api${endpoint}?${params}`);
      const data = await response.json();
      setBooks(data.books || data);
    } catch (error) {
      console.error('検索エラー:', error);
    } finally {
      setLoading(false);
    }
  };

  // 検索実行
  const handleSearch = (e) => {
    e.preventDefault();
    searchBooks();
  };

  return (
    <div className="book-search">
      <form onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="書籍名または著者名で検索..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        
        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
        >
          <option value="">全カテゴリー</option>
          {categories.map(category => (
            <option key={category} value={category}>{category}</option>
          ))}
        </select>
        
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
        >
          <option value="newest">新着順</option>
          <option value="title">タイトル順</option>
          <option value="price_asc">価格の安い順</option>
          <option value="price_desc">価格の高い順</option>
          <option value="rating">評価の高い順</option>
        </select>
        
        <button type="submit" disabled={loading}>
          {loading ? '検索中...' : '検索'}
        </button>
      </form>

      <div className="book-list">
        {books.map(book => (
          <div key={book.id} className="book-item">
            <img src={book.imageUrl} alt={book.title} />
            <h3>{book.title}</h3>
            <p>著者: {book.author}</p>
            <p>価格: ¥{book.price.toLocaleString()}</p>
            <p>評価: {book.rating}/5.0 ({book.reviewCount}件)</p>
            <p className={book.inStock ? 'in-stock' : 'out-of-stock'}>
              {book.inStock ? '在庫あり' : '在庫切れ'}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default BookSearch;
```

## エラーハンドリング

### よくあるエラーと対処法

```javascript
async function handleApiCall(apiFunction, ...args) {
  try {
    return await apiFunction(...args);
  } catch (error) {
    if (error.message.includes('fetch')) {
      console.error('ネットワークエラー: インターネット接続を確認してください');
    } else if (error.message.includes('404')) {
      console.error('リソースが見つかりません');
    } else {
      console.error('予期しないエラーが発生しました:', error.message);
    }
    throw error;
  }
}

// 使用例
try {
  const books = await handleApiCall(getBooks, { category: '技術書' });
  console.log('書籍データを取得しました');
} catch (error) {
  // エラー処理（ユーザーへの通知など）
}
```