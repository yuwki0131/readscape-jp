package jp.readscape.consumer.testutils;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.orders.model.Order;
import jp.readscape.consumer.domain.orders.model.OrderStatus;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 共通テストシナリオを定義するクラス
 * よく使われるテストパターンを事前定義して、テストコードの再利用性を向上
 */
public class TestScenarios {
    
    // ================================================================================
    // 基本的なテストシナリオ
    // ================================================================================
    
    /**
     * 新規ユーザー登録シナリオ
     * - 新しいメールアドレスでユーザー登録
     * - メール認証完了
     * - プロフィール情報入力
     */
    public static class NewUserRegistrationScenario {
        public final User newUser;
        public final String plainPassword;
        
        public NewUserRegistrationScenario() {
            this.plainPassword = "newPassword123";
            this.newUser = TestDataFactory.createUser(
                "newuser" + System.currentTimeMillis() + "@example.com",
                "新規登録ユーザー",
                UserRole.CONSUMER
            );
            this.newUser.setEmailVerified(false); // 初期状態では未認証
            this.newUser.setActive(true);
        }
        
        /**
         * メール認証完了後の状態に変更
         */
        public NewUserRegistrationScenario verifyEmail() {
            this.newUser.setEmailVerified(true);
            return this;
        }
        
        /**
         * プロフィール情報を完成させる
         */
        public NewUserRegistrationScenario completeProfile() {
            this.newUser.setPhoneNumber("090-9999-8888");
            this.newUser.setAddress("東京都新宿区1-1-1");
            return this;
        }
    }
    
    /**
     * ユーザーログインシナリオ
     * - 既存ユーザーでのログイン
     * - ログイン履歴の更新
     * - セッション管理
     */
    public static class UserLoginScenario {
        public final User existingUser;
        public final String plainPassword;
        public final LocalDateTime lastLoginBefore;
        
        public UserLoginScenario() {
            this.plainPassword = "existingPassword123";
            this.existingUser = TestDataFactory.createUser(
                "existing" + System.currentTimeMillis() + "@example.com",
                "既存ユーザー",
                UserRole.CONSUMER
            );
            this.lastLoginBefore = this.existingUser.getLastLoginAt();
        }
        
        /**
         * ログイン成功時の状態更新
         */
        public UserLoginScenario loginSuccess() {
            this.existingUser.setLastLoginAt(LocalDateTime.now());
            return this;
        }
    }
    
    /**
     * 書籍閲覧・検索シナリオ
     * - カテゴリ別書籍表示
     * - キーワード検索
     * - 並び替え・フィルタリング
     */
    public static class BookBrowsingScenario {
        public final List<Book> techBooks;
        public final List<Book> businessBooks;
        public final List<Book> outOfStockBooks;
        public final List<Book> newReleases;
        public final String searchKeyword;
        
        public BookBrowsingScenario() {
            // カテゴリ別書籍生成
            this.techBooks = List.of(
                TestDataFactory.createBook("Spring Boot完全ガイド", "技術太郎", "9784001000001"),
                TestDataFactory.createBook("Java設計パターン", "技術花子", "9784001000002"),
                TestDataFactory.createBook("データベース設計入門", "DB専門家", "9784001000003")
            );
            
            this.businessBooks = List.of(
                TestDataFactory.createBook("ビジネス戦略論", "経営者A", "9784002000001"),
                TestDataFactory.createBook("マーケティング実践", "マーケター", "9784002000002")
            );
            
            // 在庫切れ書籍
            this.outOfStockBooks = List.of(
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.OUT_OF_STOCK),
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.OUT_OF_STOCK)
            );
            
            // 新刊書籍
            this.newReleases = List.of(
                TestDataFactory.createBook("最新技術トレンド", "トレンド解説者", "9784003000001"),
                TestDataFactory.createBook("2024年版ベストプラクティス", "実践家", "9784003000002")
            );
            newReleases.forEach(book -> book.setCreatedAt(LocalDateTime.now().minusDays(7)));
            
