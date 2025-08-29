# Java コーディング規約

## 概要

Readscape-JPプロジェクトにおけるJavaコードの統一性、可読性、保守性を確保するための規約です。Google Java Style Guideをベースとし、Spring Boot固有の慣例を含めています。

## 基本原則

1. **可読性**: コードは書く回数より読む回数が多い
2. **一貫性**: プロジェクト全体で統一されたスタイル
3. **简潔性**: 必要十分で冗長でないコード
4. **保守性**: 変更・拡張が容易な構造

## ファイル構成規約

### パッケージ構造

```
src/main/java/jp/readscape/{api-name}/
├── config/                    # 設定クラス
│   ├── SecurityConfig.java
│   ├── DatabaseConfig.java
│   └── OpenApiConfig.java
├── controllers/               # REST コントローラー
│   ├── books/
│   │   ├── BooksController.java
│   │   └── response/
│   │       ├── BookSummary.java
│   │       └── BookDetail.java
│   ├── auth/
│   └── users/
├── services/                  # ビジネスロジック
│   ├── BookService.java
│   ├── UserService.java
│   └── impl/
│       ├── BookServiceImpl.java
│       └── UserServiceImpl.java
├── repositories/             # データアクセス層
│   ├── BookRepository.java
│   └── UserRepository.java
├── domain/                   # ドメインモデル
│   ├── entities/
│   │   ├── Book.java
│   │   ├── User.java
│   │   └── Order.java
│   └── enums/
│       ├── BookStatus.java
│       └── OrderStatus.java
├── dto/                      # データ転送オブジェクト
│   ├── requests/
│   │   ├── CreateBookRequest.java
│   │   └── UpdateBookRequest.java
│   ├── responses/
│   │   ├── ApiResponse.java
│   │   └── BooksResponse.java
│   └── common/
│       └── PaginationInfo.java
├── exceptions/               # 例外クラス
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   └── handlers/
│       └── GlobalExceptionHandler.java
├── utils/                    # ユーティリティ
│   ├── DateUtils.java
│   ├── StringUtils.java
│   └── ValidationUtils.java
└── constants/               # 定数クラス
    ├── ApiConstants.java
    └── MessageConstants.java
```

### ファイル命名規則

| 種類 | 命名規則 | 例 |
|------|----------|-----|
| Controller | {Entity}Controller | BooksController |
| Service | {Entity}Service | BookService |
| Service Impl | {Entity}ServiceImpl | BookServiceImpl |
| Repository | {Entity}Repository | BookRepository |
| Entity | {EntityName} | Book, User |
| DTO Request | {Action}{Entity}Request | CreateBookRequest |
| DTO Response | {Entity}{Response/Summary/Detail} | BookSummary |
| Exception | {Specific}Exception | BookNotFoundException |
| Configuration | {Purpose}Config | SecurityConfig |
| Utility | {Purpose}Utils | ValidationUtils |
| Constants | {Purpose}Constants | ApiConstants |

## 命名規約

### 1. クラス名

**規則**: PascalCase を使用

```java
// ✅ 推奨
public class BookController {
}

public class UserServiceImpl {
}

public class CreateBookRequest {
}

// ❌ 非推奨
public class bookController {        // 先頭小文字
public class BOOKCONTROLLER {       // 全て大文字
public class Book_Controller {      // アンダースコア使用
```

### 2. メソッド名

**規則**: camelCase、動詞または動詞句で開始

```java
// ✅ 推奨
public Book findBookById(Long id) {
}

public List<Book> searchBooksByKeyword(String keyword) {
}

public boolean isBookAvailable(Long bookId) {
}

public void validateBookRequest(CreateBookRequest request) {
}

// ❌ 非推奨
public Book FindBookById(Long id) {         // PascalCase
public List<Book> searchbooks(String keyword) { // キャメルケース不適切
public boolean bookAvailable(Long bookId) {     // 動詞なし
```

### 3. 変数名

**規則**: camelCase、名詞または名詞句

```java
// ✅ 推奨
private final BookService bookService;
private String userEmail;
private List<Book> availableBooks;
private int maxRetryCount;

// ❌ 非推奨
private final BookService BookService;  // PascalCase
private String user_email;             // スネークケース
private List<Book> books1;             // 意味のない数字
private int max_retry_count;          // スネークケース
```

### 4. 定数名

