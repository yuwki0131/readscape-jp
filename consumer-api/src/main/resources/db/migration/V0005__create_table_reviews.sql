-- レビューテーブルの作成
CREATE TABLE readscape.reviews (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES readscape.books(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES readscape.users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_title VARCHAR(100),
    review_comment TEXT,
    is_verified_purchase BOOLEAN DEFAULT false,
    helpful_count INTEGER DEFAULT 0 CHECK (helpful_count >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(book_id, user_id)
);

-- インデックス作成
CREATE INDEX idx_reviews_book_id ON readscape.reviews(book_id);
CREATE INDEX idx_reviews_user_id ON readscape.reviews(user_id);
CREATE INDEX idx_reviews_rating ON readscape.reviews(rating);
CREATE INDEX idx_reviews_created_at ON readscape.reviews(created_at);
CREATE INDEX idx_reviews_helpful_count ON readscape.reviews(helpful_count);
CREATE INDEX idx_reviews_is_verified_purchase ON readscape.reviews(is_verified_purchase);
CREATE INDEX idx_reviews_book_rating ON readscape.reviews(book_id, rating);
CREATE INDEX idx_reviews_book_created ON readscape.reviews(book_id, created_at DESC);

-- 複合インデックス（レビュー取得の最適化）
CREATE INDEX idx_reviews_book_verified_created ON readscape.reviews(book_id, is_verified_purchase, created_at DESC);
CREATE INDEX idx_reviews_book_helpful_created ON readscape.reviews(book_id, helpful_count DESC, created_at DESC);

-- サンプルデータの挿入
INSERT INTO readscape.reviews (book_id, user_id, rating, review_title, review_comment, is_verified_purchase, helpful_count) VALUES
-- Spring Boot実践入門のレビュー
(1, 3, 5, 'とても分かりやすい入門書', 'Spring Bootの基本から応用まで、実践的な内容で非常に理解しやすかったです。コード例も豊富で、実際の開発に活用できます。', true, 3),
(1, 4, 4, '実践的な内容', 'サンプルコードが豊富で、実際のプロジェクトですぐに使える内容でした。初心者にもおすすめです。', true, 2),

-- Java設計パターンのレビュー
(2, 3, 4, '設計パターンの理解が深まる', 'GoFパターンをJavaで実装する方法が詳しく解説されていて、実務で役立ちました。', true, 1),
(2, 2, 5, '設計の考え方が変わる', '単なるパターンの紹介ではなく、なぜそのパターンが必要なのかという背景まで説明されているのが良い。', true, 4),

-- マーケティング戦略入門のレビュー
(3, 4, 3, '基本的な内容', '入門書としては良いですが、もう少し深い内容を期待していました。初学者には良いと思います。', true, 0),

-- データベース設計の基礎のレビュー
(4, 2, 5, 'DB設計の必読書', 'データベース設計の基本から応用まで、体系的に学べる素晴らしい書籍です。実務に直結する内容ばかりです。', true, 5),
(4, 3, 4, '理論と実践のバランスが良い', '理論的な説明と実践的な例のバランスが良く、理解しやすい構成になっています。', true, 2),

-- プロジェクト管理の教科書のレビュー
(5, 2, 4, 'PM入門に最適', 'プロジェクトマネジメントの基本的な考え方から具体的な手法まで、分かりやすく解説されています。', true, 1),

-- アルゴリズム図鑑のレビュー
(6, 3, 5, '視覚的で理解しやすい', '図解が豊富で、複雑なアルゴリズムも直感的に理解できます。プログラマー必携の一冊です。', true, 8),
(6, 4, 5, '素晴らしい図解', 'アルゴリズムを視覚的に理解できる素晴らしい書籍。初心者から上級者まで役立ちます。', true, 6),
(6, 2, 4, '分かりやすい説明', '図が多用されていて、アルゴリズムの動作が直感的に理解できる良書です。', true, 3),

-- デザイン思考入門のレビュー
(7, 4, 4, 'デザイン思考の基本を学べる', 'デザイン思考のプロセスが体系的に説明されていて、実践しやすい内容です。', true, 2),
(7, 2, 3, '入門書としては良い', 'デザイン思考の概要は理解できますが、もう少し具体的な事例があると良かったです。', true, 1),

-- 機械学習はじめの一歩のレビュー
(8, 3, 5, 'ML入門の決定版', '機械学習の基礎から実装まで、段階的に学習できる優れた入門書です。サンプルコードも豊富で実践的です。', true, 7),
(8, 4, 4, '実装重視で良い', '理論だけでなく、実際のコード実装に重点を置いているのが良い。実務で使える知識が身につきます。', true, 4);