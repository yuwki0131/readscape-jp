-- 在庫履歴テーブルの作成
CREATE TABLE readscape.stock_history (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    change_type TEXT NOT NULL CHECK (change_type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT', 'RETURN')),
    quantity_before INTEGER NOT NULL CHECK (quantity_before >= 0),
    quantity_after INTEGER NOT NULL CHECK (quantity_after >= 0),
    quantity_changed INTEGER NOT NULL,
    reason TEXT,
    reference_type TEXT, -- 'ORDER', 'ADJUSTMENT', 'RETURN', 'MANUAL'
    reference_id BIGINT, -- 関連する注文IDや調整記録ID
    created_by BIGINT, -- 操作実行ユーザーID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES readscape.books(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES readscape.users(id) ON DELETE SET NULL
);

-- 操作ログテーブルの作成
CREATE TABLE readscape.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username TEXT,
    action TEXT NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'ORDER'
    resource_type TEXT NOT NULL, -- 'BOOK', 'USER', 'ORDER', 'CATEGORY', 'CART'
    resource_id BIGINT,
    old_values JSONB, -- 変更前の値（JSON形式）
    new_values JSONB, -- 変更後の値（JSON形式）
    ip_address INET,
    user_agent TEXT,
    session_id TEXT,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES readscape.users(id) ON DELETE SET NULL
);

-- ログイン試行履歴テーブルの作成
CREATE TABLE readscape.login_attempts (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    ip_address INET NOT NULL,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason TEXT,
    user_agent TEXT,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- インデックス作成
CREATE INDEX idx_stock_history_book_id ON readscape.stock_history(book_id);
CREATE INDEX idx_stock_history_created_at ON readscape.stock_history(created_at);
CREATE INDEX idx_stock_history_change_type ON readscape.stock_history(change_type);
CREATE INDEX idx_stock_history_reference ON readscape.stock_history(reference_type, reference_id);

CREATE INDEX idx_audit_logs_user_id ON readscape.audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON readscape.audit_logs(created_at);
CREATE INDEX idx_audit_logs_action ON readscape.audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON readscape.audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_ip_address ON readscape.audit_logs(ip_address);

CREATE INDEX idx_login_attempts_username ON readscape.login_attempts(username);
CREATE INDEX idx_login_attempts_ip_address ON readscape.login_attempts(ip_address);
CREATE INDEX idx_login_attempts_attempted_at ON readscape.login_attempts(attempted_at);
CREATE INDEX idx_login_attempts_success ON readscape.login_attempts(success);

-- パフォーマンス向上のための部分インデックス
CREATE INDEX idx_audit_logs_failed_actions ON readscape.audit_logs(created_at) WHERE success = FALSE;
CREATE INDEX idx_login_attempts_failures ON readscape.login_attempts(attempted_at, ip_address) WHERE success = FALSE;