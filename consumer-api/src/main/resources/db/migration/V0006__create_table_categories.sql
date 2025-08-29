-- カテゴリテーブルの作成（階層構造対応）
CREATE TABLE readscape.categories (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES readscape.categories(id) ON DELETE SET NULL
);

-- インデックス作成
CREATE INDEX idx_categories_name ON readscape.categories(name);
CREATE INDEX idx_categories_parent_id ON readscape.categories(parent_id);
CREATE INDEX idx_categories_sort_order ON readscape.categories(sort_order);
CREATE INDEX idx_categories_is_active ON readscape.categories(is_active);

-- 初期カテゴリーデータの挿入（親カテゴリー）
INSERT INTO readscape.categories (name, description, sort_order) VALUES
('技術書', 'プログラミング・IT関連の技術書籍', 1),
('ビジネス書', '経営・マネジメント・自己啓発関連書籍', 2),
('デザイン書', 'UI/UX・グラフィックデザイン関連書籍', 3),
('学術書', '学術論文・研究関連書籍', 4),
('実用書', '生活・趣味・実用関連書籍', 5);

-- サブカテゴリーの追加
INSERT INTO readscape.categories (name, description, parent_id, sort_order) VALUES
('Java', 'Java言語関連書籍', 1, 1),
('Python', 'Python言語関連書籍', 1, 2),
('JavaScript', 'JavaScript関連書籍', 1, 3),
('データベース', 'データベース設計・管理関連書籍', 1, 4),
('Web開発', 'Web開発全般の書籍', 1, 5),
('マーケティング', 'マーケティング戦略・手法関連書籍', 2, 1),
('リーダーシップ', 'リーダーシップ・マネジメント関連書籍', 2, 2),
('起業・経営', '起業・経営戦略関連書籍', 2, 3);

-- 既存のbooksテーブルのcategory_idを適切に設定
UPDATE readscape.books 
SET category_id = CASE 
    WHEN category LIKE '%技術%' OR category LIKE '%Java%' OR category LIKE '%Spring%' OR category LIKE '%データベース%' OR category LIKE '%アルゴリズム%' OR category LIKE '%機械学習%' THEN 1
    WHEN category LIKE '%ビジネス%' OR category LIKE '%マーケティング%' OR category LIKE '%プロジェクト%' THEN 2
    WHEN category LIKE '%デザイン%' THEN 3
    ELSE 1  -- デフォルトは技術書
END
WHERE category_id IS NULL;