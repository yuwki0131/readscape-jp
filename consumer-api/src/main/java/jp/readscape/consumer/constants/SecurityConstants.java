package jp.readscape.consumer.constants;

/**
 * セキュリティ関連の定数
 */
public final class SecurityConstants {
    
    private SecurityConstants() {
        // インスタンス化を防ぐ
    }
    
    // パスワード関連
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MIN_PASSWORD_CRITERIA = 3; // 必要な文字種の最小数
    
    // JWT関連
    public static final int MIN_JWT_SECRET_LENGTH = 32; // 256bits
    public static final int RECOMMENDED_JWT_SECRET_LENGTH = 64; // 512bits
    
    // ログイン試行制限関連
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final long LOGIN_LOCK_DURATION_MINUTES = 30;
    public static final long LOGIN_ATTEMPT_RESET_MINUTES = 15;
    
    // セキュリティヘッダー関連
    public static final int MAX_USER_IDENTIFIER_LOG_LENGTH = 30;
    public static final String DEFAULT_IP_MASK = "unknown";
    
    // トークンブラックリスト関連
    public static final int BLACKLIST_CLEANUP_THRESHOLD = 100; // クリーンアップを実行する閾値
}