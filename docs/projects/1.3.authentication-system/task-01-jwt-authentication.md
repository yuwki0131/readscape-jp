# タスク01: JWT認証システム実装

## タスク概要
JWT トークンベースの認証システムを実装し、セキュアなAPI アクセス制御を提供します。

## 実装内容

### 1. JwtAuthenticationFilter実装
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        // JWT トークン検証
        // SecurityContext 設定
    }
}
```

### 2. JwtTokenProvider実装
```java
@Component
public class JwtTokenProvider {
    
    public String createToken(String email, List<String> roles) {
        // JWT トークン生成
    }
    
    public boolean validateToken(String token) {
        // トークン検証
    }
    
    public String getEmailFromToken(String token) {
        // メールアドレス抽出
    }
    
    public List<String> getRolesFromToken(String token) {
        // ロール抽出
    }
}
```

### 3. AuthController実装
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        // ログイン処理
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
        @Valid @RequestBody RefreshTokenRequest request
    ) {
        // トークン更新
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
        HttpServletRequest request
    ) {
        // ログアウト処理
    }
}
```

## 受け入れ条件
- [ ] JWT トークンを生成・検証できる
- [ ] ログイン時にトークンを発行する
- [ ] リフレッシュトークン機能が動作する
- [ ] トークンの有効期限が適切に管理される
- [ ] 無効なトークンは拒否される
- [ ] ログアウト時にトークンを無効化する
- [ ] セキュリティ設定が適切である

## 関連ファイル
- `src/main/java/jp/readscape/api/configurations/security/JwtAuthenticationFilter.java`
- `src/main/java/jp/readscape/api/configurations/security/JwtTokenProvider.java`
- `src/main/java/jp/readscape/api/controllers/auth/AuthController.java`