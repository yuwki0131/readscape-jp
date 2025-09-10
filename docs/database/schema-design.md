# データベーススキーマ設計書

## 概要

Readscape-JPのデータベース設計は、書籍販売システムの要件を満たすために正規化された関係データベース構造を採用しています。PostgreSQL 15を使用し、ACID特性を活用した堅牢なデータ管理を実現します。

## 設計原則

### 1. 正規化原則
- **第3正規形（3NF）** までを基本とする
- データの冗長性を排除
- 更新異常を防止
- パフォーマンス要件に応じて非正規化を検討

### 2. 制約設計原則
- 主キーは必須（SERIAL型推奨）
- 外部キー制約による参照整合性確保
- NOT NULL制約の適切な設定
- CHECK制約によるデータ品質保証

### 3. パフォーマンス設計原則
- 検索頻度の高いカラムにインデックス作成
- 複合インデックスの適切な設計
- パーティショニングの検討（将来拡張）

## 全体ER図

```mermaid
erDiagram
    users ||--o{ orders : "places"
    users ||--o{ reviews : "writes"  
    users ||--|| carts : "has"
    users ||--o{ inventory_transactions : "performs"
    
    books ||--o{ order_items : "contains"
    books ||--o{ cart_items : "added_to"
    books ||--o{ reviews : "receives"
    books ||--o{ inventory_transactions : "affects"
    
    orders ||--o{ order_items : "contains"
    carts ||--o{ cart_items : "contains"
    
    categories ||--o{ books : "classifies"
    
    users {
        bigserial id PK
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar name "NOT NULL"
        varchar role "DEFAULT 'USER'"
        timestamp created_at "DEFAULT NOW()"
        timestamp updated_at "DEFAULT NOW()"
        boolean is_active "DEFAULT TRUE"
    }
    
    books {
        bigserial id PK
        varchar title "NOT NULL"
        varchar author "NOT NULL"
        varchar isbn UK "NOT NULL"
        bigint category_id FK "NOT NULL"
        integer price "NOT NULL CHECK (price > 0)"
        text description
        date publication_date "NOT NULL"
        varchar publisher "NOT NULL"
        integer pages "CHECK (pages > 0)"
        integer stock_quantity "DEFAULT 0 CHECK (stock_quantity >= 0)"
        varchar status "DEFAULT 'ACTIVE'"
        varchar image_url
        timestamp created_at "DEFAULT NOW()"
        timestamp updated_at "DEFAULT NOW()"
        bigint created_by FK
        bigint updated_by FK
    }
    
    categories {
        bigserial id PK
        varchar name UK "NOT NULL"
        varchar description
        boolean is_active "DEFAULT TRUE"
        timestamp created_at "DEFAULT NOW()"
    }
    
    orders {
        bigserial id PK
        varchar order_number UK "NOT NULL"
        bigint user_id FK "NOT NULL"
        varchar status "NOT NULL DEFAULT 'PENDING'"
        integer total_price "NOT NULL CHECK (total_price > 0)"
        text shipping_address "NOT NULL"
        varchar payment_method "NOT NULL"
        timestamp order_date "DEFAULT NOW()"
        date estimated_delivery
        timestamp shipped_at
        timestamp delivered_at
        text notes
        timestamp created_at "DEFAULT NOW()"
        timestamp updated_at "DEFAULT NOW()"
    }
    
    order_items {
        bigserial id PK
        bigint order_id FK "NOT NULL"
        bigint book_id FK "NOT NULL"
        integer quantity "NOT NULL CHECK (quantity > 0)"
        integer unit_price "NOT NULL CHECK (unit_price > 0)"
        integer total_price "NOT NULL CHECK (total_price > 0)"
        timestamp created_at "DEFAULT NOW()"
    }
    
    carts {
        bigserial user_id PK FK
        timestamp updated_at "DEFAULT NOW()"
    }
    
    cart_items {
        bigserial id PK
        bigint cart_user_id FK "NOT NULL"
        bigint book_id FK "NOT NULL"
        integer quantity "NOT NULL CHECK (quantity > 0 AND quantity <= 99)"
        timestamp added_at "DEFAULT NOW()"
        timestamp updated_at "DEFAULT NOW()"
    }
    
    reviews {
        bigserial id PK
        bigint user_id FK "NOT NULL"
        bigint book_id FK "NOT NULL"
        integer rating "NOT NULL CHECK (rating >= 1 AND rating <= 5)"
        text comment
        boolean is_verified "DEFAULT FALSE"
        timestamp created_at "DEFAULT NOW()"
        timestamp updated_at "DEFAULT NOW()"
    }
    
    inventory_transactions {
        bigserial id PK
        bigint book_id FK "NOT NULL"
        varchar transaction_type "NOT NULL"
        integer quantity "NOT NULL"
        integer previous_stock "NOT NULL"
        integer new_stock "NOT NULL"
        text reason
        bigint created_by FK
        timestamp created_at "DEFAULT NOW()"
    }
```

