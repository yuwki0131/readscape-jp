# タスク03: 注文管理API実装

## タスク概要
管理者による注文状況確認、ステータス更新、配送管理のためのAPI機能を実装します。

## 実装内容

### 1. AdminOrdersController実装
```java
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminOrdersController {
    
    @GetMapping
    public ResponseEntity<Page<AdminOrderView>> getOrders(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        // 注文一覧取得
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderDetail> getOrderById(@PathVariable Long id) {
        // 注文詳細取得
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateOrderStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        // 注文ステータス更新
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<PendingOrder>> getPendingOrders() {
        // 処理待ち注文取得
    }
}
```

### 2. AdminOrderService実装
- 注文検索・フィルタリング
- ステータス更新ロジック
- 配送管理機能
- 注文キャンセル処理

### 3. 注文ステータス管理
- ステータス遷移制御
- 通知機能連携
- 履歴管理

## 受け入れ条件
- [ ] 全注文一覧を取得できる
- [ ] ステータス別に注文をフィルタリングできる
- [ ] 注文詳細を確認できる
- [ ] 注文ステータスを更新できる
- [ ] 処理待ち注文を優先表示できる
- [ ] 配送情報を管理できる
- [ ] ステータス更新履歴を記録する

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/admin/orders/AdminOrdersController.java`
- `src/main/java/jp/readscape/api/services/AdminOrderService.java`
- `src/main/java/jp/readscape/api/controllers/admin/orders/response/AdminOrderView.java`