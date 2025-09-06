package jp.readscape.consumer.testutils;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.cart.model.Cart;
import jp.readscape.consumer.domain.cart.model.CartItem;
import jp.readscape.consumer.domain.orders.model.Order;
import jp.readscape.consumer.domain.orders.model.OrderItem;
import jp.readscape.consumer.domain.orders.model.OrderStatus;
import jp.readscape.consumer.domain.orders.model.PaymentMethod;
import jp.readscape.consumer.domain.reviews.model.Review;
import jp.readscape.consumer.domain.users.model.Gender;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.dto.auth.LoginRequest;
import jp.readscape.consumer.dto.auth.RegisterRequest;
import jp.readscape.consumer.dto.orders.CreateOrderRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * テストデータ生成ファクトリークラス
 * 一貫性のあるテストデータを効率的に生成するためのユーティリティ
 */
public class TestDataFactory {
    
    private static final Random random = new Random();
    
    // サンプルデータ配列
    private static final String[] BOOK_TITLES = {
        "Spring Bootガイド", "Java入門", "React実践", "データベース設計",
        "アルゴリズム解説", "システム設計", "セキュリティ基礎", "クラウド活用",
        "マイクロサービス", "機械学習入門", "Web API設計", "テスト技法"
    };
    
    private static final String[] AUTHOR_NAMES = {
        "山田太郎", "佐藤花子", "田中一郎", "鈴木美智子",
        "高橋俊介", "伊藤麻衣", "渡辺健太", "中村恵子"
    };
    
    private static final String[] CATEGORIES = {
        "技術書", "ビジネス", "小説", "実用書", 
        "趣味・実用", "学習参考書", "コンピュータ・IT", "経済・経営"
    };
    
    private static final String[] PUBLISHERS = {
        "技術出版", "ビジネス社", "学習社", "実用出版",
        "IT出版", "専門書店", "教育出版", "総合出版"
    };
    
    // ================================================================================
    // User テストデータ生成
    // ================================================================================
    
    /**
     * 基本的なテストユーザーを生成
     */
    public static User createUser() {
        return createUser("test@example.com", "テストユーザー", UserRole.CONSUMER);
    }
    
    /**
     * 指定パラメータでユーザーを生成
     */
    public static User createUser(String email, String name, UserRole role) {
        return User.builder()
            .email(email)
            .password("$2a$10$hashedPassword123")
            .name(name)
            .phoneNumber("090-1234-5678")
            .address("東京都渋谷区1-1-1")
            .birthDate(LocalDate.of(1990, 5, 15))
            .gender(Gender.MALE)
            .role(role)
            .active(true)
            .emailVerified(true)
            .lastLoginAt(LocalDateTime.now().minusDays(1))
            .createdAt(LocalDateTime.now().minusDays(30))
            .updatedAt(LocalDateTime.now().minusDays(1))
            .build();
    }
    
    /**
     * ランダムなユーザーを生成
     */
    public static User createRandomUser() {
        int userIndex = random.nextInt(1000);
        return User.builder()
            .email("user" + userIndex + "@example.com")
            .password("$2a$10$randomPassword" + userIndex)
            .name("ユーザー" + userIndex)
            .phoneNumber("090-" + String.format("%04d", random.nextInt(10000)) + "-" + String.format("%04d", random.nextInt(10000)))
            .address(generateRandomAddress())
            .birthDate(generateRandomBirthDate())
            .gender(random.nextBoolean() ? Gender.MALE : Gender.FEMALE)
            .role(random.nextDouble() < 0.05 ? UserRole.ADMIN : UserRole.CONSUMER) // 5%の確率で管理者
            .active(random.nextDouble() < 0.95) // 95%の確率でアクティブ
            .emailVerified(random.nextDouble() < 0.9) // 90%の確率で認証済み
            .lastLoginAt(generateRandomDateTime(30))
            .createdAt(generateRandomDateTime(365))
            .updatedAt(generateRandomDateTime(30))
            .build();
    }
    
