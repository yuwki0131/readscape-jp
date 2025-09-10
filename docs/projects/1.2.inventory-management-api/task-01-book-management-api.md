# タスク01: 書籍管理API実装

## タスク概要
管理者が書籍情報の登録・更新・削除を行うためのAPI機能を実装します。

## 実装内容

### 1. AdminBooksController実装
```java
@RestController
@RequestMapping("/api/admin/books")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminBooksController {
    
    @GetMapping
    public ResponseEntity<List<AdminBookView>> getBooks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status
    ) {
        // 管理者向け書籍一覧
    }
    
    @PostMapping
    public ResponseEntity<CreateBookResponse> createBook(
        @Valid @RequestBody CreateBookRequest request
    ) {
        // 新規書籍登録
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateBook(
        @PathVariable Long id,
        @Valid @RequestBody UpdateBookRequest request
    ) {
        // 書籍情報更新
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteBook(@PathVariable Long id) {
        // 書籍削除
    }
}
```

### 2. AdminBookService実装
- 書籍CRUD操作
- 画像アップロード処理
- カテゴリー管理
- 変更履歴記録

### 3. バルク操作機能
- CSV一括インポート
- Excel一括エクスポート
- バッチ更新処理

## 受け入れ条件
- [ ] 管理者権限でのみアクセス可能
- [ ] 書籍の新規登録ができる
- [ ] 書籍情報を更新できる
- [ ] 書籍を削除できる（ADMIN権限のみ）
- [ ] 管理者向け書籍一覧を取得できる
- [ ] バリデーションが適切に機能する
- [ ] 変更履歴が記録される

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/admin/books/AdminBooksController.java`
- `src/main/java/jp/readscape/api/services/AdminBookService.java`
- `src/main/java/jp/readscape/api/controllers/admin/books/request/CreateBookRequest.java`