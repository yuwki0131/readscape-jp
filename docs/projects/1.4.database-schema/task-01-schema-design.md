# タスク01: データベーススキーマ設計

## タスク概要
PostgreSQL を使用したReadscape-JP のデータベーススキーマ設計と基本テーブル構造の作成を行います。

## 実装内容

### 1. スキーマ作成
```sql
-- V0001__create_schema_readscape.sql
CREATE SCHEMA IF NOT EXISTS readscape;
```

### 2. 基本テーブル作成
```sql
-- V0002__create_table_books.sql
CREATE TABLE readscape.books (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    isbn VARCHAR(13) UNIQUE,
    price INTEGER NOT NULL CHECK (price >= 0),
    category_id INTEGER,
    description TEXT,
    stock_quantity INTEGER DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V0003__create_table_users.sql
CREATE TABLE readscape.users (
    id SERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'CONSUMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3. インデックス設計
```sql
-- インデックス作成
CREATE INDEX idx_books_title ON readscape.books USING gin(to_tsvector('english', title));
CREATE INDEX idx_books_author ON readscape.books(author);
CREATE INDEX idx_books_category_id ON readscape.books(category_id);
CREATE INDEX idx_users_email ON readscape.users(email);
```

## 受け入れ条件
- [ ] readscape スキーマが作成される
- [ ] 全必要テーブルが作成される
- [ ] 適切な制約が設定されている
- [ ] インデックスが最適に設計されている
- [ ] データ型が適切に選択されている
- [ ] 外部キー制約が正しく設定されている

## 関連ファイル
- `src/main/resources/db/migration/V0001__create_schema_readscape.sql`
- `src/main/resources/db/migration/V0002__create_table_books.sql`
- `src/main/resources/db/migration/V0003__create_table_users.sql`