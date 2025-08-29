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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
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
@DisplayName("書籍API統合テスト")
class BookApiIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // テストデータ準備
        createTestData();
    }

    @Test
    @DisplayName("書籍一覧取得 - 認証なし")
    void getBooks_WithoutAuth_Success() throws Exception {
        mockMvc.perform(get("/books")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.currentPage").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    @Test
    @DisplayName("書籍一覧取得 - ページネーション")
    void getBooks_WithPagination_Success() throws Exception {
        mockMvc.perform(get("/books")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.hasNext").exists())
                .andExpect(jsonPath("$.hasPrevious").exists());
    }

    @Test
    @DisplayName("書籍詳細取得 - 成功")
    void getBookById_Success() throws Exception {
        // Given
        Book book = bookRepository.findAll().get(0);

        // When & Then
        mockMvc.perform(get("/books/{id}", book.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()))
                .andExpect(jsonPath("$.title").value(book.getTitle()))
                .andExpect(jsonPath("$.author").value(book.getAuthor()))
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.inStock").exists());
    }

    @Test
    @DisplayName("書籍詳細取得 - 存在しないID")
    void getBookById_NotFound() throws Exception {
        mockMvc.perform(get("/books/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("見つかりません")));
    }

    @Test
    @DisplayName("書籍検索 - キーワード検索")
    void searchBooks_Success() throws Exception {
        mockMvc.perform(get("/books/search")
                        .param("q", "Spring")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("カテゴリ別書籍検索 - 成功")
    void getBooksByCategory_Success() throws Exception {
        mockMvc.perform(get("/books")
                        .param("category", "技術書")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray());
    }

    @Test
    @DisplayName("人気書籍一覧取得 - 成功")
    void getPopularBooks_Success() throws Exception {
        mockMvc.perform(get("/books/popular")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("高評価書籍一覧取得 - 成功")
    void getTopRatedBooks_Success() throws Exception {
        mockMvc.perform(get("/books/top-rated")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("在庫のある書籍一覧取得 - 成功")
    void getBooksInStock_Success() throws Exception {
        mockMvc.perform(get("/books/in-stock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("ISBN検索 - 成功")
    void getBookByIsbn_Success() throws Exception {
        // Given
        Book book = bookRepository.findAll().get(0);
        String isbn = book.getIsbn();

        // When & Then
        mockMvc.perform(get("/books/isbn/{isbn}", isbn)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.title").value(book.getTitle()));
    }

    @Test
    @DisplayName("カテゴリ一覧取得 - 成功")
    void getCategories_Success() throws Exception {
        mockMvc.perform(get("/books/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("不正なページ番号でのリクエスト")
    void getBooks_InvalidPageNumber() throws Exception {
        mockMvc.perform(get("/books")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("不正なページサイズでのリクエスト")
    void getBooks_InvalidPageSize() throws Exception {
        mockMvc.perform(get("/books")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("ソート条件指定での書籍一覧取得")
    void getBooks_WithSorting_Success() throws Exception {
        mockMvc.perform(get("/books")
                        .param("sortBy", "price_asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.books").isArray());
    }

    private void createTestData() {
        // テストユーザー作成
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRole(UserRole.CONSUMER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(testUser);

        // テスト書籍作成
        Book book1 = new Book();
        book1.setTitle("Spring Boot実践入門");
        book1.setAuthor("技術太郎");
        book1.setIsbn("9784000000001");
        book1.setPrice(BigDecimal.valueOf(3200));
        book1.setDescription("Spring Bootの実践的な入門書");
        book1.setCategory("技術書");
        book1.setStockQuantity(25);
        book1.setAverageRating(BigDecimal.valueOf(4.5));
        book1.setReviewCount(12);
        book1.setImageUrl("http://test.com/spring-boot.jpg");
        book1.setCreatedAt(LocalDateTime.now());
        book1.setUpdatedAt(LocalDateTime.now());

        Book book2 = new Book();
        book2.setTitle("Java設計パターン");
        book2.setAuthor("パターン花子");
        book2.setIsbn("9784000000002");
        book2.setPrice(BigDecimal.valueOf(2800));
        book2.setDescription("Javaの設計パターン解説書");
        book2.setCategory("技術書");
        book2.setStockQuantity(15);
        book2.setAverageRating(BigDecimal.valueOf(4.2));
        book2.setReviewCount(8);
        book2.setImageUrl("http://test.com/java-patterns.jpg");
        book2.setCreatedAt(LocalDateTime.now());
        book2.setUpdatedAt(LocalDateTime.now());

        bookRepository.save(book1);
        bookRepository.save(book2);

        // JWT トークン生成
        authToken = jwtService.generateToken(testUser);
    }
}