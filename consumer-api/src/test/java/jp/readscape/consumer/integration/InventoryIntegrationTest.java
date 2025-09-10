package jp.readscape.consumer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Inventory Integration Test")
class InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private RestTemplate restTemplate;

    private User testUser;
    private Book testBook;
    private String authToken;

    @BeforeEach
    void setUp() {
        // テストユーザー作成
        testUser = User.builder()
            .email("inventory-test@example.com")
            .password("$2a$10$encodedPassword")
            .name("在庫統合テストユーザー")
            .role(UserRole.CONSUMER)
            .active(true)
            .emailVerified(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        testUser = userRepository.save(testUser);

        // テスト書籍作成
        testBook = Book.builder()
            .title("在庫テスト用書籍")
            .author("在庫テスト著者")
            .isbn("9784000001000")
            .price(new BigDecimal("3000"))
            .category("在庫テスト")
            .description("在庫統合テスト用の書籍です")
            .publisher("在庫テスト出版")
            .pages(400)
            .publicationDate(LocalDate.now())
            .stockQuantity(10)
            .inStock(true)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        testBook = bookRepository.save(testBook);

        // JWT トークン生成
        authToken = "Bearer " + jwtService.generateToken(testUser.getEmail());
    }

    @Test
    @DisplayName("在庫確認による商品表示制御")
    @Transactional
    void stockAvailabilityDisplayControl() throws Exception {
        // 在庫ありの商品検索
        mockMvc.perform(get("/api/books")
                .param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].inStock").value(true))
                .andExpect(jsonPath("$.books[0].stockQuantity").value(10));

        // 在庫を0に設定
        testBook.setStockQuantity(0);
        testBook.setInStock(false);
        bookRepository.save(testBook);

        // 在庫切れ商品は在庫ありフィルターで除外される
        mockMvc.perform(get("/api/books")
                .param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books", hasSize(0)));

        // 在庫切れでも全体検索では表示される
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books", hasSize(1)))
                .andExpect(jsonPath("$.books[0].inStock").value(false))
                .andExpect(jsonPath("$.books[0].stockQuantity").value(0));
    }

    @Test
    @DisplayName("在庫不足時のカート追加制限")
    @Transactional
    void cartAdditionWithInsufficientStock() throws Exception {
        // 在庫数以下の追加は成功
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 5
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));

        // 在庫を超える追加は失敗
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 10
                    ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));

        // カート内既存数量 + 新規追加数量が在庫を超える場合
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 8
                    ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("注文確定時の在庫減算処理")
    @Transactional
    void stockDeductionOnOrderConfirmation() throws Exception {
        // 初期在庫確認
        assertThat(testBook.getStockQuantity()).isEqualTo(10);

        // カートに商品追加
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 3
                    ))))
                .andExpect(status().isOk());

        // 注文作成（在庫減算が発生）
        mockMvc.perform(post("/api/orders")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "paymentMethod", "credit_card",
                        "shippingAddress", "在庫テスト住所"
                    ))))
                .andExpect(status().isCreated());

        // 在庫が減算されていることを確認
        Book updatedBook = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(updatedBook.getStockQuantity()).isEqualTo(7); // 10 - 3 = 7
    }

    @Test
    @DisplayName("同時アクセス時の在庫整合性テスト")
    @Transactional
    void concurrentStockConsistencyTest() throws Exception {
        final int threadCount = 5;
        final int quantityPerThread = 2;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        // 複数スレッドで同時にカート追加を実行
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    // 各スレッド用のユーザー作成
                    User threadUser = User.builder()
                        .email("concurrent-user-" + threadIndex + "@example.com")
                        .password("password")
                        .name("並行ユーザー" + threadIndex)
                        .role(UserRole.CONSUMER)
                        .active(true)
                        .build();
                    threadUser = userRepository.save(threadUser);
                    
                    String threadToken = "Bearer " + jwtService.generateToken(threadUser.getEmail());

                    // カート追加実行
                    mockMvc.perform(post("/api/cart")
                            .header("Authorization", threadToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                    "bookId", testBook.getId(),
                                    "quantity", quantityPerThread
                                ))))
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                            });

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 在庫整合性確認
        // 初期在庫10 - 成功した注文の合計数量 = 残在庫
        Book finalBook = bookRepository.findById(testBook.getId()).orElseThrow();
        int expectedRemainingStock = 10 - (successCount.get() * quantityPerThread);
        
        // 在庫不足により一部の注文は失敗するはず
        assertThat(successCount.get()).isLessThanOrEqualTo(5); // 10在庫 ÷ 2数量 = 最大5回成功
        assertThat(failureCount.get()).isGreaterThan(0);
        assertThat(finalBook.getStockQuantity()).isEqualTo(expectedRemainingStock);
    }

    @Test
    @DisplayName("在庫復旧処理（注文キャンセル時）")
    @Transactional
    void stockRestoreOnOrderCancellation() throws Exception {
        // 初期在庫確認
        assertThat(testBook.getStockQuantity()).isEqualTo(10);

        // 注文作成で在庫減算
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 4
                    ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "paymentMethod", "credit_card",
                        "shippingAddress", "キャンセルテスト住所"
                    ))))
                .andExpect(status().isCreated());

        // 在庫減算確認
        Book afterOrder = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(afterOrder.getStockQuantity()).isEqualTo(6); // 10 - 4 = 6

        // 注文キャンセル（在庫復旧）
        Long orderId = 1L; // 簡略化
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("reason", "在庫復旧テスト"))))
                .andExpect(status().isOk());

        // 在庫復旧確認
        Book afterCancel = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(afterCancel.getStockQuantity()).isEqualTo(10); // 6 + 4 = 10 (復旧)
    }

    @Test
    @DisplayName("低在庫アラート機能テスト")
    @Transactional
    void lowStockAlertTest() throws Exception {
        // 在庫を低在庫レベル（例：5未満）に設定
        testBook.setStockQuantity(3);
        testBook.setMinStockLevel(5);
        bookRepository.save(testBook);

        // 低在庫商品として検出されることを確認
        mockMvc.perform(get("/api/books/{id}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(3))
                .andExpect(jsonPath("$.isLowStock").value(true));

        // 管理者向け低在庫アラート API（将来的な実装想定）
        // 実際の実装があれば以下のようなテストを追加
        /*
        mockMvc.perform(get("/api/admin/inventory/low-stock")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].bookId").value(testBook.getId()));
        */
    }

    @Test
    @DisplayName("在庫予約機能（カート保持）テスト")
    @Transactional
    void stockReservationTest() throws Exception {
        // カートに商品を追加（一時的な在庫予約）
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 5
                    ))))
                .andExpect(status().isOk());

        // 他のユーザーが同じ商品を在庫を超える数量で追加しようとする
        User anotherUser = User.builder()
            .email("another-user@example.com")
            .password("password")
            .name("別ユーザー")
            .role(UserRole.CONSUMER)
            .active(true)
            .build();
        anotherUser = userRepository.save(anotherUser);
        String anotherToken = "Bearer " + jwtService.generateToken(anotherUser.getEmail());

        // 残り在庫5個に対して6個は追加できない
        mockMvc.perform(post("/api/cart")
                .header("Authorization", anotherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 6
                    ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));

        // 残り在庫内（5個以下）なら追加可能
        mockMvc.perform(post("/api/cart")
                .header("Authorization", anotherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 3
                    ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("在庫データ整合性チェック")
    @Transactional
    void stockDataIntegrityCheck() throws Exception {
        // 負の在庫は設定できないことを確認
        testBook.setStockQuantity(-1);
        
        assertThatThrownBy(() -> bookRepository.save(testBook))
            .isInstanceOf(Exception.class); // 制約違反による例外

        // 在庫と inStock フラグの整合性チェック
        testBook.setStockQuantity(0);
        testBook.setInStock(true); // 不整合な状態
        
        // 保存時に自動的に整合性が取られることを確認
        Book saved = bookRepository.save(testBook);
        assertThat(saved.isInStock()).isFalse(); // 在庫0なら自動的にfalse
    }
}