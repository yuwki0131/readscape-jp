# タスク03: セキュリティ機能実装

## タスク概要
パスワード強度チェック、ログイン試行制限、セキュリティログ記録などの包括的なセキュリティ機能を実装します。

## 実装内容

### 1. パスワードセキュリティ
```java
@Component
public class PasswordSecurityService {
    
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public boolean isValidPassword(String password) {
        // パスワード強度チェック
        // 8文字以上、大小英数字、記号を含む
    }
    
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
```

### 2. ログイン試行制限
```java
@Service
public class LoginAttemptService {
    
    private final Map<String, AttemptInfo> attemptCache = new ConcurrentHashMap<>();
    
    public void recordSuccessfulLogin(String email) {
        attemptCache.remove(email);
    }
    
    public void recordFailedLogin(String email) {
        AttemptInfo info = attemptCache.computeIfAbsent(email, 
            k -> new AttemptInfo());
        info.incrementAttempts();
    }
    
    public boolean isBlocked(String email) {
        AttemptInfo info = attemptCache.get(email);
        return info != null && info.isBlocked();
    }
}
```

### 3. セキュリティログ記録
```java
@Component
public class SecurityAuditService {
    
    public void logSuccessfulLogin(String email, String ipAddress) {
        // ログイン成功ログ
    }
    
    public void logFailedLogin(String email, String ipAddress, String reason) {
        // ログイン失敗ログ
    }
    
    public void logUnauthorizedAccess(String email, String resource, String ipAddress) {
        // 不正アクセスログ
    }
    
    public void logSuspiciousActivity(String email, String activity, String details) {
        // 不審な活動ログ
    }
}
```

### 4. CSRF・XSS対策
```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = 
            new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}
```

## 受け入れ条件
- [ ] パスワード強度チェックが機能する
- [ ] ログイン試行回数制限が動作する
- [ ] ブルートフォース攻撃を防げる
- [ ] セキュリティイベントがログ記録される
- [ ] CSRF攻撃対策が設定されている
- [ ] XSS攻撃対策が設定されている
- [ ] セキュリティヘッダーが適切に設定されている

## 関連ファイル
- `src/main/java/jp/readscape/api/services/security/PasswordSecurityService.java`
- `src/main/java/jp/readscape/api/services/security/LoginAttemptService.java`
- `src/main/java/jp/readscape/api/services/security/SecurityAuditService.java`