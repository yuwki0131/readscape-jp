package jp.readscape.inventory.controllers.admin.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.readscape.inventory.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ANALYST')")
@Tag(name = "Analytics", description = "分析・統計API")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
        summary = "売上分析取得",
        description = "指定期間の売上統計を取得します。日別、週別、月別の粒度を選択できます。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "売上分析取得成功"),
        @ApiResponse(responseCode = "400", description = "パラメータエラー"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/sales")
    public ResponseEntity<AnalyticsService.SalesAnalytics> getSalesAnalytics(
            @Parameter(description = "開始日", example = "2024-01-01", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "終了日", example = "2024-01-31", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "集計粒度", example = "daily", 
                schema = @Schema(allowableValues = {"daily", "weekly", "monthly"}))
            @RequestParam(defaultValue = "daily") String granularity
    ) {
        log.info("GET /admin/analytics/sales - startDate: {}, endDate: {}, granularity: {}", 
                startDate, endDate, granularity);

        AnalyticsService.SalesAnalytics analytics = analyticsService.getSalesAnalytics(
            startDate.atStartOfDay(), endDate.atTime(23, 59, 59), granularity);
        
        return ResponseEntity.ok(analytics);
    }

    @Operation(
        summary = "人気書籍ランキング取得",
        description = "指定期間の人気書籍ランキングを取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "人気書籍ランキング取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/books/popular")
    public ResponseEntity<List<AnalyticsService.PopularBook>> getPopularBooks(
            @Parameter(description = "過去日数", example = "30")
            @RequestParam(defaultValue = "30") int days,
            
            @Parameter(description = "取得件数", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("GET /admin/analytics/books/popular - days: {}, limit: {}", days, limit);

        List<AnalyticsService.PopularBook> popularBooks = analyticsService.getPopularBooks(days, limit);
        return ResponseEntity.ok(popularBooks);
    }

    @Operation(
        summary = "カテゴリ別売上取得",
        description = "指定期間のカテゴリ別売上統計を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "カテゴリ別売上取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/categories")
    public ResponseEntity<List<AnalyticsService.CategorySales>> getCategorySales(
            @Parameter(description = "過去日数", example = "30")
            @RequestParam(defaultValue = "30") int days
    ) {
        log.info("GET /admin/analytics/categories - days: {}", days);

        List<AnalyticsService.CategorySales> categorySales = analyticsService.getCategorySales(days);
        return ResponseEntity.ok(categorySales);
    }

    @Operation(
        summary = "ダッシュボード情報取得",
        description = "管理ダッシュボード用の統計情報を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ダッシュボード情報取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsService.DashboardInfo> getDashboardInfo() {
        log.info("GET /admin/analytics/dashboard");

        AnalyticsService.DashboardInfo dashboardInfo = analyticsService.getDashboardInfo();
        return ResponseEntity.ok(dashboardInfo);
    }

    @Operation(
        summary = "月別売上推移取得",
        description = "過去12ヶ月の月別売上推移を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "月別売上推移取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/sales/monthly-trend")
    public ResponseEntity<AnalyticsService.MonthlySalesTrend> getMonthlySalesTrend(
            @Parameter(description = "過去月数", example = "12")
            @RequestParam(defaultValue = "12") int months
    ) {
        log.info("GET /admin/analytics/sales/monthly-trend - months: {}", months);

        AnalyticsService.MonthlySalesTrend trend = analyticsService.getMonthlySalesTrend(months);
        return ResponseEntity.ok(trend);
    }

    @Operation(
        summary = "顧客分析取得",
        description = "顧客別の購入統計を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "顧客分析取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/customers")
    public ResponseEntity<AnalyticsService.CustomerAnalytics> getCustomerAnalytics(
            @Parameter(description = "過去日数", example = "90")
            @RequestParam(defaultValue = "90") int days,
            
            @Parameter(description = "上位顧客取得数", example = "20")
            @RequestParam(defaultValue = "20") int topCustomerLimit
    ) {
        log.info("GET /admin/analytics/customers - days: {}, topCustomerLimit: {}", days, topCustomerLimit);

        AnalyticsService.CustomerAnalytics customerAnalytics = 
            analyticsService.getCustomerAnalytics(days, topCustomerLimit);
        
        return ResponseEntity.ok(customerAnalytics);
    }

    @Operation(
        summary = "在庫回転率分析取得",
        description = "書籍の在庫回転率を分析します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫回転率分析取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/inventory/turnover")
    public ResponseEntity<List<AnalyticsService.InventoryTurnover>> getInventoryTurnover(
            @Parameter(description = "過去日数", example = "90")
            @RequestParam(defaultValue = "90") int days
    ) {
        log.info("GET /admin/analytics/inventory/turnover - days: {}", days);

        List<AnalyticsService.InventoryTurnover> turnoverData = 
            analyticsService.getInventoryTurnover(days);
        
        return ResponseEntity.ok(turnoverData);
    }

    @Operation(
        summary = "レポートエクスポート",
        description = "各種レポートをCSV、Excel、PDF形式でエクスポートします"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "レポートエクスポート成功"),
        @ApiResponse(responseCode = "400", description = "無効なレポートタイプまたは形式"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(
            @Parameter(description = "レポートタイプ", example = "sales", required = true,
                schema = @Schema(allowableValues = {"sales", "inventory", "customers", "books"}))
            @RequestParam String type,
            
            @Parameter(description = "出力形式", example = "csv", required = true,
                schema = @Schema(allowableValues = {"csv", "excel", "pdf"}))
            @RequestParam String format,
            
            @Parameter(description = "開始日", example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "終了日", example = "2024-01-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /admin/analytics/reports/export - type: {}, format: {}, startDate: {}, endDate: {}", 
                type, format, startDate, endDate);

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        AnalyticsService.ExportResult exportResult = analyticsService.exportReport(type, format, start, end);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(format));
        headers.setContentDispositionFormData("attachment", exportResult.getFilename());
        headers.setContentLength(exportResult.getData().length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(exportResult.getData());
    }

    @Operation(
        summary = "リアルタイム統計取得",
        description = "今日の売上・注文などのリアルタイム統計を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "リアルタイム統計取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/realtime")
    public ResponseEntity<AnalyticsService.RealtimeStats> getRealtimeStats() {
        log.info("GET /admin/analytics/realtime");

        AnalyticsService.RealtimeStats stats = analyticsService.getRealtimeStats();
        return ResponseEntity.ok(stats);
    }

    // プライベートメソッド

    private MediaType getMediaType(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "excel" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}