    /**
     * 複数のテストユーザーを生成
     */
    public static List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createRandomUser());
        }
        return users;
    }
    
    // ================================================================================
    // Book テストデータ生成
    // ================================================================================
    
    /**
     * 基本的なテスト書籍を生成
     */
    public static Book createBook() {
        return createBook("テスト書籍", "テスト著者", "9784000000001");
    }
    
    /**
     * 指定パラメータで書籍を生成
     */
    public static Book createBook(String title, String author, String isbn) {
        return Book.builder()
            .title(title)
            .author(author)
            .isbn(isbn)
            .price(new BigDecimal("3000"))
            .category("技術書")
            .description(title + "の詳細な説明です。")
            .publisher("技術出版")
            .pages(400)
            .publicationDate(LocalDate.now().minusMonths(6))
            .stockQuantity(50)
            .inStock(true)
            .active(true)
            .rating(4.5)
            .reviewCount(25)
            .createdAt(LocalDateTime.now().minusDays(180))
            .updatedAt(LocalDateTime.now().minusDays(30))
            .build();
    }
    
    /**
     * ランダムな書籍を生成
     */
    public static Book createRandomBook() {
        int bookIndex = random.nextInt(1000);
        String title = BOOK_TITLES[random.nextInt(BOOK_TITLES.length)];
        String author = AUTHOR_NAMES[random.nextInt(AUTHOR_NAMES.length)];
        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        String publisher = PUBLISHERS[random.nextInt(PUBLISHERS.length)];
        
        return Book.builder()
            .title(title + " " + bookIndex)
            .author(author)
            .isbn("978400000" + String.format("%04d", bookIndex))
            .price(new BigDecimal(1000 + random.nextInt(9000))) // 1000-9999円
            .category(category)
            .description(title + "の詳細な説明。" + generateRandomDescription())
            .publisher(publisher)
            .pages(100 + random.nextInt(900)) // 100-999ページ
            .publicationDate(generateRandomDate(3650)) // 10年間のランダム日付
            .stockQuantity(random.nextInt(100))
            .inStock(random.nextBoolean())
            .active(random.nextDouble() < 0.9) // 90%の確率でアクティブ
            .rating(1.0 + random.nextDouble() * 4.0) // 1.0-5.0
            .reviewCount(random.nextInt(100))
            .createdAt(generateRandomDateTime(365))
            .updatedAt(generateRandomDateTime(90))
            .build();
    }
    
    /**
     * 在庫レベル別書籍を生成
     */
    public static Book createBookWithStockLevel(StockLevel level) {
        Book book = createRandomBook();
        switch (level) {
            case OUT_OF_STOCK:
                book.setStockQuantity(0);
                book.setInStock(false);
                break;
            case LOW_STOCK:
                book.setStockQuantity(1 + random.nextInt(5)); // 1-5
                book.setInStock(true);
                break;
            case MEDIUM_STOCK:
                book.setStockQuantity(6 + random.nextInt(15)); // 6-20
                book.setInStock(true);
                break;
            case HIGH_STOCK:
                book.setStockQuantity(21 + random.nextInt(80)); // 21-100
                book.setInStock(true);
                break;
        }
        return book;
    }
    
    /**
     * 複数の書籍を生成
     */
    public static List<Book> createBooks(int count) {
        List<Book> books = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            books.add(createRandomBook());
        }
        return books;
    }
    
    // ================================================================================
    // Order テストデータ生成
    // ================================================================================
    
    /**
     * 基本的なテスト注文を生成
     */
    public static Order createOrder(Long userId, List<Book> books) {
        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            int quantity = 1 + random.nextInt(3); // 1-3個
            BigDecimal itemTotal = book.getPrice().multiply(new BigDecimal(quantity));
            
            OrderItem item = OrderItem.builder()
                .bookId(book.getId())
                .book(book)
                .quantity(quantity)
                .unitPrice(book.getPrice())
                .totalPrice(itemTotal)
                .build();
            
            items.add(item);
            subtotal = subtotal.add(itemTotal);
        }
        
        BigDecimal shippingFee = new BigDecimal("500");
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.1"));
        BigDecimal totalAmount = subtotal.add(shippingFee).add(tax);
        
        Order order = Order.builder()
            .orderNumber("ORD-" + System.currentTimeMillis() + "-" + random.nextInt(1000))
            .userId(userId)
            .status(OrderStatus.values()[random.nextInt(OrderStatus.values().length)])
            .items(items)
            .subtotal(subtotal)
            .shippingFee(shippingFee)
            .tax(tax)
            .totalAmount(totalAmount)
            .paymentMethod(PaymentMethod.values()[random.nextInt(PaymentMethod.values().length)])
            .shippingAddress(generateRandomAddress())
            .notes("テスト注文 - " + generateRandomNotes())
            .createdAt(generateRandomDateTime(90))
            .updatedAt(generateRandomDateTime(30))
            .build();
        
        // OrderItemにOrderを設定
        items.forEach(item -> item.setOrder(order));
        
        return order;
    }
    
    // ================================================================================
    // Cart テストデータ生成
    // ================================================================================
    
    /**
     * テストカートを生成
     */
    public static Cart createCart(Long userId, List<Book> books) {
        List<CartItem> items = new ArrayList<>();
        
        for (Book book : books) {
            int quantity = 1 + random.nextInt(3); // 1-3個
            CartItem item = CartItem.builder()
                .bookId(book.getId())
                .book(book)
                .quantity(quantity)
                .unitPrice(book.getPrice())
                .addedAt(generateRandomDateTime(7))
                .build();
            items.add(item);
        }
        
        return Cart.builder()
            .userId(userId)
            .active(true)
            .items(items)
            .createdAt(generateRandomDateTime(30))
            .updatedAt(generateRandomDateTime(7))
            .build();
    }
    
    // ================================================================================
    // Review テストデータ生成
    // ================================================================================
    
    /**
     * テストレビューを生成
     */
    public static Review createReview(Long userId, Long bookId) {
        String[] reviewTitles = {
            "とても良い本でした", "参考になりました", "おすすめです",
            "わかりやすい内容", "実践的で役立つ", "初心者にも優しい"
        };
        
        String[] reviewContents = {
            "詳細な解説で理解しやすかったです。実際の業務でも活用できそうです。",
            "基礎から応用まで幅広くカバーされており、とても参考になりました。",
            "具体的な例が多く、実践的な内容でした。続編も期待しています。"
        };
        
        return Review.builder()
            .userId(userId)
            .bookId(bookId)
            .rating(1 + random.nextInt(5)) // 1-5
            .title(reviewTitles[random.nextInt(reviewTitles.length)])
            .content(reviewContents[random.nextInt(reviewContents.length)])
            .helpful(random.nextInt(20))
            .verified(random.nextBoolean())
            .createdAt(generateRandomDateTime(90))
            .updatedAt(generateRandomDateTime(30))
            .build();
    }
    
    // ================================================================================
    // DTO テストデータ生成
    // ================================================================================
    
    /**
     * ログインリクエストDTO生成
     */
    public static LoginRequest createLoginRequest() {
        return createLoginRequest("test@example.com", "password123");
    }
    
    public static LoginRequest createLoginRequest(String email, String password) {
        return LoginRequest.builder()
            .email(email)
            .password(password)
            .build();
    }
    
    /**
     * 登録リクエストDTO生成
     */
    public static RegisterRequest createRegisterRequest() {
        return createRegisterRequest("newuser@example.com", "password123", "新規ユーザー");
    }
    
    public static RegisterRequest createRegisterRequest(String email, String password, String name) {
        return RegisterRequest.builder()
            .email(email)
            .password(password)
            .name(name)
            .build();
    }
    
    /**
     * 注文作成リクエストDTO生成
     */
    public static CreateOrderRequest createOrderRequest() {
        return CreateOrderRequest.builder()
            .paymentMethod("credit_card")
            .shippingAddress("東京都渋谷区1-1-1 テストビル101")
            .notes("テスト注文です")
            .build();
    }
    
    // ================================================================================
    // パフォーマンステスト用大量データ生成
    // ================================================================================
    
    /**
     * パフォーマンステスト用に大量のユーザーを生成
     */
    public static List<User> createBulkUsers(int count) {
        System.out.println("Generating " + count + " users for performance testing...");
        List<User> users = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            if (i % 1000 == 0) {
                System.out.println("Generated " + i + " users...");
            }
            users.add(createRandomUser());
        }
        
        System.out.println("Completed generating " + count + " users.");
        return users;
    }
    
    /**
     * パフォーマンステスト用に大量の書籍を生成
     */
    public static List<Book> createBulkBooks(int count) {
        System.out.println("Generating " + count + " books for performance testing...");
        List<Book> books = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            if (i % 1000 == 0) {
                System.out.println("Generated " + i + " books...");
            }
            books.add(createRandomBook());
        }
        
        System.out.println("Completed generating " + count + " books.");
        return books;
    }
    
    // ================================================================================
    // ヘルパーメソッド
    // ================================================================================
    
    private static String generateRandomAddress() {
        String[] prefectures = {"東京都", "大阪府", "愛知県", "福岡県", "北海道"};
        String[] cities = {"渋谷区", "新宿区", "港区", "中央区", "千代田区"};
        
        return prefectures[random.nextInt(prefectures.length)] + 
               cities[random.nextInt(cities.length)] + 
               (1 + random.nextInt(9)) + "-" + 
               (1 + random.nextInt(9)) + "-" + 
               (1 + random.nextInt(9));
    }
    
    private static LocalDate generateRandomBirthDate() {
        return LocalDate.of(
            1970 + random.nextInt(35), // 1970-2004年
            1 + random.nextInt(12),     // 1-12月
            1 + random.nextInt(28)      // 1-28日
        );
    }
    
    private static LocalDate generateRandomDate(int maxDaysAgo) {
        return LocalDate.now().minusDays(random.nextInt(maxDaysAgo));
    }
    
    private static LocalDateTime generateRandomDateTime(int maxDaysAgo) {
        return LocalDateTime.now().minusDays(random.nextInt(maxDaysAgo))
                                  .minusHours(random.nextInt(24))
                                  .minusMinutes(random.nextInt(60));
    }
    
    private static String generateRandomDescription() {
        String[] descriptions = {
            "実践的な内容を含む優れた一冊です。",
            "初心者から上級者まで幅広く対応しています。",
            "豊富なサンプルコードで理解が深まります。",
            "現場で使える実用的な知識が満載です。"
        };
        return descriptions[random.nextInt(descriptions.length)];
    }
    
    private static String generateRandomNotes() {
        String[] notes = {
            "お急ぎでお願いします", "丁寧な梱包をお願いします", 
            "不在の場合は再配達をお願いします", "ギフト包装をお願いします"
        };
        return notes[random.nextInt(notes.length)];
    }
    
    // ================================================================================
    // 列挙型
    // ================================================================================
    
    public enum StockLevel {
        OUT_OF_STOCK, LOW_STOCK, MEDIUM_STOCK, HIGH_STOCK
    }
    
    // ================================================================================
    // ビルダーパターンでのテストシナリオ生成
    // ================================================================================
    
    /**
     * 複雑なテストシナリオを構築するためのビルダークラス
     */
    public static class ScenarioBuilder {
        private int userCount = 1;
        private int bookCount = 1;
        private int orderCount = 0;
        private int reviewCount = 0;
        private UserRole userRole = UserRole.CONSUMER;
        private StockLevel stockLevel = StockLevel.MEDIUM_STOCK;
        
        public ScenarioBuilder users(int count) {
            this.userCount = count;
            return this;
        }
        
        public ScenarioBuilder books(int count) {
            this.bookCount = count;
            return this;
        }
        
        public ScenarioBuilder orders(int count) {
            this.orderCount = count;
            return this;
        }
        
        public ScenarioBuilder reviews(int count) {
            this.reviewCount = count;
            return this;
        }
        
        public ScenarioBuilder userRole(UserRole role) {
            this.userRole = role;
            return this;
        }
        
        public ScenarioBuilder stockLevel(StockLevel level) {
            this.stockLevel = level;
            return this;
        }
        
        public TestScenario build() {
            return new TestScenario(userCount, bookCount, orderCount, reviewCount, userRole, stockLevel);
        }
    }
    
    public static ScenarioBuilder scenario() {
        return new ScenarioBuilder();
    }
    
    /**
     * テストシナリオデータを保持するクラス
     */
    public static class TestScenario {
        public final List<User> users;
        public final List<Book> books;
        public final List<Order> orders;
        public final List<Review> reviews;
        
        private TestScenario(int userCount, int bookCount, int orderCount, int reviewCount, 
                           UserRole userRole, StockLevel stockLevel) {
            // ユーザー生成
            this.users = new ArrayList<>();
            for (int i = 0; i < userCount; i++) {
                User user = createRandomUser();
                user.setRole(userRole);
                this.users.add(user);
            }
            
            // 書籍生成
            this.books = new ArrayList<>();
            for (int i = 0; i < bookCount; i++) {
                this.books.add(createBookWithStockLevel(stockLevel));
            }
            
            // 注文生成
            this.orders = new ArrayList<>();
            for (int i = 0; i < orderCount && !users.isEmpty() && !books.isEmpty(); i++) {
                User user = users.get(i % users.size());
                List<Book> orderBooks = Arrays.asList(books.get(i % books.size()));
                this.orders.add(createOrder(user.getId(), orderBooks));
            }
            
            // レビュー生成
            this.reviews = new ArrayList<>();
            for (int i = 0; i < reviewCount && !users.isEmpty() && !books.isEmpty(); i++) {
                User user = users.get(i % users.size());
                Book book = books.get(i % books.size());
                this.reviews.add(createReview(user.getId(), book.getId()));
            }
        }
    }
}