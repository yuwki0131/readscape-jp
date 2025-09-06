package jp.readscape.consumer.services.security;

import jp.readscape.consumer.constants.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;
    private final String testIdentifier = "test@example.com";

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    void recordSuccessfulLogin_ShouldClearPreviousAttempts() throws Exception {
        // Given
        loginAttemptService.recordFailedLogin(testIdentifier);
        loginAttemptService.recordFailedLogin(testIdentifier);
        
        // Verify attempts are recorded
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isEqualTo(2);

        // When
        loginAttemptService.recordSuccessfulLogin(testIdentifier);

        // Then
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isZero();
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isFalse();
    }

    @Test
    void recordFailedLogin_ShouldIncrementAttempts() {
        // When
        loginAttemptService.recordFailedLogin(testIdentifier);
        loginAttemptService.recordFailedLogin(testIdentifier);

        // Then
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isEqualTo(2);
    }

    @Test
    void isBlocked_WithNoAttempts_ShouldReturnFalse() {
        // When & Then
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isFalse();
    }

    @Test
    void isBlocked_WithAttemptsUnderLimit_ShouldReturnFalse() {
        // Given - make attempts under the limit
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS - 1; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }

        // When & Then
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isFalse();
    }

    @Test
    void isBlocked_WithAttemptsOverLimit_ShouldReturnTrue() {
        // Given - exceed the limit
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }

        // When & Then
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isTrue();
    }

    @Test
    void isBlocked_AfterLockTimeExpired_ShouldReturnFalse() throws Exception {
        // Given - exceed the limit to get blocked
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isTrue();

        // Simulate time passing by manipulating the internal state
        manipulateLastAttemptTime(testIdentifier, 
            LocalDateTime.now().minusMinutes(SecurityConstants.LOGIN_LOCK_DURATION_MINUTES + 1));

        // When & Then
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isFalse();
    }

    @Test
    void getRemainingLockTime_WithNoAttempts_ShouldReturnZero() {
        // When & Then
        assertThat(loginAttemptService.getRemainingLockTime(testIdentifier)).isZero();
    }

    @Test
    void getRemainingLockTime_WithAttemptsUnderLimit_ShouldReturnZero() {
        // Given
        loginAttemptService.recordFailedLogin(testIdentifier);
        loginAttemptService.recordFailedLogin(testIdentifier);

        // When & Then
        assertThat(loginAttemptService.getRemainingLockTime(testIdentifier)).isZero();
    }

    @Test
    void getRemainingLockTime_WithBlockedAccount_ShouldReturnPositiveValue() {
        // Given - block the account
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }

        // When & Then
        assertThat(loginAttemptService.getRemainingLockTime(testIdentifier)).isGreaterThan(0);
        assertThat(loginAttemptService.getRemainingLockTime(testIdentifier))
                .isLessThanOrEqualTo(SecurityConstants.LOGIN_LOCK_DURATION_MINUTES);
    }

    @Test
    void getAttempts_WithNoAttempts_ShouldReturnZero() {
        // When & Then
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isZero();
    }

    @Test
    void getAttempts_WithMultipleAttempts_ShouldReturnCorrectCount() {
        // Given
        int attemptCount = 3;
        for (int i = 0; i < attemptCount; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }

        // When & Then
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isEqualTo(attemptCount);
    }

    @Test
    void clearLoginAttempts_ShouldResetAttemptsAndUnblock() {
        // Given - block the account
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isTrue();

        // When
        loginAttemptService.clearLoginAttempts(testIdentifier);

        // Then
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isZero();
        assertThat(loginAttemptService.isBlocked(testIdentifier)).isFalse();
        assertThat(loginAttemptService.getRemainingLockTime(testIdentifier)).isZero();
    }

    @Test
    void getLoginAttemptStatus_WithNoAttempts_ShouldReturnCleanStatus() {
        // When
        LoginAttemptService.LoginAttemptStatus status = 
                loginAttemptService.getLoginAttemptStatus(testIdentifier);

        // Then
        assertThat(status.isBlocked()).isFalse();
        assertThat(status.getAttempts()).isZero();
        assertThat(status.getMaxAttempts()).isEqualTo(SecurityConstants.MAX_LOGIN_ATTEMPTS);
        assertThat(status.getRemainingLockTime()).isZero();
        assertThat(status.getRemainingAttempts()).isEqualTo(SecurityConstants.MAX_LOGIN_ATTEMPTS);
    }

    @Test
    void getLoginAttemptStatus_WithSomeAttempts_ShouldReturnCorrectStatus() {
        // Given
        int attempts = 2;
        for (int i = 0; i < attempts; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }

        // When
        LoginAttemptService.LoginAttemptStatus status = 
                loginAttemptService.getLoginAttemptStatus(testIdentifier);

        // Then
        assertThat(status.isBlocked()).isFalse();
        assertThat(status.getAttempts()).isEqualTo(attempts);
        assertThat(status.getMaxAttempts()).isEqualTo(SecurityConstants.MAX_LOGIN_ATTEMPTS);
        assertThat(status.getRemainingLockTime()).isZero();
        assertThat(status.getRemainingAttempts())
                .isEqualTo(SecurityConstants.MAX_LOGIN_ATTEMPTS - attempts);
    }

    @Test
    void getLoginAttemptStatus_WithBlockedAccount_ShouldReturnBlockedStatus() {
        // Given - block the account
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS; i++) {
            loginAttemptService.recordFailedLogin(testIdentifier);
        }

        // When
        LoginAttemptService.LoginAttemptStatus status = 
                loginAttemptService.getLoginAttemptStatus(testIdentifier);

        // Then
        assertThat(status.isBlocked()).isTrue();
        assertThat(status.getAttempts()).isEqualTo(SecurityConstants.MAX_LOGIN_ATTEMPTS);
        assertThat(status.getMaxAttempts()).isEqualTo(SecurityConstants.MAX_LOGIN_ATTEMPTS);
        assertThat(status.getRemainingLockTime()).isGreaterThan(0);
        assertThat(status.getRemainingAttempts()).isZero();
    }

    @Test
    void multipleIdentifiers_ShouldBeTrackedSeparately() {
        // Given
        String identifier1 = "user1@example.com";
        String identifier2 = "user2@example.com";

        // When
        loginAttemptService.recordFailedLogin(identifier1);
        loginAttemptService.recordFailedLogin(identifier1);
        loginAttemptService.recordFailedLogin(identifier2);

        // Then
        assertThat(loginAttemptService.getAttempts(identifier1)).isEqualTo(2);
        assertThat(loginAttemptService.getAttempts(identifier2)).isEqualTo(1);
        assertThat(loginAttemptService.isBlocked(identifier1)).isFalse();
        assertThat(loginAttemptService.isBlocked(identifier2)).isFalse();
    }

    @Test
    void concurrentAccess_ShouldHandleCorrectly() throws InterruptedException {
        // Given
        int threadCount = 10;
        int attemptsPerThread = 2;
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < attemptsPerThread; j++) {
                    loginAttemptService.recordFailedLogin(testIdentifier);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        assertThat(loginAttemptService.getAttempts(testIdentifier))
                .isEqualTo(threadCount * attemptsPerThread);
    }

    @Test
    void attemptResetAfterTime_ShouldClearAttempts() throws Exception {
        // Given
        loginAttemptService.recordFailedLogin(testIdentifier);
        loginAttemptService.recordFailedLogin(testIdentifier);
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isEqualTo(2);

        // Simulate time passing beyond reset time
        manipulateFirstAttemptTime(testIdentifier, 
            LocalDateTime.now().minusMinutes(SecurityConstants.LOGIN_ATTEMPT_RESET_MINUTES + 1));

        // When
        boolean blocked = loginAttemptService.isBlocked(testIdentifier);

        // Then
        assertThat(blocked).isFalse();
        assertThat(loginAttemptService.getAttempts(testIdentifier)).isZero();
    }

    @Test
    void loginAttemptStatus_getRemainingAttempts_ShouldCalculateCorrectly() {
        // Given
        LoginAttemptService.LoginAttemptStatus status1 = 
                new LoginAttemptService.LoginAttemptStatus(false, 2, 5, 0);
        LoginAttemptService.LoginAttemptStatus status2 = 
                new LoginAttemptService.LoginAttemptStatus(true, 5, 5, 10);

        // When & Then
        assertThat(status1.getRemainingAttempts()).isEqualTo(3);
        assertThat(status2.getRemainingAttempts()).isZero();
    }

    @Test
    void recordFailedLogin_ShouldSetFirstAttemptTimeOnlyOnce() throws Exception {
        // When
        loginAttemptService.recordFailedLogin(testIdentifier);
        LocalDateTime firstTime = getFirstAttemptTime(testIdentifier);
        
        // Add small delay and record another attempt
        Thread.sleep(10);
        loginAttemptService.recordFailedLogin(testIdentifier);
        LocalDateTime secondTime = getFirstAttemptTime(testIdentifier);

        // Then
        assertThat(firstTime).isEqualTo(secondTime);
    }

    // Helper methods to manipulate internal state for testing

    private void manipulateLastAttemptTime(String identifier, LocalDateTime newTime) throws Exception {
        ConcurrentMap<String, Object> attemptCache = getAttemptCache();
        Object attemptInfo = attemptCache.get(identifier);
        if (attemptInfo != null) {
            Field lastAttemptField = attemptInfo.getClass().getDeclaredField("lastAttempt");
            lastAttemptField.setAccessible(true);
            lastAttemptField.set(attemptInfo, newTime);
        }
    }

    private void manipulateFirstAttemptTime(String identifier, LocalDateTime newTime) throws Exception {
        ConcurrentMap<String, Object> attemptCache = getAttemptCache();
        Object attemptInfo = attemptCache.get(identifier);
        if (attemptInfo != null) {
            Field firstAttemptField = attemptInfo.getClass().getDeclaredField("firstAttempt");
            firstAttemptField.setAccessible(true);
            firstAttemptField.set(attemptInfo, newTime);
        }
    }

    private LocalDateTime getFirstAttemptTime(String identifier) throws Exception {
        ConcurrentMap<String, Object> attemptCache = getAttemptCache();
        Object attemptInfo = attemptCache.get(identifier);
        if (attemptInfo != null) {
            Field firstAttemptField = attemptInfo.getClass().getDeclaredField("firstAttempt");
            firstAttemptField.setAccessible(true);
            return (LocalDateTime) firstAttemptField.get(attemptInfo);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, Object> getAttemptCache() throws Exception {
        Field cacheField = LoginAttemptService.class.getDeclaredField("attemptCache");
        cacheField.setAccessible(true);
        return (ConcurrentMap<String, Object>) cacheField.get(loginAttemptService);
    }
}