## テーブル詳細設計

### 1. users（ユーザー）

書籍購入システムのユーザー情報を管理します。

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    phone VARCHAR(20),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'PREMIUM', 'ADMIN', 'MANAGER')),
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- インデックス
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_email_verification_token ON users(email_verification_token);
```

#### フィールド説明

| フィールド名 | 型 | 制約 | 説明 |
|------------|-----|------|------|
| id | BIGSERIAL | PK | ユーザーID（自動採番） |
| email | VARCHAR(255) | NOT NULL, UNIQUE | メールアドレス（ログインID） |
| password_hash | VARCHAR(255) | NOT NULL | パスワードハッシュ（bcrypt） |
| name | VARCHAR(100) | NOT NULL | ユーザー名 |
| role | VARCHAR(20) | NOT NULL | ユーザーロール |
| is_active | BOOLEAN | NOT NULL | アクティブフラグ |
| email_verified | BOOLEAN | NOT NULL | メール認証フラグ |

### 2. books（書籍）

書籍の基本情報を管理するマスターテーブルです。

```sql
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(13) NOT NULL UNIQUE,
    category_id BIGINT NOT NULL,
    price INTEGER NOT NULL,
    description TEXT,
    publication_date DATE NOT NULL,
    publisher VARCHAR(100) NOT NULL,
    pages INTEGER,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER NOT NULL DEFAULT 10,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    
    -- 制約
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_books_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_books_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_books_price CHECK (price > 0),
    CONSTRAINT chk_books_pages CHECK (pages > 0),
    CONSTRAINT chk_books_stock CHECK (stock_quantity >= 0),
    CONSTRAINT chk_books_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    CONSTRAINT chk_books_isbn_format CHECK (isbn ~ '^97[8-9][0-9]{10}$')
);

-- インデックス
CREATE INDEX idx_books_title ON books USING GIN(to_tsvector('japanese', title));
CREATE INDEX idx_books_author ON books USING GIN(to_tsvector('japanese', author));
CREATE INDEX idx_books_category_id ON books(category_id);
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_status ON books(status);
CREATE INDEX idx_books_stock_quantity ON books(stock_quantity);
CREATE INDEX idx_books_publication_date ON books(publication_date);
CREATE INDEX idx_books_created_at ON books(created_at);
```

### 3. categories（カテゴリー）

書籍のカテゴリー分類を管理します。

```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- インデックス
CREATE INDEX idx_categories_name ON categories(name);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_display_order ON categories(display_order);
```

### 4. orders（注文）

顧客の注文情報を管理します。

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_price INTEGER NOT NULL,
    tax_amount INTEGER NOT NULL DEFAULT 0,
    shipping_fee INTEGER NOT NULL DEFAULT 0,
    discount_amount INTEGER NOT NULL DEFAULT 0,
    final_amount INTEGER NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    shipping_address TEXT NOT NULL,
    billing_address TEXT,
    order_date TIMESTAMP NOT NULL DEFAULT NOW(),
    estimated_delivery DATE,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancel_reason TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_payment_status CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_orders_payment_method CHECK (payment_method IN ('credit_card', 'debit_card', 'bank_transfer', 'cash_on_delivery')),
    CONSTRAINT chk_orders_total_price CHECK (total_price > 0),
    CONSTRAINT chk_orders_final_amount CHECK (final_amount > 0)
);

-- インデックス
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);
```

### 5. order_items（注文明細）

注文に含まれる書籍の詳細情報を管理します。

```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price INTEGER NOT NULL,
    total_price INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_order_items_total_price CHECK (total_price > 0),
    CONSTRAINT uk_order_items_order_book UNIQUE (order_id, book_id)
);

-- インデックス
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_book_id ON order_items(book_id);
```

### 6. carts（ショッピングカート）

ユーザーのショッピングカートを管理します。

