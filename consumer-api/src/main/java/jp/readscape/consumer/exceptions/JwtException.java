package jp.readscape.consumer.exceptions;

/**
 * JWT関連の例外
 */
public class JwtException extends RuntimeException {
    
    public JwtException(String message) {
        super(message);
    }
    
    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * JWTの有効期限切れ例外
     */
    public static class ExpiredException extends JwtException {
        public ExpiredException() {
            super("JWT token has expired");
        }
    }
    
    /**
     * JWT形式不正例外
     */
    public static class MalformedException extends JwtException {
        public MalformedException() {
            super("Invalid JWT token format");
        }
    }
    
    /**
     * JWTセキュリティ違反例外
     */
    public static class SecurityException extends JwtException {
        public SecurityException() {
            super("JWT token security violation");
        }
    }
    
    /**
     * JWT無効例外
     */
    public static class InvalidException extends JwtException {
        public InvalidException() {
            super("Invalid JWT token");
        }
        
        public InvalidException(String message) {
            super(message);
        }
    }
}