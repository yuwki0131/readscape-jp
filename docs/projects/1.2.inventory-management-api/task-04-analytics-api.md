# タスク04: 売上分析API実装

## タスク概要
売上統計、人気書籍ランキング、各種レポート生成のためのAPI機能を実装します。

## 実装内容

### 1. AnalyticsController実装
```java
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
public class AnalyticsController {
    
    @GetMapping("/sales")
    public ResponseEntity<SalesAnalytics> getSalesAnalytics(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate,
        @RequestParam(defaultValue = "daily") String granularity
    ) {
        // 売上統計
    }
    
    @GetMapping("/books/popular")
    public ResponseEntity<List<PopularBook>> getPopularBooks(
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(defaultValue = "10") int limit
    ) {
        // 人気書籍ランキング
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardInfo> getDashboardInfo() {
        // ダッシュボード情報
    }
    
    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(
        @RequestParam String type,
        @RequestParam String format
    ) {
        // レポートエクスポート
    }
}
```

### 2. AnalyticsService実装
- 売上集計ロジック
- ランキング算出
- レポート生成
- データ可視化用データ提供

### 3. レポート生成機能
- CSV/Excel出力
- PDF帳票生成
- グラフ表示用JSON API
- 定期レポート自動生成

## 受け入れ条件
- [ ] 期間別売上統計を取得できる
- [ ] 人気書籍ランキングを取得できる
- [ ] ダッシュボード情報を表示できる
- [ ] 各種レポートをエクスポートできる
- [ ] CSV、Excel、PDF形式に対応
- [ ] データの集計が正確である
- [ ] パフォーマンスが適切である

## 関連ファイル
- `src/main/java/jp/readscape/api/controllers/admin/analytics/AnalyticsController.java`
- `src/main/java/jp/readscape/api/services/AnalyticsService.java`
- `src/main/java/jp/readscape/api/controllers/admin/analytics/response/SalesAnalytics.java`