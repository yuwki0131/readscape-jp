-- 書籍テーブルの作成
CREATE TABLE readscape.books (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    isbn VARCHAR(13) UNIQUE,
    price INTEGER NOT NULL CHECK (price >= 0),
    category_id BIGINT,
    description TEXT,
    stock_quantity INTEGER DEFAULT 0 CHECK (stock_quantity >= 0),
    average_rating DECIMAL(2,1) DEFAULT 0.0 CHECK (average_rating >= 0.0 AND average_rating <= 5.0),
    review_count INTEGER DEFAULT 0 CHECK (review_count >= 0),
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- インデックス作成
CREATE INDEX idx_books_title ON readscape.books USING gin(to_tsvector('english', title));
CREATE INDEX idx_books_author ON readscape.books(author);
CREATE INDEX idx_books_category_id ON readscape.books(category_id);
CREATE INDEX idx_books_price ON readscape.books(price);
CREATE INDEX idx_books_average_rating ON readscape.books(average_rating);
CREATE INDEX idx_books_stock_quantity ON readscape.books(stock_quantity);

-- サンプルデータの挿入
INSERT INTO readscape.books (title, author, isbn, price, description, category_id, stock_quantity, average_rating, review_count, image_url) VALUES
('Spring Boot実践入門', '技術太郎', '9784000000001', 3200, 'Spring Bootの基本から応用まで幅広く解説した実践的な入門書です。', 1, 25, 4.5, 12, 'https://images.readscape.jp/books/spring-boot.jpg'),
('Java設計パターン', 'パターン花子', '9784000000002', 2800, 'GoFパターンをJavaで実践的に学ぶための決定版。', 1, 15, 4.2, 8, 'https://images.readscape.jp/books/java-patterns.jpg'),
('マーケティング戦略入門', 'ビジネス次郎', '9784000000003', 2200, '現代マーケティングの基礎から実践まで学べる入門書。', 2, 30, 4.0, 5, 'https://images.readscape.jp/books/marketing.jpg'),
('データベース設計の基礎', 'データベース三郎', '9784000000004', 3500, 'リレーショナルデータベース設計の理論と実践。', 1, 20, 4.8, 15, 'https://images.readscape.jp/books/database-design.jpg'),
('プロジェクト管理の教科書', 'プロジェクト四郎', '9784000000005', 2600, '効果的なプロジェクト管理手法を体系的に解説。', 2, 18, 3.9, 7, 'https://images.readscape.jp/books/project-management.jpg'),
('アルゴリズム図鑑', 'アルゴリズム五郎', '9784000000006', 2400, '重要なアルゴリズムを図解でわかりやすく説明。', 1, 22, 4.6, 20, 'https://images.readscape.jp/books/algorithms.jpg'),
('デザイン思考入門', 'デザイン六子', '9784000000007', 2000, 'ユーザー中心のデザイン思考を身につける入門書。', 3, 28, 4.1, 9, 'https://images.readscape.jp/books/design-thinking.jpg'),
('機械学習はじめの一歩', 'AI七郎', '9784000000008', 3800, '機械学習の基礎理論から実装まで段階的に学習。', 1, 12, 4.7, 25, 'https://images.readscape.jp/books/machine-learning.jpg');