```sql
CREATE TABLE carts (
    user_id BIGINT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 7. cart_items（カートアイテム）

カートに追加された書籍を管理します。

```sql
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_user_id) REFERENCES carts(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0 AND quantity <= 99),
    CONSTRAINT uk_cart_items_cart_book UNIQUE (cart_user_id, book_id)
);

-- インデックス
CREATE INDEX idx_cart_items_cart_user_id ON cart_items(cart_user_id);
CREATE INDEX idx_cart_items_book_id ON cart_items(book_id);
```

### 8. reviews（レビュー）

書籍のレビュー・評価を管理します。

```sql
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    rating INTEGER NOT NULL,
    title VARCHAR(100),
    comment TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    helpful_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT uk_reviews_user_book UNIQUE (user_id, book_id)
);

-- インデックス
CREATE INDEX idx_reviews_book_id ON reviews(book_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);
```

### 9. inventory_transactions（在庫トランザクション）

在庫の変動履歴を管理します。

```sql
CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    previous_stock INTEGER NOT NULL,
    new_stock INTEGER NOT NULL,
    reference_type VARCHAR(20),
    reference_id BIGINT,
    reason TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- 制約
    CONSTRAINT fk_inventory_transactions_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT fk_inventory_transactions_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_inventory_transaction_type CHECK (transaction_type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT', 'SALE', 'RETURN')),
    CONSTRAINT chk_inventory_previous_stock CHECK (previous_stock >= 0),
    CONSTRAINT chk_inventory_new_stock CHECK (new_stock >= 0)
);

-- インデックス
CREATE INDEX idx_inventory_transactions_book_id ON inventory_transactions(book_id);
CREATE INDEX idx_inventory_transactions_created_at ON inventory_transactions(created_at);
CREATE INDEX idx_inventory_transactions_type ON inventory_transactions(transaction_type);
```

## ビュー定義

### 1. 書籍詳細ビュー

```sql
CREATE VIEW book_details AS
SELECT 
    b.id,
    b.title,
    b.author,
    b.isbn,
    c.name AS category_name,
    b.price,
    b.description,
    b.publication_date,
    b.publisher,
    b.pages,
    b.stock_quantity,
    b.status,
    b.image_url,
    COALESCE(r.avg_rating, 0) AS average_rating,
    COALESCE(r.review_count, 0) AS review_count,
    b.created_at,
    b.updated_at
FROM books b
    LEFT JOIN categories c ON b.category_id = c.id
    LEFT JOIN (
        SELECT 
            book_id,
            ROUND(AVG(rating::numeric), 2) AS avg_rating,
            COUNT(*) AS review_count
        FROM reviews
        GROUP BY book_id
    ) r ON b.id = r.book_id
WHERE b.status = 'ACTIVE';
```

### 2. 注文詳細ビュー

```sql
CREATE VIEW order_details AS
SELECT 
    o.id,
    o.order_number,
    o.user_id,
    u.name AS user_name,
    u.email AS user_email,
    o.status,
    o.total_price,
    o.payment_method,
    o.payment_status,
    o.order_date,
    o.estimated_delivery,
    o.shipped_at,
    o.delivered_at,
    COUNT(oi.id) AS item_count,
    SUM(oi.quantity) AS total_quantity
FROM orders o
    JOIN users u ON o.user_id = u.id
    LEFT JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, u.name, u.email;
```

## インデックス戦略

### 1. 単一カラムインデックス

```sql
-- 頻繁に検索されるカラム
CREATE INDEX idx_books_title_gin ON books USING GIN(to_tsvector('japanese', title));
CREATE INDEX idx_books_author_gin ON books USING GIN(to_tsvector('japanese', author));

