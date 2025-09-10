package jp.readscape.consumer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.dto.carts.AddToCartRequest;
import jp.readscape.consumer.dto.orders.CreateOrderRequest;
import jp.readscape.consumer.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("カート→注文処理 E2Eテスト")
class CartToOrderE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        createTestData();
    }

    @Test
    @DisplayName("完全なE2Eフロー: カート追加→注文→在庫減少確認")
    void completeE2EFlow_Success() throws Exception {
        // Step 1: 初期在庫確認
        int initialStock = testBook.getStockQuantity();
        
        mockMvc.perform(get("/books/{id}", testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(initialStock));

        // Step 2: カートに商品追加
        AddToCartRequest addToCartRequest = AddToCartRequest.builder()
                .bookId(testBook.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 3: カート内容確認
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").exists());

        // Step 4: 注文作成
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .shippingAddress("東京都渋谷区テスト1-1-1")
                .shippingPhone("090-1234-5678")
                .paymentMethod("CREDIT_CARD")
                .notes("テスト注文")
                .build();

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.totalAmount").exists())
                .andReturn();

        // 注文IDを取得
        String orderResponse = orderResult.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderResponse).get("orderId").asLong();

        // Step 5: 注文詳細確認
        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        // Step 6: 在庫減少確認 (注文処理後)
        mockMvc.perform(get("/books/{id}", testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(initialStock - 2));

        // Step 7: カートがクリアされていることを確認
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("在庫不足時の注文エラーテスト")
    void orderWithInsufficientStock_ShouldFail() throws Exception {
        // カートに在庫以上の数量を追加しようとする
        AddToCartRequest addToCartRequest = AddToCartRequest.builder()
                .bookId(testBook.getId())
                .quantity(testBook.getStockQuantity() + 10) // 在庫超過
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("在庫")));
    }

    @Test
    @DisplayName("未認証ユーザーによるカート操作エラー")
    void cartOperationsWithoutAuth_ShouldFail() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(testBook.getId())
                .quantity(1)
                .build();

        // Authorization ヘッダーなしでリクエスト
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("存在しない書籍のカート追加エラー")
    void addNonExistentBookToCart_ShouldFail() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(999999L) // 存在しないID
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("カート数量更新テスト")
    void updateCartQuantity_Success() throws Exception {
        // まずカートに追加
        AddToCartRequest addRequest = AddToCartRequest.builder()
                .bookId(testBook.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        // 数量更新
        UpdateCartQuantityRequest updateRequest = UpdateCartQuantityRequest.builder()
                .quantity(3)
                .build();

        mockMvc.perform(put("/api/cart/items/{bookId}", testBook.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // 更新確認
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    private void createTestData() {
        // テストユーザー作成
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setFullName("テストユーザー");
        testUser.setRole(UserRole.CONSUMER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(testUser);

        // テスト書籍作成
        testBook = new Book();
        testBook.setTitle("テスト書籍");
        testBook.setAuthor("テスト著者");
        testBook.setIsbn("9784000000999");
        testBook.setPrice(2500);
        testBook.setDescription("E2Eテスト用の書籍");
        testBook.setCategory("テストカテゴリ");
        testBook.setStockQuantity(20);
        testBook.setAverageRating(BigDecimal.valueOf(4.0));
        testBook.setReviewCount(5);
        testBook.setCreatedAt(LocalDateTime.now());
        testBook.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(testBook);

        // JWT トークン生成
        authToken = jwtService.generateToken(testUser);
    }

    // 内部クラス（DTO不足分）
    @lombok.Builder
    @lombok.Data
    static class UpdateCartQuantityRequest {
        private Integer quantity;
    }
}