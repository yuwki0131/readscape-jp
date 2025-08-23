# タスク01: 書籍閲覧API実装

## タスク概要
一般消費者が書籍を閲覧・検索するためのREST APIエンドポイントを実装します。

## 実装内容

### 1. BooksController実装
```java
@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "書籍閲覧API")
public class BooksController {
    
    @GetMapping
    public ResponseEntity<BooksResponse> getBooks(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        // 実装
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookDetail> getBookById(@PathVariable Long id) {
        // 実装
    }
}
```

### 2. BookService実装
- 書籍一覧取得ロジック
- カテゴリー別フィルタリング
- キーワード検索機能
- ページネーション処理

### 3. DTOクラス作成
- BookSummary: 一覧表示用
- BookDetail: 詳細表示用
- BooksResponse: レスポンス用

## 受け入れ条件
- [ ] GET /api/books で書籍一覧を取得できる
- [ ] クエリパラメータでカテゴリー、キーワード検索ができる
- [ ] ページネーション機能が動作する
- [ ] GET /api/books/{id} で書籍詳細を取得できる
- [ ] 存在しない書籍IDで404エラーを返す

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/books/BooksController.java`
- `src/main/java/jp/readscape/api/services/BookService.java`
- `src/main/java/jp/readscape/api/controllers/books/response/BookSummary.java`
- `src/main/java/jp/readscape/api/controllers/books/response/BookDetail.java`