# タスク02: 在庫管理API実装

## タスク概要
書籍在庫の管理、入荷処理、在庫アラート機能のAPI実装します。

## 実装内容

### 1. InventoryController実装
```java
@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class InventoryController {
    
    @GetMapping
    public ResponseEntity<List<InventoryItem>> getInventory() {
        // 在庫一覧取得
    }
    
    @PostMapping("/{bookId}/stock")
    public ResponseEntity<ApiResponse> updateStock(
        @PathVariable Long bookId,
        @Valid @RequestBody StockUpdateRequest request
    ) {
        // 入荷処理・在庫更新
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockItem>> getLowStockItems() {
        // 低在庫アラート
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<StockHistory>> getStockHistory(
        @RequestParam(required = false) Long bookId,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate
    ) {
        // 在庫変動履歴
    }
}
```

### 2. InventoryService実装
- 在庫数管理
- 入荷・出荷記録
- 低在庫アラート機能
- 在庫予測機能

### 3. 在庫管理機能
- リアルタイム在庫更新
- 自動発注機能
- 在庫移動管理
- 棚卸し機能

## 受け入れ条件
- [ ] 在庫一覧を取得できる
- [ ] 在庫数を更新できる
- [ ] 入荷処理を記録できる
- [ ] 低在庫商品をアラートできる
- [ ] 在庫変動履歴を確認できる
- [ ] 在庫数が負の値にならない
- [ ] 在庫更新が正確に反映される

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/admin/inventory/InventoryController.java`
- `src/main/java/jp/readscape/api/services/InventoryService.java`
- `src/main/java/jp/readscape/api/domain/books/model/StockHistory.java`