# タスク02: ショッピングカートAPI実装

## タスク概要
ユーザーが書籍をカートに追加・管理するためのAPI機能を実装します。

## 実装内容

### 1. CartsController実装
```java
@RestController
@RequestMapping("/api/cart")
public class CartsController {
    
    @GetMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<CartResponse> getCart(Authentication auth) {
        // カート内容取得
    }
    
    @PostMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> addToCart(
        @Valid @RequestBody AddToCartRequest request,
        Authentication auth
    ) {
        // カートに追加
    }
    
    @PutMapping("/{bookId}")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> updateCartQuantity(
        @PathVariable Long bookId,
        @Valid @RequestBody UpdateCartQuantityRequest request,
        Authentication auth
    ) {
        // 数量変更
    }
    
    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> removeFromCart(
        @PathVariable Long bookId,
        Authentication auth
    ) {
        // カートから削除
    }
}
```

### 2. CartService実装
- カート作成・取得ロジック
- アイテム追加・更新・削除ロジック
- 在庫数チェック機能
- 価格計算機能

### 3. エンティティ設計
- Cart エンティティ
- CartItem エンティティ
- User との関連設定

## 受け入れ条件
- [ ] 認証済みユーザーのみカート操作可能
- [ ] カート内容を取得できる
- [ ] 書籍をカートに追加できる
- [ ] カート内書籍の数量を変更できる
- [ ] カートから書籍を削除できる
- [ ] 在庫数を超える数量は追加できない
- [ ] 合計金額が正しく計算される

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/carts/CartsController.java`
- `src/main/java/jp/readscape/api/services/CartService.java`
- `src/main/java/jp/readscape/api/domain/carts/model/Cart.java`
- `src/main/java/jp/readscape/api/domain/carts/model/CartItem.java`