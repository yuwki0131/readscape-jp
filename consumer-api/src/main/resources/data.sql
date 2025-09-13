-- 開発環境用サンプルユーザー（パスワード: testpass をBCryptでハッシュ化）
-- $2a$10$pJdmEBt.WqGiJJ7GdYPl7O8VV5J85J8PNMzd4p4kfJ1Q3lWFNkzFy = "testpass"
INSERT INTO users (username, email, password, first_name, last_name, phone, role, is_active, created_at, updated_at) VALUES
('consumer1', 'consumer1@readscape.jp', '$2a$10$pJdmEBt.WqGiJJ7GdYPl7O8VV5J85J8PNMzd4p4kfJ1Q3lWFNkzFy', '一般', '消費者1', '090-1234-5678', 'CONSUMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('manager1', 'manager1@readscape.jp', '$2a$10$pJdmEBt.WqGiJJ7GdYPl7O8VV5J85J8PNMzd4p4kfJ1Q3lWFNkzFy', '管理', '者1', '090-1234-5679', 'MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admin1', 'admin1@readscape.jp', '$2a$10$pJdmEBt.WqGiJJ7GdYPl7O8VV5J85J8PNMzd4p4kfJ1Q3lWFNkzFy', 'システム', '管理者', '090-1234-5680', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- サンプル書籍データの挿入
INSERT INTO books (title, author, isbn, price, description, category, stock_quantity, average_rating, review_count, image_url, created_at, updated_at) VALUES
('Spring Boot実践入門', '技術太郎', '9784000000001', 3200, 'Spring Bootの基本から応用まで幅広く解説した実践的な入門書です。', '技術書', 25, 4.5, 12, 'https://images.readscape.jp/books/spring-boot.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Java設計パターン', 'パターン花子', '9784000000002', 2800, 'GoFパターンをJavaで実践的に学ぶための決定版。', '技術書', 15, 4.2, 8, 'https://images.readscape.jp/books/java-patterns.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('マーケティング戦略入門', 'ビジネス次郎', '9784000000003', 2200, '現代マーケティングの基礎から実践まで学べる入門書。', 'ビジネス書', 30, 4.0, 5, 'https://images.readscape.jp/books/marketing.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('データベース設計の基礎', 'データベース三郎', '9784000000004', 3500, 'リレーショナルデータベース設計の理論と実践。', '技術書', 20, 4.8, 15, 'https://images.readscape.jp/books/database-design.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('プロジェクト管理の教科書', 'プロジェクト四郎', '9784000000005', 2600, '効果的なプロジェクト管理手法を体系的に解説。', 'ビジネス書', 18, 3.9, 7, 'https://images.readscape.jp/books/project-management.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('アルゴリズム図鑑', 'アルゴリズム五郎', '9784000000006', 2400, '重要なアルゴリズムを図解でわかりやすく説明。', '技術書', 22, 4.6, 20, 'https://images.readscape.jp/books/algorithms.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('デザイン思考入門', 'デザイン六子', '9784000000007', 2000, 'ユーザー中心のデザイン思考を身につける入門書。', 'デザイン', 28, 4.1, 9, 'https://images.readscape.jp/books/design-thinking.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('機械学習はじめの一歩', 'AI七郎', '9784000000008', 3800, '機械学習の基礎理論から実装まで段階的に学習。', '技術書', 12, 4.7, 25, 'https://images.readscape.jp/books/machine-learning.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);