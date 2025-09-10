# タスク05: レビューシステムAPI実装

## タスク概要
書籍に対するレビュー投稿・閲覧機能のAPI実装します。

## 実装内容

### 1. ReviewsController実装
```java
@RestController
@RequestMapping("/api/books/{bookId}/reviews")
public class ReviewsController {
    
    @GetMapping
    public ResponseEntity<List<Review>> getReviews(@PathVariable Long bookId) {
        // レビュー一覧取得
    }
    
    @PostMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> postReview(
        @PathVariable Long bookId,
        @Valid @RequestBody PostReviewRequest request,
        Authentication auth
    ) {
        // レビュー投稿
    }
}
```

### 2. ReviewService実装
- レビュー投稿ロジック
- 平均評価計算
- 投稿権限チェック（購入者のみ）
- 重複投稿チェック

### 3. 評価システム設計
- 5段階評価システム
- 平均評価の自動計算・更新
- レビュー数カウント

## 受け入れ条件
- [ ] 書籍のレビュー一覧を取得できる
- [ ] 認証済みユーザーがレビューを投稿できる
- [ ] 5段階評価とコメントを投稿できる
- [ ] 同一ユーザーは1書籍につき1レビューのみ
- [ ] 購入履歴があるユーザーのみ投稿可能
- [ ] 平均評価が自動計算される
- [ ] レビュー投稿時に書籍の評価情報が更新される

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/books/reviews/ReviewsController.java`
- `src/main/java/jp/readscape/api/services/ReviewService.java`
- `src/main/java/jp/readscape/api/domain/books/model/Review.java`
- `src/main/java/jp/readscape/api/controllers/books/reviews/request/PostReviewRequest.java`