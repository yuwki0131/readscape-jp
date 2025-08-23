# タスク03: ユーザー管理API実装

## タスク概要
ユーザー登録、ログイン、プロフィール管理のためのAPI機能を実装します。

## 実装内容

### 1. UsersController実装
```java
@RestController
@RequestMapping("/api/users")
public class UsersController {
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(
        @Valid @RequestBody RegisterUserRequest request
    ) {
        // ユーザー登録
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        // ログイン
    }
    
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<UserProfile> getProfile(Authentication auth) {
        // プロフィール取得
    }
    
    @PutMapping("/profile")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request,
        Authentication auth
    ) {
        // プロフィール更新
    }
    
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<List<OrderSummary>> getOrders(Authentication auth) {
        // 注文履歴取得
    }
}
```

### 2. UserService実装
- ユーザー登録ロジック
- パスワード暗号化
- ユーザー情報更新ロジック
- 重複チェック機能

### 3. バリデーション実装
- メールアドレス形式チェック
- パスワード強度チェック
- ユーザー名重複チェック

## 受け入れ条件
- [ ] 新規ユーザーを登録できる
- [ ] メールアドレス・ユーザー名の重複チェック
- [ ] パスワードがハッシュ化されて保存される
- [ ] ログイン機能が動作する
- [ ] 認証済みユーザーがプロフィールを取得・更新できる
- [ ] 注文履歴を取得できる
- [ ] 入力値バリデーションが機能する

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/users/UsersController.java`
- `src/main/java/jp/readscape/api/services/UserService.java`
- `src/main/java/jp/readscape/api/domain/users/model/User.java`
- `src/main/java/jp/readscape/api/controllers/users/request/RegisterUserRequest.java`