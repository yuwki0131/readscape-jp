package jp.readscape.consumer.services.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * トークンブラックリスト管理サービス
 * 本番環境では Redis または データベースを使用することを推奨
 */
@Slf4j
@Service
public class TokenBlacklistService {
    
    // TODO: 本番環境では Redis または データベースに置き換える
    private final ConcurrentMap<String, TokenInfo> blacklistedTokens = new ConcurrentHashMap<>();
    
    /**
     * トークンをブラックリストに追加
     */
    public void blacklistToken(String token, LocalDateTime expiration) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        // トークンのハッシュ値を保存（メモリ効率のため）
        String tokenHash = Integer.toString(token.hashCode());
        blacklistedTokens.put(tokenHash, new TokenInfo(LocalDateTime.now(), expiration));
        
        log.debug("Token added to blacklist. Hash: {}, Expires: {}", tokenHash, expiration);
        
        // 期限切れトークンのクリーンアップ
        cleanupExpiredTokens();
    }
    
    /**
     * トークンがブラックリストに登録されているかチェック
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String tokenHash = Integer.toString(token.hashCode());
        TokenInfo info = blacklistedTokens.get(tokenHash);
        
        if (info == null) {
            return false;
        }
        
        // 期限切れの場合は削除
        if (info.expiration.isBefore(LocalDateTime.now())) {
            blacklistedTokens.remove(tokenHash);
            return false;
        }
        
        return true;
    }
    
    /**
     * 期限切れトークンのクリーンアップ
     */
    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int initialSize = blacklistedTokens.size();
        
        blacklistedTokens.entrySet().removeIf(entry -> 
            entry.getValue().expiration.isBefore(now)
        );
        
        int removedCount = initialSize - blacklistedTokens.size();
        if (removedCount > 0) {
            log.debug("Cleaned up {} expired tokens from blacklist", removedCount);
        }
    }
    
    /**
     * ブラックリストの統計情報を取得
     */
    public BlacklistStats getStats() {
        cleanupExpiredTokens();
        return new BlacklistStats(blacklistedTokens.size());
    }
    
    /**
     * 手動でブラックリストをクリア（テスト用）
     */
    public void clearBlacklist() {
        int size = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.info("Manually cleared blacklist. Removed {} tokens", size);
    }
    
    private static class TokenInfo {
        final LocalDateTime blacklistedAt;
        final LocalDateTime expiration;
        
        TokenInfo(LocalDateTime blacklistedAt, LocalDateTime expiration) {
            this.blacklistedAt = blacklistedAt;
            this.expiration = expiration;
        }
    }
    
    public static class BlacklistStats {
        private final int activeTokens;
        
        public BlacklistStats(int activeTokens) {
            this.activeTokens = activeTokens;
        }
        
        public int getActiveTokens() {
            return activeTokens;
        }
    }
}