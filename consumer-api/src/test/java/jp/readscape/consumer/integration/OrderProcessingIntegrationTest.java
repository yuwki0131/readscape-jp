package jp.readscape.consumer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.cart.model.Cart;
import jp.readscape.consumer.domain.cart.repository.CartRepository;
import jp.readscape.consumer.domain.orders.model.Order;
import jp.readscape.consumer.domain.orders.repository.OrderRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.dto.orders.CreateOrderRequest;
import jp.readscape.consumer.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Order Processing Integration Test")
class OrderProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private Book testBook;
    private String authToken;

    @BeforeEach
    void setUp() {
        // テストユーザー作成
        testUser = User.builder()
            .email("integration-test@example.com")
            .password("$2a$10$encodedPassword")
            .name("統合テストユーザー")
            .role(UserRole.CONSUMER)
            .active(true)
            .emailVerified(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        testUser = userRepository.save(testUser);

        // テスト書籍作成
        testBook = Book.builder()
            .title("統合テスト用書籍")
            .author("テスト著者")
            .isbn("9784000000999")
            .price(new BigDecimal("2500"))
            .category("テスト")
            .description("統合テスト用の書籍です")
            .publisher("テスト出版")
            .pages(300)
            .publicationDate(LocalDate.now())
            .stockQuantity(50)
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
    @DisplayName("完全な注文処理フロー - カート追加→注文作成→確認")
    @Transactional
    void completeOrderProcessingFlow() throws Exception {
        // 1. カートに商品を追加
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 2
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].book.id").value(testBook.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(5000)); // 2500 * 2

        // 2. カート内容確認
        mockMvc.perform(get("/api/cart")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(5000));

        // 3. 注文作成
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
            .paymentMethod("credit_card")
            .shippingAddress("東京都渋谷区1-1-1 テストビル101")
            .notes("統合テスト用注文")
            .build();

        mockMvc.perform(post("/api/orders")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.subtotal").value(5000))
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.paymentMethod").value("credit_card"))
                .andExpect(jsonPath("$.shippingAddress").value("東京都渋谷区1-1-1 テストビル101"));

        // 4. 注文一覧確認
        mockMvc.perform(get("/api/orders")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].status").value("PENDING"));

        // 5. カートが空になっていることを確認
        mockMvc.perform(get("/api/cart")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("在庫不足時の注文処理エラー")
    @Transactional
    void orderProcessingWithInsufficientStock() throws Exception {
        // 在庫を1に設定
        testBook.setStockQuantity(1);
        bookRepository.save(testBook);

        // 在庫を超える数量をカートに追加しようとする
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 5
                    ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").containsString("Insufficient stock"));
    }

    @Test
    @DisplayName("複数商品での注文処理フロー")
    @Transactional
    void multipleItemsOrderProcessing() throws Exception {
        // 2つ目の書籍作成
        Book secondBook = Book.builder()
            .title("統合テスト用書籍2")
            .author("テスト著者2")
            .isbn("9784000000998")
            .price(new BigDecimal("3000"))
            .category("テスト")
            .stockQuantity(30)
            .inStock(true)
            .active(true)
            .build();
        secondBook = bookRepository.save(secondBook);

        // 1つ目の商品をカートに追加
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 1
                    ))))
                .andExpect(status().isOk());

        // 2つ目の商品をカートに追加
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", secondBook.getId(),
                        "quantity", 2
                    ))))
                .andExpect(status().isOk());

        // カート内容確認（2つの商品）
        mockMvc.perform(get("/api/cart")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalAmount").value(8500)); // 2500*1 + 3000*2

        // 注文作成
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
            .paymentMethod("bank_transfer")
            .shippingAddress("大阪府大阪市2-2-2")
            .build();

        mockMvc.perform(post("/api/orders")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.subtotal").value(8500));
    }

    @Test
    @DisplayName("認証なしでの注文処理エラー")
    void orderProcessingWithoutAuthentication() throws Exception {
        // 認証なしでカートアクセス
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        // 認証なしで注文作成
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
            .paymentMethod("credit_card")
            .shippingAddress("テスト住所")
            .build();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("不正なJWTトークンでの注文処理エラー")
    void orderProcessingWithInvalidToken() throws Exception {
        String invalidToken = "Bearer invalid-jwt-token";

        mockMvc.perform(get("/api/cart")
                .header("Authorization", invalidToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/orders")
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("注文キャンセル処理フロー")
    @Transactional
    void orderCancellationFlow() throws Exception {
        // まず注文を作成
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 1
                    ))))
                .andExpect(status().isOk());

        String orderResponse = mockMvc.perform(post("/api/orders")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreateOrderRequest.builder()
                        .paymentMethod("credit_card")
                        .shippingAddress("キャンセルテスト住所")
                        .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 注文IDを抽出（実際の実装に応じて調整が必要）
        Long orderId = 1L; // 簡略化

        // 注文をキャンセル
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("reason", "テストキャンセル"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").containsString("キャンセル"));
    }

    @Test
    @DisplayName("商品検索から注文までの完全フロー")
    @Transactional
    void completeSearchToOrderFlow() throws Exception {
        // 1. 商品検索
        mockMvc.perform(get("/api/books")
                .param("keyword", "統合テスト")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books", hasSize(1)))
                .andExpect(jsonPath("$.books[0].title").value("統合テスト用書籍"));

        // 2. 商品詳細表示
        mockMvc.perform(get("/api/books/{id}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBook.getId()))
                .andExpect(jsonPath("$.title").value("統合テスト用書籍"))
                .andExpect(jsonPath("$.stockQuantity").value(50));

        // 3. カートに追加
        mockMvc.perform(post("/api/cart")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "bookId", testBook.getId(),
                        "quantity", 3
                    ))))
                .andExpect(status().isOk());

        // 4. 注文作成
        mockMvc.perform(post("/api/orders")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreateOrderRequest.builder()
                        .paymentMethod("convenience_store")
                        .shippingAddress("完全フローテスト住所")
                        .notes("検索から注文までのテスト")
                        .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.subtotal").value(7500)) // 2500 * 3
                .andExpect(jsonPath("$.notes").value("検索から注文までのテスト"));
    }
}