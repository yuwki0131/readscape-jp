-- カテゴリテーブルの作成
CREATE TABLE readscape.categories (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- インデックス作成
CREATE INDEX idx_categories_name ON readscape.categories(name);

-- 初期カテゴリデータ
INSERT INTO readscape.categories (name, description) VALUES
('技術書', 'プログラミング・IT・データベース関連書籍'),
('ビジネス書', '経営・マーケティング・プロジェクト管理関連書籍'),
('デザイン', 'デザイン思考・UI/UX・グラフィック関連書籍'),
('文学', '小説・詩・エッセイなどの文学作品'),
('学術書', '学術研究・理論書・専門書籍'),
('実用書', '趣味・生活・スキルアップ関連書籍');