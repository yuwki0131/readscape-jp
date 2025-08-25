-- ユーザーテーブルの作成
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'CONSUMER' CHECK (role IN ('CONSUMER', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- インデックス作成
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);

-- サンプルデータの挿入（パスワードは 'password123' をBCryptでエンコードしたもの）
INSERT INTO users (username, email, password, first_name, last_name, phone, address, role, is_active) VALUES
('admin', 'admin@readscape.jp', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '管理者', '太郎', '03-1234-5678', '東京都渋谷区神南1-1-1', 'ADMIN', true),
('testuser', 'test@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'テスト', 'ユーザー', '090-1234-5678', '東京都新宿区西新宿1-1-1', 'CONSUMER', true),
('customer1', 'customer1@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '顧客', '一郎', '080-1111-2222', '大阪府大阪市北区梅田1-1-1', 'CONSUMER', true),
('customer2', 'customer2@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '顧客', '二郎', '070-3333-4444', '愛知県名古屋市中村区名駅1-1-1', 'CONSUMER', true);