**規則**: UPPER_SNAKE_CASE

```java
// ✅ 推奨
public static final int MAX_BOOKS_PER_ORDER = 99;
public static final String DEFAULT_SORT_FIELD = "createdAt";
public static final Duration JWT_EXPIRATION_TIME = Duration.ofHours(1);

// ❌ 非推奨
public static final int maxBooksPerOrder = 99;  // camelCase
public static final String DefaultSortField = "createdAt"; // PascalCase
```

### 5. パッケージ名

**規則**: 全て小文字、ドット区切り

```java
// ✅ 推奨
package jp.readscape.consumer.controllers.books;
package jp.readscape.inventory.services.impl;

// ❌ 非推奨  
package jp.readscape.Consumer.controllers.books; // 大文字混在
package jp.readscape.consumer.controllers.Books; // 最後が大文字
```

## コード構造規約

### 1. クラス内要素の順序

```java
public class BookController {
    
    // 1. 定数（static final）
    private static final String DEFAULT_SORT = "createdAt";
    
    // 2. フィールド（依存性注入対象）
    private final BookService bookService;
    private final ValidationUtils validationUtils;
    
    // 3. コンストラクタ
    public BookController(BookService bookService, ValidationUtils validationUtils) {
        this.bookService = bookService;
        this.validationUtils = validationUtils;
    }
    
    // 4. publicメソッド（APIエンドポイント）
    @GetMapping
    public ResponseEntity<BooksResponse> getBooks(/* parameters */) {
        // implementation
    }
    
    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody CreateBookRequest request) {
        // implementation
    }
    
    // 5. privateメソッド（ヘルパーメソッド）
    private void validateRequest(CreateBookRequest request) {
        // implementation
    }
}
```

### 2. Import文の規約

**順序**:
1. java.* と javax.*
2. サードパーティライブラリ（org.*, com.*）
3. Spring Framework（org.springframework.*）
4. プロジェクト内パッケージ（jp.readscape.*）

```java
// ✅ 推奨順序
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.readscape.consumer.dto.BookSummary;
import jp.readscape.consumer.services.BookService;
```

## Spring Boot 固有規約

### 1. アノテーション使用規約

#### Controller層
```java
@RestController                              // 必須
@RequestMapping("/api/books")                // エンドポイントベースパス
@RequiredArgsConstructor                     // Lombok - コンストラクタ注入
@Slf4j                                       // Lombok - ログ機能
@Tag(name = "Books", description = "書籍API")  // OpenAPI
@Validated                                   // Bean Validation
public class BooksController {

    private final BookService bookService;
    
    @Operation(summary = "書籍一覧取得")        // OpenAPI
    @GetMapping                              // HTTPメソッド指定
    public ResponseEntity<BooksResponse> getBooks(
        @Parameter(description = "ページ番号")    // OpenAPI
        @RequestParam(defaultValue = "0") Integer page
    ) {
        // implementation
    }
}
```

#### Service層
```java
@Service                        // 必須
@RequiredArgsConstructor        // Lombok
@Transactional(readOnly = true) // デフォルトは読み取り専用
public class BookService {
    
    private final BookRepository bookRepository;
    
    @Transactional              // 更新処理は明示的に指定
    public Book createBook(CreateBookRequest request) {
        // implementation
    }
}
```

#### Repository層
```java
@Repository                     // Spring Data JPAでは省略可能だが推奨
public interface BookRepository extends JpaRepository<Book, Long> {
    
    @Query("SELECT b FROM Book b WHERE b.title LIKE %:keyword%")
    List<Book> findByTitleContaining(@Param("keyword") String keyword);
    
    @Modifying                  // 更新・削除クエリに必須
    @Query("UPDATE Book b SET b.status = :status WHERE b.id = :id")
    void updateBookStatus(@Param("id") Long id, @Param("status") BookStatus status);
}
```

