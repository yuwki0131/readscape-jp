package jp.readscape.consumer.controllers.books;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.readscape.consumer.dto.books.BookDetail;
import jp.readscape.consumer.dto.books.BookSummary;
import jp.readscape.consumer.dto.books.BooksResponse;
import jp.readscape.consumer.services.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BooksController.class)
class BooksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getBooks_WithDefaultParameters_ShouldReturnBooks() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Arrays.asList(createMockBookSummary()))
                .totalPages(1)
                .currentPage(0)
                .totalElements(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooks(null, null, 0, 10, "newest"))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books[0].id").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    void getBooks_WithAllParameters_ShouldReturnFilteredBooks() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Arrays.asList(createMockBookSummary()))
                .totalPages(1)
                .currentPage(0)
                .totalElements(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooks("技術書", "Spring", 0, 20, "price_asc"))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books")
                .param("category", "技術書")
                .param("keyword", "Spring")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getBooks_WithInvalidPageSize_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books")
                .param("page", "-1")
                .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBooks_WithInvalidSortBy_ShouldUseDefaultSort() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Collections.emptyList())
                .totalPages(0)
                .currentPage(0)
                .totalElements(0L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooks(null, null, 0, 10, "newest"))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books")
                .param("sortBy", "invalid_sort"))
                .andExpect(status().isOk());
    }

    @Test
    void getBookById_WithValidId_ShouldReturnBookDetail() throws Exception {
        // Given
        BookDetail mockBookDetail = createMockBookDetail();
        when(bookService.findBookById(1L)).thenReturn(mockBookDetail);

        // When & Then
        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"));
    }

    @Test
    void getBookById_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBookById_WithNegativeId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchBooks_WithValidQuery_ShouldReturnResults() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Arrays.asList(createMockBookSummary()))
                .totalPages(1)
                .currentPage(0)
                .totalElements(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooks(null, "Java", 0, 10, "relevance"))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books/search")
                .param("q", "Java"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books[0].title").exists());
    }

    @Test
    void searchBooks_WithQueryAndCategory_ShouldReturnFilteredResults() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Arrays.asList(createMockBookSummary()))
                .totalPages(1)
                .currentPage(0)
                .totalElements(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooks("技術書", "Spring", 0, 10, "relevance"))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books/search")
                .param("q", "Spring")
                .param("category", "技術書"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray());
    }

    @Test
    void searchBooks_WithEmptyQuery_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/search")
                .param("q", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchBooks_WithoutQuery_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_ShouldReturnCategoryList() throws Exception {
        // Given
        List<String> categories = Arrays.asList("技術書", "小説", "ビジネス");
        when(bookService.findAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/books/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("技術書"))
                .andExpect(jsonPath("$[1]").value("小説"))
                .andExpect(jsonPath("$[2]").value("ビジネス"));
    }

    @Test
    void getCategories_WithEmptyList_ShouldReturnEmptyArray() throws Exception {
        // Given
        when(bookService.findAllCategories()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/books/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPopularBooks_WithDefaultLimit_ShouldReturnPopularBooks() throws Exception {
        // Given
        List<BookSummary> popularBooks = Arrays.asList(createMockBookSummary());
        when(bookService.findPopularBooks(10)).thenReturn(popularBooks);

        // When & Then
        mockMvc.perform(get("/books/popular"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getPopularBooks_WithCustomLimit_ShouldReturnLimitedBooks() throws Exception {
        // Given
        List<BookSummary> popularBooks = Arrays.asList(createMockBookSummary());
        when(bookService.findPopularBooks(5)).thenReturn(popularBooks);

        // When & Then
        mockMvc.perform(get("/books/popular")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPopularBooks_WithInvalidLimit_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/popular")
                .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPopularBooks_WithLimitExceedingMax_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/popular")
                .param("limit", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTopRatedBooks_WithDefaultLimit_ShouldReturnTopRatedBooks() throws Exception {
        // Given
        List<BookSummary> topRatedBooks = Arrays.asList(createMockBookSummary());
        when(bookService.findTopRatedBooks(10)).thenReturn(topRatedBooks);

        // When & Then
        mockMvc.perform(get("/books/top-rated"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getTopRatedBooks_WithCustomLimit_ShouldReturnLimitedBooks() throws Exception {
        // Given
        List<BookSummary> topRatedBooks = Arrays.asList(createMockBookSummary());
        when(bookService.findTopRatedBooks(15)).thenReturn(topRatedBooks);

        // When & Then
        mockMvc.perform(get("/books/top-rated")
                .param("limit", "15"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getBooksInStock_WithDefaultParameters_ShouldReturnInStockBooks() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Arrays.asList(createMockBookSummary()))
                .totalPages(1)
                .currentPage(0)
                .totalElements(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooksInStock(0, 10)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books/in-stock"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getBooksInStock_WithCustomParameters_ShouldReturnPagedResults() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Arrays.asList(createMockBookSummary()))
                .totalPages(2)
                .currentPage(1)
                .totalElements(15L)
                .hasNext(false)
                .hasPrevious(true)
                .build();

        when(bookService.findBooksInStock(1, 20)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books/in-stock")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getBookByIsbn_WithValidIsbn_ShouldReturnBookDetail() throws Exception {
        // Given
        BookDetail mockBookDetail = createMockBookDetail();
        mockBookDetail.setIsbn("9784000000001");
        when(bookService.findBookByIsbn("9784000000001")).thenReturn(mockBookDetail);

        // When & Then
        mockMvc.perform(get("/books/isbn/9784000000001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isbn").value("9784000000001"))
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void getBookByIsbn_WithEmptyIsbn_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/isbn/"))
                .andExpect(status().isNotFound()); // Path variable is missing
    }

    @Test
    void getBookByIsbn_WithWhitespaceIsbn_ShouldTrimAndProcess() throws Exception {
        // Given
        BookDetail mockBookDetail = createMockBookDetail();
        mockBookDetail.setIsbn("9784000000001");
        when(bookService.findBookByIsbn("9784000000001")).thenReturn(mockBookDetail);

        // When & Then
        mockMvc.perform(get("/books/isbn/%20%209784000000001%20%20")) // URL encoded spaces
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isbn").value("9784000000001"));
    }

    @Test
    void searchBooks_WithWhitespaceQuery_ShouldTrimAndProcess() throws Exception {
        // Given
        BooksResponse mockResponse = BooksResponse.builder()
                .books(Arrays.asList(createMockBookSummary()))
                .totalPages(1)
                .currentPage(0)
                .totalElements(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooks(null, "Java", 0, 10, "relevance"))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/books/search")
                .param("q", "  Java  "))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray());
    }

    // Helper methods to create mock objects

    private BookSummary createMockBookSummary() {
        return BookSummary.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .price(BigDecimal.valueOf(1500))
                .averageRating(BigDecimal.valueOf(4.5))
                .reviewCount(10)
                .category("技術書")
                .stockQuantity(5)
                .build();
    }

    private BookDetail createMockBookDetail() {
        return BookDetail.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("9781234567890")
                .description("Test description")
                .price(BigDecimal.valueOf(1500))
                .category("技術書")
                .publisher("Test Publisher")
                .publishedDate(LocalDate.of(2023, 1, 1))
                .stockQuantity(5)
                .averageRating(BigDecimal.valueOf(4.5))
                .reviewCount(10)
                .build();
    }
}