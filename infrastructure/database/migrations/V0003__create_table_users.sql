-- ユーザーテーブルの作成
CREATE TABLE readscape.users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    first_name TEXT,
    last_name TEXT,
    phone TEXT,
    address TEXT,
    role TEXT NOT NULL DEFAULT 'CONSUMER' CHECK (role IN ('CONSUMER', 'ADMIN', 'MANAGER', 'ANALYST')),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- インデックス作成
CREATE INDEX idx_users_email ON readscape.users(email);
CREATE INDEX idx_users_username ON readscape.users(username);
CREATE INDEX idx_users_role ON readscape.users(role);
CREATE INDEX idx_users_is_active ON readscape.users(is_active);

-- サンプルデータの挿入（パスワードは 'password123' をBCryptでエンコードしたもの）
INSERT INTO readscape.users (username, email, password, first_name, last_name, phone, address, role, is_active) VALUES
('admin', 'admin@readscape.jp', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '管理者', '太郎', '03-1234-5678', '東京都渋谷区神南1-1-1', 'ADMIN', true),
('testuser', 'test@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'テスト', 'ユーザー', '090-1234-5678', '東京都新宿区西新宿1-1-1', 'CONSUMER', true),
('customer1', 'customer1@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '顧客', '一郎', '080-1111-2222', '大阪府大阪市北区梅田1-1-1', 'CONSUMER', true),
('customer2', 'customer2@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '顧客', '二郎', '070-3333-4444', '愛知県名古屋市中村区名駅1-1-1', 'CONSUMER', true);