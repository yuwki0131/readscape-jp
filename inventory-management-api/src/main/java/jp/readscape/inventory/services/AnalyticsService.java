package jp.readscape.inventory.services;

import jp.readscape.inventory.domain.books.repository.BookRepository;
import jp.readscape.inventory.domain.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;

    /**
     * 売上分析データ取得
     */
    public SalesAnalytics getSalesAnalytics(LocalDateTime startDate, LocalDateTime endDate, String granularity) {
        log.debug("Getting sales analytics - startDate: {}, endDate: {}, granularity: {}", 
                 startDate, endDate, granularity);

        // 基本統計の計算
        Long totalOrders = orderRepository.countByOrderDateBetween(startDate, endDate);
        
        // 簡略化された売上計算（実際のRepositoryメソッドが複雑な場合の代替）
        List<Object[]> salesData = orderRepository.findSalesDataBetween(startDate, endDate);
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        
        if (!salesData.isEmpty()) {
            totalRevenue = salesData.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalOrders > 0) {
                averageOrderValue = totalRevenue.divide(
                    BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
            }
        }

        // 時系列データの生成
        List<SalesDataPoint> salesTimeSeries = generateTimeSeries(startDate, endDate, granularity);

        return SalesAnalytics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .granularity(granularity)
                .salesTimeSeries(salesTimeSeries)
                .build();
    }

    /**
     * 人気書籍ランキング取得
     */
    public List<PopularBook> getPopularBooks(int days, int limit) {
        log.debug("Getting popular books - days: {}, limit: {}", days, limit);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        
        List<Object[]> popularBooksData = orderRepository.findPopularBooks(startDate, pageable);
        
        return popularBooksData.stream()
                .map(row -> PopularBook.builder()
                        .bookId((Long) row[0])
                        .title((String) row[1])
                        .author((String) row[2])
                        .totalSold(((Number) row[3]).intValue())
                        .totalRevenue((BigDecimal) row[4])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * カテゴリ別売上取得
     */
    public List<CategorySales> getCategorySales(int days) {
        log.debug("Getting category sales - days: {}", days);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> categoryData = orderRepository.findCategorySales(startDate);
        
        return categoryData.stream()
                .map(row -> CategorySales.builder()
                        .categoryName((String) row[0])
                        .totalRevenue((BigDecimal) row[1])
                        .totalOrders(((Number) row[2]).longValue())
                        .totalBooks(((Number) row[3]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * ダッシュボード情報取得
     */
    public DashboardInfo getDashboardInfo() {
        log.debug("Getting dashboard info");

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);

        // 今日の統計
        Long todayOrders = orderRepository.countByOrderDateBetween(today, LocalDateTime.now());
        BigDecimal todayRevenue = orderRepository.sumRevenueByOrderDateBetween(today, LocalDateTime.now());
        
        // 週間統計
        Long weeklyOrders = orderRepository.countByOrderDateBetween(weekAgo, LocalDateTime.now());
        BigDecimal weeklyRevenue = orderRepository.sumRevenueByOrderDateBetween(weekAgo, LocalDateTime.now());
        
        // 月間統計
        Long monthlyOrders = orderRepository.countByOrderDateBetween(monthAgo, LocalDateTime.now());
        BigDecimal monthlyRevenue = orderRepository.sumRevenueByOrderDateBetween(monthAgo, LocalDateTime.now());
        
        // 在庫統計
        Long totalBooks = bookRepository.count();
        Long lowStockBooks = bookRepository.countByStockQuantityLessThanEqual(10);
        
        return DashboardInfo.builder()
                .todayOrders(todayOrders != null ? todayOrders : 0L)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .weeklyOrders(weeklyOrders != null ? weeklyOrders : 0L)
                .weeklyRevenue(weeklyRevenue != null ? weeklyRevenue : BigDecimal.ZERO)
                .monthlyOrders(monthlyOrders != null ? monthlyOrders : 0L)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .totalBooks(totalBooks)
                .lowStockBooks(lowStockBooks != null ? lowStockBooks : 0L)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * 月別売上推移取得
     */
    public MonthlySalesTrend getMonthlySalesTrend(int months) {
        log.debug("Getting monthly sales trend - months: {}", months);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(months);
        
        List<Object[]> monthlyData = orderRepository.findMonthlySales(startDate, endDate);
        
        List<MonthlySalesData> salesData = monthlyData.stream()
                .map(row -> MonthlySalesData.builder()
                        .month((String) row[0])
                        .revenue((BigDecimal) row[1])
                        .orders(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        return MonthlySalesTrend.builder()
                .startDate(startDate)
                .endDate(endDate)
                .monthlyData(salesData)
                .build();
    }

    /**
     * 顧客分析取得
     */
    public CustomerAnalytics getCustomerAnalytics(int days, int topCustomerLimit) {
        log.debug("Getting customer analytics - days: {}, limit: {}", days, topCustomerLimit);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // トップ顧客データ
        Pageable pageable = PageRequest.of(0, topCustomerLimit);
        List<Object[]> topCustomersData = orderRepository.findTopCustomers(startDate, pageable);
        
        List<TopCustomer> topCustomers = topCustomersData.stream()
                .map(row -> TopCustomer.builder()
                        .userId((Long) row[0])
                        .username((String) row[1])
                        .email((String) row[2])
                        .totalSpent((BigDecimal) row[3])
                        .orderCount(((Number) row[4]).intValue())
                        .build())
                .collect(Collectors.toList());

        // 顧客統計
        Long totalCustomers = orderRepository.countDistinctCustomers(startDate);
        Long newCustomers = orderRepository.countNewCustomers(startDate);
        Double averageOrdersPerCustomer = orderRepository.calculateAverageOrdersPerCustomer(startDate);

        return CustomerAnalytics.builder()
                .totalCustomers(totalCustomers != null ? totalCustomers : 0L)
                .newCustomers(newCustomers != null ? newCustomers : 0L)
                .averageOrdersPerCustomer(averageOrdersPerCustomer != null ? averageOrdersPerCustomer : 0.0)
                .topCustomers(topCustomers)
                .build();
    }

    /**
     * 在庫回転率取得
     */
    public List<InventoryTurnover> getInventoryTurnover(int days) {
        log.debug("Getting inventory turnover - days: {}", days);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> turnoverData = orderRepository.findInventoryTurnover(startDate);
        
        return turnoverData.stream()
                .map(row -> InventoryTurnover.builder()
                        .bookId((Long) row[0])
                        .title((String) row[1])
                        .currentStock(((Number) row[2]).intValue())
                        .soldQuantity(((Number) row[3]).intValue())
                        .turnoverRate(calculateTurnoverRate(
                            ((Number) row[2]).intValue(), 
                            ((Number) row[3]).intValue(), 
                            days))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * レポートエクスポート
     */
    public ExportResult exportReport(String type, String format, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Exporting report - type: {}, format: {}, startDate: {}, endDate: {}", 
                 type, format, startDate, endDate);

        String filename = generateFilename(type, format);
        byte[] data;

        try {
            data = switch (type.toLowerCase()) {
                case "sales" -> exportSalesReport(format, startDate, endDate);
                case "inventory" -> exportInventoryReport(format);
                case "customers" -> exportCustomerReport(format, startDate, endDate);
                case "books" -> exportBooksReport(format);
                default -> throw new IllegalArgumentException("Unknown report type: " + type);
            };
        } catch (Exception e) {
            log.error("Failed to export report: type={}, format={}", type, format, e);
            throw new RuntimeException("レポートのエクスポートに失敗しました: " + e.getMessage());
        }

        return ExportResult.builder()
                .filename(filename)
                .data(data)
                .contentType(getContentType(format))
                .build();
    }

    /**
     * リアルタイム統計取得
     */
    public RealtimeStats getRealtimeStats() {
        log.debug("Getting realtime stats");

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        Long todayOrders = orderRepository.countByOrderDateBetween(todayStart, now);
        BigDecimal todayRevenue = orderRepository.sumRevenueByOrderDateBetween(todayStart, now);
        
        // 過去1時間の注文
        LocalDateTime oneHourAgo = now.minusHours(1);
        Long ordersLastHour = orderRepository.countByOrderDateBetween(oneHourAgo, now);

        return RealtimeStats.builder()
                .todayOrders(todayOrders != null ? todayOrders : 0L)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .ordersLastHour(ordersLastHour != null ? ordersLastHour : 0L)
                .timestamp(now)
                .build();
    }

    // プライベートメソッド

    private List<SalesDataPoint> generateTimeSeries(LocalDateTime startDate, LocalDateTime endDate, String granularity) {
        // 簡単な実装例（実際はより複雑なロジックが必要）
        List<SalesDataPoint> dataPoints = new ArrayList<>();
        
        LocalDateTime current = startDate;
        while (current.isBefore(endDate)) {
            LocalDateTime nextPeriod = switch (granularity) {
                case "daily" -> current.plusDays(1);
                case "weekly" -> current.plusWeeks(1);
                case "monthly" -> current.plusMonths(1);
                default -> current.plusDays(1);
            };

            // 期間内の売上データを取得（簡略化）
            Long orders = orderRepository.countByOrderDateBetween(current, nextPeriod);
            BigDecimal revenue = orderRepository.sumRevenueByOrderDateBetween(current, nextPeriod);

            dataPoints.add(SalesDataPoint.builder()
                    .period(current.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .orders(orders != null ? orders : 0L)
                    .build());

            current = nextPeriod;
        }
        
        return dataPoints;
    }

    private double calculateTurnoverRate(int currentStock, int soldQuantity, int days) {
        if (currentStock <= 0) return 0.0;
        double averageInventory = (double) currentStock;
        double periodTurnover = (double) soldQuantity / averageInventory;
        return (periodTurnover * 365.0) / days; // 年間回転率に換算
    }

    private String generateFilename(String type, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = switch (format.toLowerCase()) {
            case "csv" -> ".csv";
            case "excel" -> ".xlsx";
            case "pdf" -> ".pdf";
            default -> ".txt";
        };
        return String.format("%s_report_%s%s", type, timestamp, extension);
    }

    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> "text/csv";
            case "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    // 実際のエクスポート処理（簡略化）
    private byte[] exportSalesReport(String format, LocalDateTime startDate, LocalDateTime endDate) {
        String content = String.format("Sales Report (%s to %s)\n", startDate, endDate);
        return content.getBytes();
    }

    private byte[] exportInventoryReport(String format) {
        String content = "Inventory Report\n";
        return content.getBytes();
    }

    private byte[] exportCustomerReport(String format, LocalDateTime startDate, LocalDateTime endDate) {
        String content = String.format("Customer Report (%s to %s)\n", startDate, endDate);
        return content.getBytes();
    }

    private byte[] exportBooksReport(String format) {
        String content = "Books Report\n";
        return content.getBytes();
    }

    // DTOクラス

    @lombok.Builder
    @lombok.Data
    public static class SalesAnalytics {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal totalRevenue;
        private Long totalOrders;
        private BigDecimal averageOrderValue;
        private String granularity;
        private List<SalesDataPoint> salesTimeSeries;
    }

    @lombok.Builder
    @lombok.Data
    public static class SalesDataPoint {
        private String period;
        private BigDecimal revenue;
        private Long orders;
    }

    @lombok.Builder
    @lombok.Data
    public static class PopularBook {
        private Long bookId;
        private String title;
        private String author;
        private Integer totalSold;
        private BigDecimal totalRevenue;
    }

    @lombok.Builder
    @lombok.Data
    public static class CategorySales {
        private String categoryName;
        private BigDecimal totalRevenue;
        private Long totalOrders;
        private Integer totalBooks;
    }

    @lombok.Builder
    @lombok.Data
    public static class DashboardInfo {
        private Long todayOrders;
        private BigDecimal todayRevenue;
        private Long weeklyOrders;
        private BigDecimal weeklyRevenue;
        private Long monthlyOrders;
        private BigDecimal monthlyRevenue;
        private Long totalBooks;
        private Long lowStockBooks;
        private LocalDateTime lastUpdated;
    }

    @lombok.Builder
    @lombok.Data
    public static class MonthlySalesTrend {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private List<MonthlySalesData> monthlyData;
    }

    @lombok.Builder
    @lombok.Data
    public static class MonthlySalesData {
        private String month;
        private BigDecimal revenue;
        private Long orders;
    }

    @lombok.Builder
    @lombok.Data
    public static class CustomerAnalytics {
        private Long totalCustomers;
        private Long newCustomers;
        private Double averageOrdersPerCustomer;
        private List<TopCustomer> topCustomers;
    }

    @lombok.Builder
    @lombok.Data
    public static class TopCustomer {
        private Long userId;
        private String username;
        private String email;
        private BigDecimal totalSpent;
        private Integer orderCount;
    }

    @lombok.Builder
    @lombok.Data
    public static class InventoryTurnover {
        private Long bookId;
        private String title;
        private Integer currentStock;
        private Integer soldQuantity;
        private Double turnoverRate;
    }

    @lombok.Builder
    @lombok.Data
    public static class ExportResult {
        private String filename;
        private byte[] data;
        private String contentType;
    }

    @lombok.Builder
    @lombok.Data
    public static class RealtimeStats {
        private Long todayOrders;
        private BigDecimal todayRevenue;
        private Long ordersLastHour;
        private LocalDateTime timestamp;
    }
}