            this.searchKeyword = "Spring";
        }
        
        /**
         * 検索対象書籍一覧
         */
        public List<Book> getAllBooks() {
            return List.of(
                techBooks, businessBooks, outOfStockBooks, newReleases
            ).stream().flatMap(List::stream).toList();
        }
        
        /**
         * 在庫ありの書籍のみ
         */
        public List<Book> getInStockBooks() {
            return getAllBooks().stream()
                .filter(Book::isInStock)
                .toList();
        }
    }
    
    /**
     * ショッピングカート操作シナリオ
     * - 商品追加
     * - 数量変更
     * - 商品削除
     * - 在庫チェック
     */
    public static class ShoppingCartScenario {
        public final User customer;
        public final List<Book> availableBooks;
        public final Book lowStockBook;
        public final Book outOfStockBook;
        
        public ShoppingCartScenario() {
            this.customer = TestDataFactory.createUser(
                "cart" + System.currentTimeMillis() + "@example.com",
                "カートテストユーザー",
                UserRole.CONSUMER
            );
            
            this.availableBooks = List.of(
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.HIGH_STOCK),
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.MEDIUM_STOCK),
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.MEDIUM_STOCK)
            );
            
            this.lowStockBook = TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.LOW_STOCK);
            this.outOfStockBook = TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.OUT_OF_STOCK);
        }
        
        /**
         * 通常の購入可能数量を取得
         */
        public int getNormalQuantity() {
            return 2;
        }
        
        /**
         * 在庫を超える数量を取得
         */
        public int getExcessiveQuantity() {
            return 999;
        }
    }
    
    /**
     * 注文処理シナリオ
     * - カートから注文作成
     * - 支払い処理
     * - 在庫減算
     * - 注文確認
     */
    public static class OrderProcessingScenario {
        public final User customer;
        public final List<Book> orderBooks;
        public final Order pendingOrder;
        public final Order confirmedOrder;
        public final Order shippedOrder;
        
        public OrderProcessingScenario() {
            this.customer = TestDataFactory.createUser(
                "order" + System.currentTimeMillis() + "@example.com",
                "注文テストユーザー",
                UserRole.CONSUMER
            );
            
            this.orderBooks = List.of(
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.HIGH_STOCK),
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.MEDIUM_STOCK)
            );
            
            // 各状態の注文を生成
            this.pendingOrder = TestDataFactory.createOrder(customer.getId(), orderBooks);
            this.pendingOrder.setStatus(OrderStatus.PENDING);
            
            this.confirmedOrder = TestDataFactory.createOrder(customer.getId(), orderBooks);
            this.confirmedOrder.setStatus(OrderStatus.CONFIRMED);
            
            this.shippedOrder = TestDataFactory.createOrder(customer.getId(), orderBooks);
            this.shippedOrder.setStatus(OrderStatus.SHIPPED);
        }
        
        /**
         * 注文ステータス遷移パターン
         */
        public List<OrderStatus> getStatusTransitionFlow() {
            return List.of(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED
            );
        }
        
        /**
         * 無効なステータス遷移パターン
         */
        public List<OrderStatus[]> getInvalidStatusTransitions() {
            return List.of(
                new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.PENDING},
                new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.SHIPPED},
                new OrderStatus[]{OrderStatus.SHIPPED, OrderStatus.PENDING}
            );
        }
    }
    
    /**
     * レビュー投稿シナリオ
     * - 購入済み商品へのレビュー
     * - 評価・コメント投稿
     * - レビューの編集・削除
     */
    public static class ReviewScenario {
        public final User reviewer;
        public final Book reviewedBook;
        public final Order purchaseOrder; // 購入実績
        
        public ReviewScenario() {
            this.reviewer = TestDataFactory.createUser(
                "reviewer" + System.currentTimeMillis() + "@example.com",
                "レビュワー",
                UserRole.CONSUMER
            );
            
            this.reviewedBook = TestDataFactory.createBook(
                "レビューテスト書籍",
                "レビュー著者",
                "9784999000001"
            );
            
            // 購入実績を作成（レビュー投稿の前提条件）
            this.purchaseOrder = TestDataFactory.createOrder(reviewer.getId(), List.of(reviewedBook));
            this.purchaseOrder.setStatus(OrderStatus.DELIVERED);
        }
        
        /**
         * 良いレビューデータ
         */
        public String[] getPositiveReviews() {
            return new String[]{
                "とても参考になる内容でした",
                "実践的で役に立つ情報が満載",
                "初心者にもわかりやすく説明されている"
            };
        }
        
        /**
         * 否定的なレビューデータ
         */
        public String[] getNegativeReviews() {
            return new String[]{
                "期待していた内容と異なっていた",
                "説明が不十分で理解しづらい",
                "古い情報が多く実用性に欠ける"
            };
        }
    }
    
    // ================================================================================
    // 管理者向けシナリオ
    // ================================================================================
    
    /**
     * 在庫管理シナリオ
     * - 在庫確認
     * - 在庫更新
     * - 低在庫アラート
     */
    public static class InventoryManagementScenario {
        public final User admin;
        public final List<Book> normalStockBooks;
        public final List<Book> lowStockBooks;
        public final List<Book> outOfStockBooks;
        
        public InventoryManagementScenario() {
            this.admin = TestDataFactory.createUser(
                "admin" + System.currentTimeMillis() + "@example.com",
                "在庫管理者",
                UserRole.ADMIN
            );
            
            this.normalStockBooks = List.of(
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.HIGH_STOCK),
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.MEDIUM_STOCK)
            );
            
            this.lowStockBooks = List.of(
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.LOW_STOCK),
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.LOW_STOCK)
            );
            
            this.outOfStockBooks = List.of(
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.OUT_OF_STOCK),
                TestDataFactory.createBookWithStockLevel(TestDataFactory.StockLevel.OUT_OF_STOCK)
            );
        }
        
        /**
         * 在庫補充の推奨数量を計算
         */
        public int getRecommendedRestockQuantity(Book book) {
            if (book.getStockQuantity() == 0) {
                return 50; // 完全に在庫切れの場合
            } else if (book.getStockQuantity() < 10) {
                return 30; // 低在庫の場合
            }
            return 0; // 補充不要
        }
    }
    
    /**
     * 注文管理シナリオ（管理者向け）
     * - 注文一覧確認
     * - ステータス更新
     * - 配送手配
     */
    public static class AdminOrderManagementScenario {
        public final User admin;
        public final List<Order> pendingOrders;
        public final List<Order> confirmedOrders;
        public final List<Order> shippedOrders;
        
        public AdminOrderManagementScenario() {
            this.admin = TestDataFactory.createUser(
                "orderadmin" + System.currentTimeMillis() + "@example.com",
                "注文管理者",
                UserRole.ADMIN
            );
            
            // 各ステータスの注文を複数生成
            this.pendingOrders = generateOrdersWithStatus(OrderStatus.PENDING, 5);
            this.confirmedOrders = generateOrdersWithStatus(OrderStatus.CONFIRMED, 3);
            this.shippedOrders = generateOrdersWithStatus(OrderStatus.SHIPPED, 2);
        }
        
        private List<Order> generateOrdersWithStatus(OrderStatus status, int count) {
            List<Order> orders = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                User customer = TestDataFactory.createRandomUser();
                List<Book> books = TestDataFactory.createBooks(2);
                Order order = TestDataFactory.createOrder(customer.getId(), books);
                order.setStatus(status);
                orders.add(order);
            }
            return orders;
        }
        
        /**
         * 今日処理すべき注文を取得
         */
        public List<Order> getTodaysOrders() {
            return List.of(pendingOrders, confirmedOrders).stream()
                .flatMap(List::stream)
                .filter(order -> order.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1)))
                .toList();
        }
    }
    
    // ================================================================================
    // パフォーマンステスト用シナリオ
    // ================================================================================
    
    /**
     * 大量データでのパフォーマンステストシナリオ
     */
    public static class PerformanceTestScenario {
        public final int userCount;
        public final int bookCount;
        public final int orderCount;
        
        public PerformanceTestScenario(int userCount, int bookCount, int orderCount) {
            this.userCount = userCount;
            this.bookCount = bookCount;
            this.orderCount = orderCount;
        }
        
        /**
         * 小規模テスト用
         */
        public static PerformanceTestScenario small() {
            return new PerformanceTestScenario(100, 500, 1000);
        }
        
        /**
         * 中規模テスト用
         */
        public static PerformanceTestScenario medium() {
            return new PerformanceTestScenario(1000, 5000, 10000);
        }
        
        /**
         * 大規模テスト用
         */
        public static PerformanceTestScenario large() {
            return new PerformanceTestScenario(10000, 50000, 100000);
        }
        
        /**
         * パフォーマンステストデータを生成
         */
        public void generateTestData() {
            System.out.println("Generating performance test data...");
            System.out.println("Users: " + userCount + ", Books: " + bookCount + ", Orders: " + orderCount);
            
            long startTime = System.currentTimeMillis();
            
            // 実際の生成処理（メモリ使用量に注意）
            TestDataFactory.createBulkUsers(userCount);
            TestDataFactory.createBulkBooks(bookCount);
            
            long endTime = System.currentTimeMillis();
            System.out.println("Performance test data generation completed in " + 
                             (endTime - startTime) + "ms");
        }
    }
    
    // ================================================================================
    // エラーケーステストシナリオ
    // ================================================================================
    
    /**
     * エラーケース用のテストシナリオ
     */
    public static class ErrorCaseScenario {
        
        /**
         * 無効なデータでのテストケース
         */
        public static class InvalidDataScenario {
            public final String[] invalidEmails = {
                "invalid-email",
                "@example.com",
                "test@",
                "test..double.dot@example.com",
                ""
            };
            
            public final String[] invalidPasswords = {
                "", "123", "short", null
            };
            
            public final String[] invalidISBNs = {
                "123456789", // 短すぎる
                "97840000000001", // 長すぎる
                "978-invalid-isbn", // 無効な文字
                ""
            };
        }
        
        /**
         * 権限エラーのテストケース
         */
        public static class UnauthorizedAccessScenario {
            public final User normalUser;
            public final User adminUser;
            public final String[] adminOnlyEndpoints = {
                "/api/admin/books",
                "/api/admin/users",
                "/api/admin/orders",
                "/api/admin/inventory"
            };
            
            public UnauthorizedAccessScenario() {
                this.normalUser = TestDataFactory.createUser(
                    "normal" + System.currentTimeMillis() + "@example.com",
                    "一般ユーザー",
                    UserRole.CONSUMER
                );
                
                this.adminUser = TestDataFactory.createUser(
                    "admin" + System.currentTimeMillis() + "@example.com",
                    "管理者ユーザー",
                    UserRole.ADMIN
                );
            }
        }
    }
}