package jp.readscape.consumer.services.security;

import jp.readscape.consumer.constants.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class LoginAttemptService {

    // 定数をSecurityConstantsから取得
    private static final int MAX_ATTEMPT = SecurityConstants.MAX_LOGIN_ATTEMPTS;
    private static final long LOCK_TIME_DURATION = SecurityConstants.LOGIN_LOCK_DURATION_MINUTES;
    private static final long ATTEMPT_RESET_TIME = SecurityConstants.LOGIN_ATTEMPT_RESET_MINUTES;

    // 試行情報を保存するマップ（本番環境では Redis 等を使用）
    private final ConcurrentMap<String, AttemptInfo> attemptCache = new ConcurrentHashMap<>();

    /**
     * ログイン成功を記録します
     */
    public void recordSuccessfulLogin(String identifier) {
        log.debug("Recording successful login for: {}", identifier);
        attemptCache.remove(identifier);
    }

    /**
     * ログイン失敗を記録します
     */
    public void recordFailedLogin(String identifier) {
        log.debug("Recording failed login for: {}", identifier);
        
        AttemptInfo info = attemptCache.computeIfAbsent(identifier, k -> new AttemptInfo());
        info.incrementAttempts();
        
        log.warn("Failed login attempt for {}: attempt {}/{}", 
                identifier, info.getAttempts(), MAX_ATTEMPT);
    }

    /**
     * アカウントがブロックされているかチェックします
     */
    public boolean isBlocked(String identifier) {
        AttemptInfo info = attemptCache.get(identifier);
        if (info == null) {
            return false;
        }

        // 試行回数がリセット時間を超えている場合はリセット
        if (info.shouldResetAttempts()) {
            log.debug("Resetting login attempts for: {}", identifier);
            attemptCache.remove(identifier);
            return false;
        }

        boolean blocked = info.isBlocked();
        if (blocked) {
            long remainingTime = info.getRemainingLockTime();
            log.debug("Account {} is blocked. Remaining time: {} minutes", 
                    identifier, remainingTime);
        }

        return blocked;
    }

    /**
     * 残りロック時間（分）を取得します
     */
    public long getRemainingLockTime(String identifier) {
        AttemptInfo info = attemptCache.get(identifier);
        return info != null ? info.getRemainingLockTime() : 0;
    }

    /**
     * 試行回数を取得します
     */
    public int getAttempts(String identifier) {
        AttemptInfo info = attemptCache.get(identifier);
        return info != null ? info.getAttempts() : 0;
    }

    /**
     * 手動でアカウントのブロックを解除します（管理者用）
     */
    public void clearLoginAttempts(String identifier) {
        log.info("Manually clearing login attempts for: {}", identifier);
        attemptCache.remove(identifier);
    }

    /**
     * 試行情報を表すクラス
     */
    private static class AttemptInfo {
        private int attempts;
        private LocalDateTime firstAttempt;
        private LocalDateTime lastAttempt;

        public AttemptInfo() {
            this.attempts = 0;
            this.firstAttempt = LocalDateTime.now();
            this.lastAttempt = LocalDateTime.now();
        }

        public void incrementAttempts() {
            this.attempts++;
            this.lastAttempt = LocalDateTime.now();
            if (this.firstAttempt == null) {
                this.firstAttempt = LocalDateTime.now();
            }
        }

        public boolean isBlocked() {
            if (attempts < MAX_ATTEMPT) {
                return false;
            }

            // ロック時間を超えている場合は自動的にブロック解除
            LocalDateTime unlockTime = lastAttempt.plus(LOCK_TIME_DURATION, ChronoUnit.MINUTES);
            return LocalDateTime.now().isBefore(unlockTime);
        }

        public boolean shouldResetAttempts() {
            if (firstAttempt == null) {
                return false;
            }

            LocalDateTime resetTime = firstAttempt.plus(ATTEMPT_RESET_TIME, ChronoUnit.MINUTES);
            return LocalDateTime.now().isAfter(resetTime);
        }

        public long getRemainingLockTime() {
            if (attempts < MAX_ATTEMPT) {
                return 0;
            }

            LocalDateTime unlockTime = lastAttempt.plus(LOCK_TIME_DURATION, ChronoUnit.MINUTES);
            return ChronoUnit.MINUTES.between(LocalDateTime.now(), unlockTime);
        }

        public int getAttempts() {
            return attempts;
        }
    }

    /**
     * ログイン試行状況を表すクラス
     */
    public static class LoginAttemptStatus {
        private final boolean blocked;
        private final int attempts;
        private final int maxAttempts;
        private final long remainingLockTime;

        public LoginAttemptStatus(boolean blocked, int attempts, int maxAttempts, long remainingLockTime) {
            this.blocked = blocked;
            this.attempts = attempts;
            this.maxAttempts = maxAttempts;
            this.remainingLockTime = remainingLockTime;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public int getAttempts() {
            return attempts;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public long getRemainingLockTime() {
            return remainingLockTime;
        }

        public int getRemainingAttempts() {
            return Math.max(0, maxAttempts - attempts);
        }
    }

    /**
     * ログイン試行状況を取得します
     */
    public LoginAttemptStatus getLoginAttemptStatus(String identifier) {
        AttemptInfo info = attemptCache.get(identifier);
        if (info == null) {
            return new LoginAttemptStatus(false, 0, MAX_ATTEMPT, 0);
        }

        boolean blocked = isBlocked(identifier);
        long remainingTime = blocked ? info.getRemainingLockTime() : 0;
        
        return new LoginAttemptStatus(blocked, info.getAttempts(), MAX_ATTEMPT, remainingTime);
    }
}