package jp.readscape.consumer.services.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SecurityAuditService {

    private static final String AUDIT_LOG_PREFIX = "SECURITY_AUDIT";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ログイン成功をログに記録します
     */
    public void logSuccessfulLogin(String email, String ipAddress) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - SUCCESSFUL_LOGIN: email=%s, ip=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, timestamp
        );
        
        log.info(logMessage);
    }

    /**
     * ログイン失敗をログに記録します
     */
    public void logFailedLogin(String email, String ipAddress, String reason) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - FAILED_LOGIN: email=%s, ip=%s, reason=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, reason, timestamp
        );
        
        log.warn(logMessage);
    }

    /**
     * ログアウトをログに記録します
     */
    public void logLogout(String email, String ipAddress) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - LOGOUT: email=%s, ip=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, timestamp
        );
        
        log.info(logMessage);
    }

    /**
     * 不正アクセス試行をログに記録します
     */
    public void logUnauthorizedAccess(String email, String resource, String ipAddress) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - UNAUTHORIZED_ACCESS: email=%s, resource=%s, ip=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, resource, ipAddress, timestamp
        );
        
        log.error(logMessage);
    }

    /**
     * 不審な活動をログに記録します
     */
    public void logSuspiciousActivity(String email, String activity, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - SUSPICIOUS_ACTIVITY: email=%s, activity=%s, details=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, activity, details, timestamp
        );
        
        log.error(logMessage);
    }

    /**
     * アカウントロックをログに記録します
     */
    public void logAccountLocked(String email, String ipAddress, int attempts) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - ACCOUNT_LOCKED: email=%s, ip=%s, attempts=%d, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, attempts, timestamp
        );
        
        log.error(logMessage);
    }

    /**
     * アカウントロック解除をログに記録します
     */
    public void logAccountUnlocked(String email, String unlockedBy) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - ACCOUNT_UNLOCKED: email=%s, unlocked_by=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, unlockedBy, timestamp
        );
        
        log.info(logMessage);
    }

    /**
     * パスワード変更をログに記録します
     */
    public void logPasswordChange(String email, String ipAddress) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - PASSWORD_CHANGED: email=%s, ip=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, timestamp
        );
        
        log.info(logMessage);
    }

    /**
     * 権限昇格をログに記録します
     */
    public void logPrivilegeEscalation(String email, String fromRole, String toRole, String changedBy) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - PRIVILEGE_ESCALATION: email=%s, from_role=%s, to_role=%s, changed_by=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, fromRole, toRole, changedBy, timestamp
        );
        
        log.warn(logMessage);
    }

    /**
     * データアクセスをログに記録します
     */
    public void logDataAccess(String email, String dataType, String action, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - DATA_ACCESS: email=%s, data_type=%s, action=%s, details=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, dataType, action, details, timestamp
        );
        
        log.info(logMessage);
    }

    /**
     * APIレート制限違反をログに記録します
     */
    public void logRateLimitViolation(String email, String ipAddress, String endpoint, int attempts) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - RATE_LIMIT_VIOLATION: email=%s, ip=%s, endpoint=%s, attempts=%d, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, endpoint, attempts, timestamp
        );
        
        log.warn(logMessage);
    }

    /**
     * 重要な設定変更をログに記録します
     */
    public void logConfigurationChange(String changedBy, String configType, String oldValue, String newValue) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - CONFIGURATION_CHANGE: changed_by=%s, config_type=%s, old_value=%s, new_value=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, changedBy, configType, oldValue, newValue, timestamp
        );
        
        log.warn(logMessage);
    }

    /**
     * セキュリティ例外をログに記録します
     */
    public void logSecurityException(String email, String ipAddress, String exception, String stackTrace) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - SECURITY_EXCEPTION: email=%s, ip=%s, exception=%s, stack_trace=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, exception, stackTrace, timestamp
        );
        
        log.error(logMessage);
    }

    /**
     * トークン関連のセキュリティイベントをログに記録します
     */
    public void logTokenEvent(String email, String ipAddress, String eventType, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - TOKEN_EVENT: email=%s, ip=%s, event_type=%s, details=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, eventType, details, timestamp
        );
        
        log.info(logMessage);
    }

    /**
     * ユーザー登録をログに記録します
     */
    public void logUserRegistration(String email, String ipAddress) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - USER_REGISTRATION: email=%s, ip=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, email, ipAddress, timestamp
        );
        
        log.info(logMessage);
    }

    /**
     * ユーザー削除をログに記録します
     */
    public void logUserDeletion(String deletedEmail, String deletedBy) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "%s - USER_DELETION: deleted_email=%s, deleted_by=%s, timestamp=%s",
            AUDIT_LOG_PREFIX, deletedEmail, deletedBy, timestamp
        );
        
        log.warn(logMessage);
    }
}