### 2. 例外ハンドリング

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
            .error("BOOK_NOT_FOUND")
            .message(ex.getMessage())
            .status(HttpStatus.NOT_FOUND.value())
            .timestamp(LocalDateTime.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex) {
        
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> FieldError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .build())
            .collect(Collectors.toList());
            
        ApiErrorResponse error = ApiErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message("入力値に誤りがあります")
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .fieldErrors(fieldErrors)
            .build();
            
        return ResponseEntity.badRequest().body(error);
    }
}
```

### 3. Bean Validation 規約

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {
    
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;
    
    @NotBlank(message = "著者名は必須です")  
    @Size(max = 100, message = "著者名は100文字以内で入力してください")
    private String author;
    
    @Pattern(
        regexp = "^97[8-9][0-9]{10}$",
        message = "有効なISBN-13形式で入力してください"
    )
    private String isbn;
    
    @Min(value = 1, message = "価格は1円以上で入力してください")
    @Max(value = 999999, message = "価格は999,999円以下で入力してください")
    private Integer price;
    
    @Size(max = 2000, message = "説明は2000文字以内で入力してください")
    private String description;
    
    @NotNull(message = "出版日は必須です")
    @PastOrPresent(message = "出版日は現在または過去の日付を指定してください")
    private LocalDate publicationDate;
}
```

## フォーマット規約

### 1. インデント

- **スペース4つ** を使用（タブ文字は使用しない）
- ブレース内は1レベル追加でインデント

```java
// ✅ 推奨
public class BookService {
    
    public Book findBookById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        
        return bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + id));
    }
}
```

### 2. 行長制限

- **120文字** を目安とする
- 超える場合は適切な位置で改行

```java
// ✅ 推奨
public ResponseEntity<BooksResponse> searchBooks(
        String keyword,
        String category, 
        Integer page,
        Integer size) {
    // implementation
}

// ✅ 推奨（メソッドチェーンの改行）
List<BookSummary> books = bookRepository.findAll()
    .stream()
    .filter(book -> book.isAvailable())
    .map(bookMapper::toSummary)
    .collect(Collectors.toList());
```

### 3. ブレースの配置

```java
// ✅ 推奨（同一行開始）
public void processBooks() {
    if (books.isEmpty()) {
        log.warn("No books to process");
        return;
    }
    
    for (Book book : books) {
        processBook(book);
    }
}

// ❌ 非推奨（次行開始）
public void processBooks() 
{
    // implementation
}
```

## コメント規約

### 1. JavaDoc

**すべてのpublicクラス・メソッドにJavaDocを記述**

```java
/**
 * 書籍管理サービス
 * 
 * <p>書籍の作成、更新、検索、削除などの機能を提供します。
 * 全ての操作はトランザクション管理され、適切な例外処理が行われます。</p>
 * 
 * @author Readscape Development Team
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class BookService {
    
    /**
     * 指定されたIDの書籍を取得します
     * 
     * @param id 書籍ID（必須）
     * @return 書籍情報
     * @throws BookNotFoundException 書籍が見つからない場合
     * @throws IllegalArgumentException IDがnullの場合
     */
    public Book findBookById(Long id) {
        // implementation
    }
}
```

### 2. インラインコメント

```java
public void processOrder(Order order) {
    // ビジネスルール: 在庫数量をチェック
    if (!inventoryService.isStockSufficient(order.getItems())) {
        throw new InsufficientStockException("在庫不足です");
    }
    
    // TODO: 将来的には非同期処理に変更予定
    paymentService.processPayment(order);
    
    // NOTE: この処理は必ずトランザクション内で実行する必要がある
    orderRepository.save(order);
}
```

## ログ出力規約

### 1. ログレベル使い分け

```java
@Slf4j
@Service
public class BookService {
    
    public Book createBook(CreateBookRequest request) {
        log.info("Creating new book: title={}", request.getTitle()); // 正常処理
        
        try {
            Book book = bookMapper.toEntity(request);
            Book savedBook = bookRepository.save(book);
            
            log.info("Book created successfully: id={}, title={}", 
                savedBook.getId(), savedBook.getTitle());
                
            return savedBook;
            
        } catch (DataIntegrityViolationException ex) {
            log.warn("Failed to create book due to constraint violation: {}", 
                ex.getMessage()); // 想定される業務エラー
            throw new DuplicateBookException("同じISBNの書籍が既に存在します");
            
        } catch (Exception ex) {
            log.error("Unexpected error occurred while creating book", ex); // 予期しないエラー
            throw new BookCreationException("書籍作成中にエラーが発生しました");
        }
    }
}
```

### 2. 構造化ログ

