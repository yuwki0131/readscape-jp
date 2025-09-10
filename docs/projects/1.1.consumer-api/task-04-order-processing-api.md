# タスク04: 注文処理API実装

## タスク概要
カート内容からの注文作成と注文管理のためのAPI機能を実装します。

## 実装内容

### 1. OrdersController実装
```java
@RestController
@RequestMapping("/api/orders")
public class OrdersController {
    
    @PostMapping
    @PreAuthorize("hasRole('CONSUMER')")
    @Transactional
    public ResponseEntity<CreateOrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request,
        Authentication auth
    ) {
        // 注文作成
    }
    
    @GetMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<List<OrderSummary>> getOrders(Authentication auth) {
        // 注文履歴取得
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<OrderDetail> getOrderById(
        @PathVariable Long id,
        Authentication auth
    ) {
        // 注文詳細取得
    }
}
```

### 2. OrderService実装
- カートから注文への変換ロジック
- 在庫減算処理
- 注文ステータス管理
- 合計金額計算

### 3. トランザクション管理
- 注文作成の原子性保証
- 在庫減算の整合性確保
- エラー時のロールバック処理

## 受け入れ条件
- [ ] カート内容から注文を作成できる
- [ ] 注文作成時に在庫が正しく減算される
- [ ] 在庫不足時はエラーを返す
- [ ] 注文作成はトランザクション内で実行される
- [ ] ユーザーの注文履歴を取得できる
- [ ] 注文詳細を取得できる（本人のみ）
- [ ] 注文ステータスが正しく管理される

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/orders/OrdersController.java`
- `src/main/java/jp/readscape/api/services/OrderService.java`
- `src/main/java/jp/readscape/api/domain/orders/model/Order.java`
- `src/main/java/jp/readscape/api/domain/orders/model/OrderItem.java`