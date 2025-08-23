# タスク02: ロールベースアクセス制御実装

## タスク概要
ユーザーロールに基づく権限管理システムを実装し、適切なアクセス制御を提供します。

## 実装内容

### 1. UserRole列挙型定義
```java
public enum UserRole {
    CONSUMER("ROLE_CONSUMER", "一般消費者"),
    ADMIN("ROLE_ADMIN", "システム管理者"),
    MANAGER("ROLE_MANAGER", "在庫管理者"),
    ANALYST("ROLE_ANALYST", "分析担当者");
    
    private final String authority;
    private final String description;
}
```

### 2. SecurityConfig設定
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/books/**").permitAll()
                .requestMatchers("/api/cart/**").hasRole("CONSUMER")
                .requestMatchers("/api/orders/**").hasRole("CONSUMER")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### 3. メソッドレベル認可
```java
@Service
public class BookService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBook(Long bookId) {
        // 書籍削除（ADMIN のみ）
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateStock(Long bookId, int quantity) {
        // 在庫更新（ADMIN, MANAGER）
    }
    
    @PostAuthorize("returnObject.userId == authentication.principal.id or hasRole('ADMIN')")
    public Order findOrder(Long orderId) {
        // 注文取得（本人またはADMIN）
    }
}
```

## 受け入れ条件
- [ ] ユーザーロールが適切に定義されている
- [ ] URL パスベースの認可が機能する
- [ ] メソッドレベルの認可が機能する
- [ ] 権限のないユーザーは403エラーを受ける
- [ ] ロール階層が適切に設定されている
- [ ] セキュリティ設定が漏れなく適用されている

## 関連ファイル
- `src/main/java/jp/readscape/api/domain/users/model/UserRole.java`
- `src/main/java/jp/readscape/api/configurations/SecurityConfig.java`
- `src/main/java/jp/readscape/api/configurations/security/RoleHierarchyConfig.java`