```java
// MDC（Mapped Diagnostic Context）の活用
@RestController
public class BooksController {
    
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody CreateBookRequest request) {
        MDC.put("operation", "createBook");
        MDC.put("isbn", request.getIsbn());
        
        try {
            Book book = bookService.createBook(request);
            log.info("Book creation completed successfully");
            return ResponseEntity.ok(book);
        } finally {
            MDC.clear();
        }
    }
}
```

## テストコード規約

### 1. テストクラス命名

```java
// ✅ 推奨
class BookServiceTest {           // 単体テスト
}

class BookControllerTest {        // 単体テスト
}

class BookControllerIntegrationTest { // 統合テスト
}

class BookApiTest {              // API テスト
}
```

### 2. テストメソッド構造

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @InjectMocks
    private BookService bookService;
    
    @DisplayName("有効なIDで書籍を取得できる")
    @Test
    void findBookById_WithValidId_ReturnsBook() {
        // Given
        Long bookId = 1L;
        Book expectedBook = Book.builder()
            .id(bookId)
            .title("Test Book")
            .author("Test Author")
            .build();
            
        when(bookRepository.findById(bookId))
            .thenReturn(Optional.of(expectedBook));
        
        // When
        Book actualBook = bookService.findBookById(bookId);
        
        // Then
        assertThat(actualBook).isNotNull();
        assertThat(actualBook.getId()).isEqualTo(bookId);
        assertThat(actualBook.getTitle()).isEqualTo("Test Book");
    }
    
    @DisplayName("存在しないIDで書籍取得時に例外が発生する")
    @Test
    void findBookById_WithNonExistentId_ThrowsException() {
        // Given
        Long nonExistentId = 999L;
        when(bookRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> bookService.findBookById(nonExistentId))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessage("Book not found: " + nonExistentId);
    }
}
```

## 品質チェック自動化

### 1. Checkstyle 設定

```xml
<!-- checkstyle.xml -->
<module name="Checker">
    <module name="TreeWalker">
        <!-- 命名規約 -->
        <module name="TypeName"/>
        <module name="MethodName"/>
        <module name="VariableName"/>
        <module name="ConstantName"/>
        <module name="PackageName"/>
        
        <!-- フォーマット -->
        <module name="Indentation">
            <property name="basicOffset" value="4"/>
        </module>
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
        
        <!-- コード品質 -->
        <module name="UnusedImports"/>
        <module name="RedundantImport"/>
        <module name="EmptyBlock"/>
        <module name="NeedBraces"/>
    </module>
</module>
```

### 2. SpotBugs 設定

```gradle
plugins {
    id 'com.github.spotbugs' version '5.0.14'
}

spotbugs {
    toolVersion = '4.7.3'
    reportLevel = 'medium'
    effort = 'max'
    includeFilter = file('config/spotbugs/include.xml')
    excludeFilter = file('config/spotbugs/exclude.xml')
}
```

### 3. SonarQube 品質ゲート

```gradle
plugins {
    id 'org.sonarqube' version '4.0.0.2929'
}

sonarqube {
    properties {
        property 'sonar.projectKey', 'readscape-jp'
        property 'sonar.coverage.jacoco.xmlReportPaths', 'build/reports/jacoco/test/jacocoTestReport.xml'
        property 'sonar.java.checkstyle.reportPaths', 'build/reports/checkstyle/main.xml'
        property 'sonar.java.spotbugs.reportPaths', 'build/reports/spotbugs/main.xml'
    }
}
```

## レビューチェックリスト

### コードレビュー時の確認項目

#### 基本事項
- [ ] 命名規約に従っているか
- [ ] フォーマットが統一されているか
- [ ] importが整理されているか
- [ ] 不要なコメントがないか

#### Spring Boot固有
- [ ] 適切なアノテーションが使用されているか
- [ ] トランザクション境界が適切か
- [ ] 例外ハンドリングが適切か
- [ ] Bean Validation が適用されているか

#### セキュリティ
- [ ] SQLインジェクション対策がされているか
- [ ] 入力値検証が適切か
- [ ] 機密情報がログ出力されていないか

#### パフォーマンス
- [ ] N+1問題が発生していないか
- [ ] 不要なデータベースアクセスがないか
- [ ] キャッシュが適切に活用されているか

#### テスタビリティ
- [ ] 単体テストが書けるか
- [ ] 依存関係が適切に注入されているか
- [ ] モック化が容易な設計か

この規約は継続的に改善し、チーム全体の合意のもとで更新していきます。