-- カートテーブルの作成
CREATE TABLE carts (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

-- カートアイテムテーブルの作成
CREATE TABLE cart_items (
    id SERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price INTEGER NOT NULL CHECK (unit_price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(cart_id, book_id)
);

-- 注文テーブルの作成
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    item_count INTEGER NOT NULL CHECK (item_count > 0),
    shipping_address TEXT NOT NULL,
    shipping_phone VARCHAR(20),
    payment_method VARCHAR(50),
    notes TEXT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shipped_date TIMESTAMP,
    delivered_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 注文アイテムテーブルの作成
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    book_title TEXT NOT NULL,
    book_author TEXT NOT NULL,
    book_isbn VARCHAR(13),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price INTEGER NOT NULL CHECK (unit_price >= 0),
    subtotal DECIMAL(10,2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- インデックス作成

-- カート関連のインデックス
CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_book_id ON cart_items(book_id);

-- 注文関連のインデックス
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_orders_user_order_date ON orders(user_id, order_date DESC);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_book_id ON order_items(book_id);

-- サンプルデータの挿入

-- サンプルカート（テストユーザー用）
INSERT INTO carts (user_id) VALUES 
(2), -- testuser
(3); -- customer1

-- サンプルカートアイテム
INSERT INTO cart_items (cart_id, book_id, quantity, unit_price) VALUES
(1, 1, 2, 3200), -- testuser のカート: Spring Boot実践入門 x2
(1, 4, 1, 3500), -- testuser のカート: データベース設計の基礎 x1
(2, 2, 1, 2800), -- customer1 のカート: Java設計パターン x1
(2, 6, 3, 2400); -- customer1 のカート: アルゴリズム図鑑 x3

-- サンプル注文
INSERT INTO orders (order_number, user_id, status, total_amount, item_count, shipping_address, shipping_phone, payment_method, notes, order_date) VALUES
('ORD-1704067200001', 3, 'DELIVERED', 12600.00, 4, '大阪府大阪市北区梅田1-1-1', '080-1111-2222', 'クレジットカード', '迅速な配送をお願いします', '2024-01-01 10:00:00'),
('ORD-1704153600002', 4, 'SHIPPED', 5200.00, 2, '愛知県名古屋市中村区名駅1-1-1', '070-3333-4444', '代金引換', 'なし', '2024-01-02 14:30:00'),
('ORD-1704240000003', 3, 'PROCESSING', 7200.00, 3, '大阪府大阪市北区梅田1-1-1', '080-1111-2222', 'クレジットカード', 'なし', '2024-01-03 09:15:00');

-- サンプル注文アイテム
INSERT INTO order_items (order_id, book_id, book_title, book_author, book_isbn, quantity, unit_price, subtotal) VALUES
-- 注文1のアイテム（customer1）
(1, 1, 'Spring Boot実践入門', '技術太郎', '9784000000001', 2, 3200, 6400.00),
(1, 6, 'アルゴリズム図鑑', 'アルゴリズム五郎', '9784000000006', 2, 2400, 4800.00),
(1, 7, 'デザイン思考入門', 'デザイン六子', '9784000000007', 1, 2000, 2000.00),

-- 注文2のアイテム（customer2）
(2, 2, 'Java設計パターン', 'パターン花子', '9784000000002', 1, 2800, 2800.00),
(2, 4, 'データベース設計の基礎', 'データベース三郎', '9784000000004', 1, 3500, 3500.00),

-- 注文3のアイテム（customer1）
(3, 8, '機械学習はじめの一歩', 'AI七郎', '9784000000008', 1, 3800, 3800.00),
(3, 3, 'マーケティング戦略入門', 'ビジネス次郎', '9784000000003', 1, 2200, 2200.00),
(3, 5, 'プロジェクト管理の教科書', 'プロジェクト四郎', '9784000000005', 1, 2600, 2600.00);