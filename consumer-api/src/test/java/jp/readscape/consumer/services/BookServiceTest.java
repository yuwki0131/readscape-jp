package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.reviews.repository.ReviewRepository;
import jp.readscape.consumer.dto.books.BookDetail;
import jp.readscape.consumer.dto.books.BookSummary;
import jp.readscape.consumer.dto.books.BooksResponse;
import jp.readscape.consumer.exceptions.BookNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService テスト")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("書籍一覧取得 - 成功")
    void findBooks_Success() {
        // Given
        Book book1 = createTestBook(1L, "Test Book 1", "Author 1");
        Book book2 = createTestBook(2L, "Test Book 2", "Author 2");
        List<Book> books = Arrays.asList(book1, book2);
        Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 10), 2);

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);

        // When
        BooksResponse response = bookService.findBooks(null, null, 0, 10, "newest");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBooks()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(1);

        verify(bookRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("書籍詳細取得 - 成功")
    void findBookById_Success() {
        // Given
        Long bookId = 1L;
        Book book = createTestBook(bookId, "Test Book", "Test Author");
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        // When
        BookDetail result = bookService.findBookById(bookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(bookId);
        assertThat(result.getTitle()).isEqualTo("Test Book");
        assertThat(result.getAuthor()).isEqualTo("Test Author");

        verify(bookRepository).findById(bookId);
    }

    @Test
    @DisplayName("書籍詳細取得 - 書籍が見つからない場合")
    void findBookById_NotFound() {
        // Given
        Long bookId = 999L;
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.findBookById(bookId))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("Book not found with id: " + bookId);

        verify(bookRepository).findById(bookId);
    }

    @Test
    @DisplayName("ISBN検索 - 成功")
    void findBookByIsbn_Success() {
        // Given
        String isbn = "9784000000001";
        Book book = createTestBook(1L, "Test Book", "Test Author");
        book.setIsbn(isbn);
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(book));

        // When
        BookDetail result = bookService.findBookByIsbn(isbn);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsbn()).isEqualTo(isbn);
        assertThat(result.getTitle()).isEqualTo("Test Book");

        verify(bookRepository).findByIsbn(isbn);
    }

    @Test
    @DisplayName("カテゴリ別検索 - 成功")
    void findBooks_WithCategory_Success() {
        // Given
        String category = "技術書";
        Book book = createTestBook(1L, "Spring Boot入門", "技術太郎");
        Page<Book> bookPage = new PageImpl<>(Arrays.asList(book));

        when(bookRepository.findByCategoryContainingIgnoreCase(eq(category), any(Pageable.class)))
                .thenReturn(bookPage);

        // When
        BooksResponse response = bookService.findBooks(category, null, 0, 10, "newest");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBooks()).hasSize(1);
        assertThat(response.getBooks().get(0).getTitle()).isEqualTo("Spring Boot入門");

        verify(bookRepository).findByCategoryContainingIgnoreCase(eq(category), any(Pageable.class));
    }

    @Test
    @DisplayName("キーワード検索 - 成功")
    void findBooks_WithKeyword_Success() {
        // Given
        String keyword = "Spring";
        Book book = createTestBook(1L, "Spring Boot入門", "技術太郎");
        Page<Book> bookPage = new PageImpl<>(Arrays.asList(book));

        when(bookRepository.findByTitleOrAuthorContaining(eq(keyword), any(Pageable.class)))
                .thenReturn(bookPage);

        // When
        BooksResponse response = bookService.findBooks(null, keyword, 0, 10, "newest");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBooks()).hasSize(1);

        verify(bookRepository).findByTitleOrAuthorContaining(eq(keyword), any(Pageable.class));
    }

    @Test
    @DisplayName("人気書籍取得 - 成功")
    void findPopularBooks_Success() {
        // Given
        int limit = 5;
        Book book1 = createTestBook(1L, "Popular Book 1", "Author 1");
        Book book2 = createTestBook(2L, "Popular Book 2", "Author 2");
        List<Book> popularBooks = Arrays.asList(book1, book2);

        when(bookRepository.findPopularBooks(any(Pageable.class))).thenReturn(popularBooks);

        // When
        List<BookSummary> result = bookService.findPopularBooks(limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(bookRepository).findPopularBooks(any(Pageable.class));
    }

    @Test
    @DisplayName("評価の高い書籍取得 - 成功")
    void findTopRatedBooks_Success() {
        // Given
        int limit = 5;
        Book book = createTestBook(1L, "Top Rated Book", "Author");
        book.setAverageRating(BigDecimal.valueOf(4.8));
        List<Book> topRatedBooks = Arrays.asList(book);

        when(bookRepository.findTopRatedBooks(any(Pageable.class))).thenReturn(topRatedBooks);

        // When
        List<BookSummary> result = bookService.findTopRatedBooks(limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAverageRating()).isEqualTo(BigDecimal.valueOf(4.8));

        verify(bookRepository).findTopRatedBooks(any(Pageable.class));
    }

    @Test
    @DisplayName("在庫のある書籍検索 - 成功")
    void findBooksInStock_Success() {
        // Given
        Book book = createTestBook(1L, "In Stock Book", "Author");
        book.setStockQuantity(10);
        Page<Book> bookPage = new PageImpl<>(Arrays.asList(book));

        when(bookRepository.findByStockQuantityGreaterThan(eq(0), any(Pageable.class)))
                .thenReturn(bookPage);

        // When
        BooksResponse result = bookService.findBooksInStock(0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBooks()).hasSize(1);
        assertThat(result.getBooks().get(0).isInStock()).isTrue();

        verify(bookRepository).findByStockQuantityGreaterThan(eq(0), any(Pageable.class));
    }

    @Test
    @DisplayName("書籍評価更新 - 成功")
    void updateBookRating_Success() {
        // Given
        Long bookId = 1L;
        Book book = createTestBook(bookId, "Test Book", "Author");
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(reviewRepository.calculateAverageRatingByBookId(bookId)).thenReturn(4.5);
        when(reviewRepository.countByBookId(bookId)).thenReturn(10L);

        // When
        bookService.updateBookRating(bookId);

        // Then
        verify(bookRepository).findById(bookId);
        verify(reviewRepository).calculateAverageRatingByBookId(bookId);
        verify(reviewRepository).countByBookId(bookId);
        verify(bookRepository).save(book);

        assertThat(book.getAverageRating()).isEqualTo(BigDecimal.valueOf(4.5));
        assertThat(book.getReviewCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("全書籍評価更新 - 成功")
    void updateAllBookRatings_Success() {
        // Given
        Book book1 = createTestBook(1L, "Book 1", "Author 1");
        Book book2 = createTestBook(2L, "Book 2", "Author 2");
        List<Book> books = Arrays.asList(book1, book2);

        when(bookRepository.findAll()).thenReturn(books);
        when(reviewRepository.calculateAverageRatingByBookId(1L)).thenReturn(4.0);
        when(reviewRepository.calculateAverageRatingByBookId(2L)).thenReturn(4.5);
        when(reviewRepository.countByBookId(1L)).thenReturn(5L);
        when(reviewRepository.countByBookId(2L)).thenReturn(8L);

        // When
        bookService.updateAllBookRatings();

        // Then
        verify(bookRepository).findAll();
        verify(bookRepository).saveAll(books);
        
        assertThat(book1.getAverageRating()).isEqualTo(BigDecimal.valueOf(4.0));
        assertThat(book2.getAverageRating()).isEqualTo(BigDecimal.valueOf(4.5));
    }

    // Helper method
    private Book createTestBook(Long id, String title, String author) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn("9784000000000");
        book.setPrice(BigDecimal.valueOf(2500));
        book.setDescription("Test description");
        book.setCategory("テスト");
        book.setStockQuantity(10);
        book.setAverageRating(BigDecimal.valueOf(4.0));
        book.setReviewCount(5);
        book.setImageUrl("http://test.com/image.jpg");
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        return book;
    }
}