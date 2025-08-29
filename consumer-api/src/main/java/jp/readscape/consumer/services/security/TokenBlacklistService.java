package jp.readscape.consumer.services.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * トークンブラックリスト管理サービス
 * Redis対応（フォールバックあり）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    @Autowired(required = false)
    private final RedisTemplate<String, String> stringRedisTemplate;
    
    // Redisが利用できない場合のフォールバック
    private final ConcurrentMap<String, TokenInfo> blacklistedTokens = new ConcurrentHashMap<>();
    
    private static final String REDIS_KEY_PREFIX = "blacklist:token:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    
    /**
     * トークンをブラックリストに追加（Redis対応）
     */
    public void blacklistToken(String token, LocalDateTime expiration) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        String maskedToken = token.substring(0, Math.min(10, token.length())) + "...";
        Duration ttl = expiration != null ? 
            Duration.between(LocalDateTime.now(), expiration) : DEFAULT_TTL;
        
        try {
            // Redisを試す
            if (stringRedisTemplate != null && isRedisAvailable()) {
                String key = REDIS_KEY_PREFIX + Integer.toString(token.hashCode());
                stringRedisTemplate.opsForValue().set(key, "blacklisted", ttl);
                log.debug("Token blacklisted in Redis: {} (TTL: {})", maskedToken, ttl);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, using memory storage for token: {}", maskedToken, e);
        }
        
        // フォールバック：メモリベース
        String tokenHash = Integer.toString(token.hashCode());
        blacklistedTokens.put(tokenHash, new TokenInfo(LocalDateTime.now(), expiration));
        log.debug("Token added to memory blacklist: {}, Expires: {}", maskedToken, expiration);
        cleanupExpiredTokens();
    }
    
    /**
     * トークンがブラックリストに登録されているかチェック（Redis対応）
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String tokenHash = Integer.toString(token.hashCode());
        
        try {
            // Redisをチェック
            if (stringRedisTemplate != null && isRedisAvailable()) {
                String key = REDIS_KEY_PREFIX + tokenHash;
                Boolean exists = stringRedisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(exists)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Redis check failed, checking memory storage: {}", e.getMessage());
        }
        
        // フォールバック：メモリベース
        TokenInfo info = blacklistedTokens.get(tokenHash);
        if (info == null) {
            return false;
        }
        
        // 期限切れの場合は削除
        if (info.expiration != null && info.expiration.isBefore(LocalDateTime.now())) {
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
    
    /**
     * Redisが利用可能かチェック
     */
    private boolean isRedisAvailable() {
        try {
            if (stringRedisTemplate == null) {
                return false;
            }
            stringRedisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class BlacklistStats {
        private final int activeTokens;
        private final boolean redisAvailable;
        
        public BlacklistStats(int activeTokens) {
            this(activeTokens, false);
        }
        
        public BlacklistStats(int activeTokens, boolean redisAvailable) {
            this.activeTokens = activeTokens;
            this.redisAvailable = redisAvailable;
        }
        
        public int getActiveTokens() {
            return activeTokens;
        }
        
        public boolean isRedisAvailable() {
            return redisAvailable;
        }
    }
}