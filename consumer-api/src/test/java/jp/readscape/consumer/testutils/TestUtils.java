package jp.readscape.consumer.testutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * テストで共通的に使用されるユーティリティメソッドを提供するクラス
 */
public class TestUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    // ================================================================================
    // JSON 変換ユーティリティ
    // ================================================================================
    
    /**
     * オブジェクトをJSON文字列に変換
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON変換エラー: " + e.getMessage(), e);
        }
    }
    
    /**
     * JSON文字列をオブジェクトに変換
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析エラー: " + e.getMessage(), e);
        }
    }
    
    /**
     * 整形されたJSON文字列を生成（デバッグ用）
     */
    public static String toPrettyJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON整形エラー: " + e.getMessage(), e);
        }
    }
    
    // ================================================================================
    // MockMvc テスト用アサーションヘルパー
    // ================================================================================
    
    /**
     * JSONPath でリストサイズをチェック
     */
    public static ResultMatcher jsonPathListSize(String path, int expectedSize) {
        return jsonPath(path, hasSize(expectedSize));
    }
    
    /**
     * JSONPath で値の存在をチェック
     */
    public static ResultMatcher jsonPathExists(String path) {
        return jsonPath(path).exists();
    }
    
    /**
     * JSONPath で値の非存在をチェック
     */
    public static ResultMatcher jsonPathNotExists(String path) {
        return jsonPath(path).doesNotExist();
    }
    
    /**
     * JSONPath でBigDecimal値をチェック
     */
    public static ResultMatcher jsonPathBigDecimal(String path, BigDecimal expectedValue) {
        return jsonPath(path, comparesEqualTo(expectedValue));
    }
    
    /**
     * JSONPath でローカル日時をチェック
     */
    public static ResultMatcher jsonPathDateTime(String path, LocalDateTime expectedDateTime) {
        String expectedString = expectedDateTime.format(DATETIME_FORMATTER);
        return jsonPath(path, startsWith(expectedString.substring(0, 16))); // 秒以下は無視
    }
    
    /**
     * JSONPath で日付をチェック
     */
    public static ResultMatcher jsonPathDate(String path, LocalDate expectedDate) {
        String expectedString = expectedDate.format(DATE_FORMATTER);
        return jsonPath(path, is(expectedString));
    }
    
    /**
     * エラーレスポンスの基本構造をチェック
     */
    public static ResultMatcher[] errorResponseStructure() {
        return new ResultMatcher[]{
            jsonPathExists("$.error"),
            jsonPathExists("$.message"),
            jsonPathExists("$.status"),
            jsonPathExists("$.timestamp")
        };
    }
    
    /**
     * 成功レスポンスの基本構造をチェック
     */
    public static ResultMatcher[] successResponseStructure() {
        return new ResultMatcher[]{
            status().isOk(),
            content().contentType("application/json")
        };
    }
    
    /**
     * ページング情報の構造をチェック
     */
    public static ResultMatcher[] paginationStructure() {
        return new ResultMatcher[]{
            jsonPathExists("$.pagination"),
            jsonPathExists("$.pagination.currentPage"),
            jsonPathExists("$.pagination.totalPages"),
            jsonPathExists("$.pagination.totalElements"),
            jsonPathExists("$.pagination.size"),
            jsonPathExists("$.pagination.hasNext"),
            jsonPathExists("$.pagination.hasPrevious")
        };
    }
    
    // ================================================================================
    // 時間関連ユーティリティ
    // ================================================================================
    
    /**
     * 現在時刻から指定時間前の日時を生成
     */
    public static LocalDateTime minutesAgo(int minutes) {
        return LocalDateTime.now().minusMinutes(minutes);
    }
    
    /**
     * 現在時刻から指定時間後の日時を生成
     */
    public static LocalDateTime minutesLater(int minutes) {
        return LocalDateTime.now().plusMinutes(minutes);
    }
    
    /**
     * 現在日付から指定日数前の日付を生成
     */
    public static LocalDate daysAgo(int days) {
        return LocalDate.now().minusDays(days);
    }
    
    /**
     * 現在日付から指定日数後の日付を生成
     */
    public static LocalDate daysLater(int days) {
        return LocalDate.now().plusDays(days);
    }
    
    /**
     * 2つの日時が指定された範囲内で近いかをチェック
     */
    public static boolean isWithinRange(LocalDateTime actual, LocalDateTime expected, int toleranceMinutes) {
        return Math.abs(java.time.Duration.between(actual, expected).toMinutes()) <= toleranceMinutes;
    }
    
    // ================================================================================
    // 文字列操作ユーティリティ
    // ================================================================================
    
    /**
     * ランダムな文字列を生成
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return result.toString();
    }
    
    /**
     * ランダムなメールアドレスを生成
     */
    public static String randomEmail() {
        return randomString(8) + "@example.com";
    }
    
    /**
     * テスト用の一意なID文字列を生成
     */
    public static String uniqueTestId() {
        return "test_" + System.currentTimeMillis() + "_" + randomString(5);
    }
    
    // ================================================================================
    // 数値操作ユーティリティ
    // ================================================================================
    
    /**
     * 指定範囲内のランダムなBigDecimalを生成
     */
    public static BigDecimal randomBigDecimal(int min, int max) {
        double random = Math.random() * (max - min) + min;
        return BigDecimal.valueOf(Math.round(random * 100.0) / 100.0);
    }
    
    /**
     * 指定桁数で四捨五入
     */
    public static BigDecimal roundToScale(BigDecimal value, int scale) {
        return value.setScale(scale, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * BigDecimalの等価性チェック（スケールを無視）
     */
    public static boolean bigDecimalEquals(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }
    
    // ================================================================================
    // リスト操作ユーティリティ
    // ================================================================================
    
    /**
     * リストから指定インデックスの要素を安全に取得
     */
    public static <T> T safeGet(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }
    
    /**
     * リストの最初の要素を安全に取得
     */
    public static <T> T firstElement(List<T> list) {
        return safeGet(list, 0);
    }
    
    /**
     * リストの最後の要素を安全に取得
     */
    public static <T> T lastElement(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
    }
    
    /**
     * リストが指定サイズかつ全ての要素がnullではないことをチェック
     */
    public static <T> boolean isValidList(List<T> list, int expectedSize) {
        return list != null && 
               list.size() == expectedSize && 
               list.stream().allMatch(item -> item != null);
    }
    
    // ================================================================================
    // 並行処理テストユーティリティ
    // ================================================================================
    
    /**
     * 並行実行テスト用のヘルパー
     */
    public static class ConcurrentTestHelper {
        private final ExecutorService executor;
        private final int threadCount;
        
        public ConcurrentTestHelper(int threadCount) {
            this.threadCount = threadCount;
            this.executor = Executors.newFixedThreadPool(threadCount);
        }
        
        /**
         * 指定された処理を並行実行
         */
        public <T> List<CompletableFuture<T>> runConcurrently(Supplier<T> task) {
            List<CompletableFuture<T>> futures = new java.util.ArrayList<>();
            
            for (int i = 0; i < threadCount; i++) {
                CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executor);
                futures.add(future);
            }
            
            return futures;
        }
        
        /**
         * 全ての並行処理の完了を待機
         */
        public static <T> List<T> waitForAll(List<CompletableFuture<T>> futures) {
            return futures.stream()
                .map(CompletableFuture::join)
                .toList();
        }
        
        /**
         * リソースのクリーンアップ
         */
        public void cleanup() {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }
    
    /**
     * 並行処理でのエラー率を計算
     */
    public static double calculateErrorRate(List<CompletableFuture<Boolean>> results) {
        long totalCount = results.size();
        long errorCount = results.stream()
            .map(CompletableFuture::join)
            .mapToLong(success -> success ? 0 : 1)
            .sum();
        
        return totalCount == 0 ? 0.0 : (double) errorCount / totalCount;
    }
    
    // ================================================================================
    // データベーステスト用ユーティリティ
    // ================================================================================
    
    /**
     * テストデータのクリーンアップ用SQLを生成
     */
    public static String[] getCleanupSql() {
        return new String[]{
            "DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'TEST-%')",
            "DELETE FROM orders WHERE order_number LIKE 'TEST-%'",
            "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@example.com'))",
            "DELETE FROM carts WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@example.com')",
            "DELETE FROM reviews WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@example.com')",
            "DELETE FROM users WHERE email LIKE '%@example.com'",
            "DELETE FROM books WHERE isbn LIKE '9784000%'"
        };
    }
    
    /**
     * テストデータの存在確認用SQL
     */
    public static String getTestDataCountSql(String tableName) {
        return String.format(
            "SELECT COUNT(*) FROM %s WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour'",
            tableName
        );
    }
    
    // ================================================================================
    // パフォーマンス測定ユーティリティ
    // ================================================================================
    
    /**
     * 処理時間を測定して実行
     */
    public static <T> TimedResult<T> measureTime(Supplier<T> task) {
        long startTime = System.currentTimeMillis();
        T result = task.get();
        long endTime = System.currentTimeMillis();
        return new TimedResult<>(result, endTime - startTime);
    }
    
    /**
     * 時間測定結果を保持するクラス
     */
    public static class TimedResult<T> {
        private final T result;
        private final long executionTimeMs;
        
        public TimedResult(T result, long executionTimeMs) {
            this.result = result;
            this.executionTimeMs = executionTimeMs;
        }
        
        public T getResult() { return result; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        
        public boolean isSlowerThan(long thresholdMs) {
            return executionTimeMs > thresholdMs;
        }
        
        @Override
        public String toString() {
            return String.format("Result: %s, ExecutionTime: %dms", result, executionTimeMs);
        }
    }
    
    // ================================================================================
    // ログ出力ユーティリティ
    // ================================================================================
    
    /**
     * テスト開始ログを出力
     */
    public static void logTestStart(String testName) {
        System.out.println("=== TEST START: " + testName + " ===");
    }
    
    /**
     * テスト終了ログを出力
     */
    public static void logTestEnd(String testName) {
        System.out.println("=== TEST END: " + testName + " ===");
    }
    
    /**
     * テスト中の詳細情報を出力
     */
    public static void logTestInfo(String message) {
        System.out.println("[TEST INFO] " + message);
    }
    
    /**
     * テストデータの統計情報を出力
     */
    public static void logDataStatistics(String dataType, int count, long generationTimeMs) {
        System.out.printf("[DATA STATS] %s: %d件 (生成時間: %dms)%n", dataType, count, generationTimeMs);
    }
    
    // ================================================================================
    // バリデーションヘルパー
    // ================================================================================
    
    /**
     * メールアドレスの形式をチェック
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    /**
     * パスワード強度をチェック
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$");
    }
    
    /**
     * ISBN形式をチェック
     */
    public static boolean isValidISBN(String isbn) {
        if (isbn == null) return false;
        return isbn.matches("^978\\d{10}$");
    }
}