-- 外部キー
CREATE INDEX idx_books_category_id ON books(category_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_reviews_book_id ON reviews(book_id);

-- ソート・範囲検索
CREATE INDEX idx_books_created_at ON books(created_at DESC);
CREATE INDEX idx_orders_order_date ON orders(order_date DESC);
```

### 2. 複合インデックス

```sql
-- ユーザーの注文履歴検索用
CREATE INDEX idx_orders_user_date ON orders(user_id, order_date DESC);

-- 書籍のカテゴリ別検索用
CREATE INDEX idx_books_category_status ON books(category_id, status);

-- 在庫管理用
CREATE INDEX idx_books_stock_status ON books(stock_quantity, status);

-- レビュー集計用
CREATE INDEX idx_reviews_book_rating ON reviews(book_id, rating);
```

### 3. 部分インデックス

```sql
-- アクティブな書籍のみ
CREATE INDEX idx_books_active_title ON books(title) WHERE status = 'ACTIVE';

-- 未配送の注文のみ  
CREATE INDEX idx_orders_pending_shipment ON orders(order_date) 
WHERE status IN ('CONFIRMED', 'PROCESSING');

-- 低在庫書籍のみ
CREATE INDEX idx_books_low_stock ON books(stock_quantity) 
WHERE stock_quantity <= low_stock_threshold AND status = 'ACTIVE';
```

## データ整合性制約

### 1. 外部キー制約

```sql
-- カスケード削除の設定
ALTER TABLE cart_items 
ADD CONSTRAINT fk_cart_items_cart 
FOREIGN KEY (cart_user_id) REFERENCES carts(user_id) ON DELETE CASCADE;

ALTER TABLE order_items 
ADD CONSTRAINT fk_order_items_order 
FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- 制限削除（参照データがある場合は削除不可）
ALTER TABLE books 
ADD CONSTRAINT fk_books_category 
FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT;
```

### 2. チェック制約

```sql
-- 価格制約
ALTER TABLE books ADD CONSTRAINT chk_books_price 
CHECK (price > 0 AND price <= 999999);

-- 評価制約  
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_rating 
CHECK (rating >= 1 AND rating <= 5);

-- 数量制約
ALTER TABLE cart_items ADD CONSTRAINT chk_cart_items_quantity 
CHECK (quantity > 0 AND quantity <= 99);

-- ステータス制約
ALTER TABLE orders ADD CONSTRAINT chk_orders_status 
CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));
```

### 3. 一意制約

```sql
-- 複合一意制約
ALTER TABLE reviews ADD CONSTRAINT uk_reviews_user_book 
UNIQUE (user_id, book_id);

ALTER TABLE cart_items ADD CONSTRAINT uk_cart_items_cart_book 
UNIQUE (cart_user_id, book_id);

ALTER TABLE order_items ADD CONSTRAINT uk_order_items_order_book 
UNIQUE (order_id, book_id);
```

## パーティショニング戦略

### 1. 時系列データのパーティショニング

```sql
-- 注文テーブルの月次パーティショニング
CREATE TABLE orders_partitioned (
    id BIGSERIAL,
    order_number VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT NOW(),
    -- その他のカラム
) PARTITION BY RANGE (order_date);

-- 月次パーティション作成
CREATE TABLE orders_2024_01 PARTITION OF orders_partitioned
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE orders_2024_02 PARTITION OF orders_partitioned
FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
```

### 2. 在庫トランザクションのパーティショニング

```sql
-- 四半期パーティショニング
CREATE TABLE inventory_transactions_partitioned (
    id BIGSERIAL,
    book_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    -- その他のカラム
) PARTITION BY RANGE (created_at);
```

## データ保持ポリシー

### 1. アーカイブ戦略

```sql
-- 古いセッションデータの削除
CREATE OR REPLACE FUNCTION cleanup_old_sessions()
RETURNS void AS $$
BEGIN
    DELETE FROM user_sessions 
    WHERE expires_at < NOW() - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

-- 定期実行設定
SELECT cron.schedule('cleanup-sessions', '0 2 * * *', 'SELECT cleanup_old_sessions();');
```

### 2. ログテーブルの分割

```sql
-- 監査ログの年次分割
CREATE TABLE audit_logs_2024 (
    LIKE audit_logs INCLUDING ALL,
    CHECK (created_at >= '2024-01-01' AND created_at < '2025-01-01')
) INHERITS (audit_logs);
```

## パフォーマンスモニタリング

### 1. 統計情報収集

```sql
-- 自動VACUUM・ANALYZE設定
ALTER TABLE books SET (
    autovacuum_vacuum_scale_factor = 0.1,
    autovacuum_analyze_scale_factor = 0.05
);
```

### 2. スロークエリ検出

```sql
-- pg_stat_statementsによる分析
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows
FROM pg_stat_statements
WHERE mean_time > 100
ORDER BY mean_time DESC;
```

## まとめ

このデータベース設計は以下の特徴を持ちます：

1. **正規化**: データの整合性と冗長性排除
2. **制約**: 包括的なデータ品質保証
3. **インデックス**: 高速検索のための最適化
4. **スケーラビリティ**: パーティショニングによる将来対応
5. **監視**: パフォーマンスと品質の継続的監視

継続的なパフォーマンス監視とチューニングにより、システムの成長に対応していきます。