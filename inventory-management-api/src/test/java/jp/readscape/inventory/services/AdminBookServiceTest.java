package jp.readscape.inventory.services;

import jp.readscape.inventory.domain.books.model.Book;
import jp.readscape.inventory.domain.books.repository.BookRepository;
import jp.readscape.inventory.domain.inventory.repository.StockHistoryRepository;
import jp.readscape.inventory.dto.admin.AdminBookView;
import jp.readscape.inventory.dto.admin.CreateBookRequest;
import jp.readscape.inventory.dto.admin.UpdateBookRequest;
import jp.readscape.inventory.exceptions.BookNotFoundException;
import jp.readscape.inventory.exceptions.DuplicateIsbnException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBookService Test")
class AdminBookServiceTest {

    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private StockHistoryRepository stockHistoryRepository;
    
    @Mock
    private DtoMappingService dtoMappingService;
    
    @InjectMocks
    private AdminBookService adminBookService;

    @Test
    @DisplayName("書籍一覧取得 - ステータス指定なし")
    void getBooksWithoutStatus() {
        // Arrange
        List<Book> books = createSampleBooks();
        Page<Book> bookPage = new PageImpl<>(books);
        List<AdminBookView> expectedViews = createSampleAdminBookViews();
        
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);
        when(dtoMappingService.toAdminBookView(any(Book.class))).thenReturn(expectedViews.get(0));
        
        // Act
        Page<AdminBookView> result = adminBookService.getBooks(0, 10, null);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(bookRepository).findAll(any(Pageable.class));
        verify(dtoMappingService, times(2)).toAdminBookView(any(Book.class));
    }

    @Test
    @DisplayName("書籍一覧取得 - ステータス指定あり")
    void getBooksWithStatus() {
        // Arrange
        List<Book> books = createSampleBooks();
        Page<Book> bookPage = new PageImpl<>(books);
        
        when(bookRepository.findByStatus(eq(Book.BookStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(bookPage);
        when(dtoMappingService.toAdminBookView(any(Book.class)))
            .thenReturn(createSampleAdminBookViews().get(0));
        
        // Act
        Page<AdminBookView> result = adminBookService.getBooks(0, 10, "ACTIVE");
        
        // Assert
        assertThat(result).isNotNull();
        verify(bookRepository).findByStatus(eq(Book.BookStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("書籍一覧取得 - 不正なステータス指定")
    void getBooksWithInvalidStatus() {
        // Arrange
        List<Book> books = createSampleBooks();
        Page<Book> bookPage = new PageImpl<>(books);
        
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);
        when(dtoMappingService.toAdminBookView(any(Book.class)))
            .thenReturn(createSampleAdminBookViews().get(0));
        
        // Act
        Page<AdminBookView> result = adminBookService.getBooks(0, 10, "INVALID_STATUS");
        
        // Assert
        assertThat(result).isNotNull();
        verify(bookRepository).findAll(any(Pageable.class));
        verify(bookRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("書籍詳細取得 - 正常系")
    void getBookByIdSuccess() {
        // Arrange
        Long bookId = 1L;
        Book book = createSampleBook();
        AdminBookView expectedView = createSampleAdminBookViews().get(0);
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(dtoMappingService.toAdminBookView(book)).thenReturn(expectedView);
        
        // Act
        AdminBookView result = adminBookService.getBookById(bookId);
        
        // Assert
        assertThat(result).isEqualTo(expectedView);
        verify(bookRepository).findById(bookId);
        verify(dtoMappingService).toAdminBookView(book);
    }

    @Test
    @DisplayName("書籍詳細取得 - 存在しない書籍")
    void getBookByIdNotFound() {
        // Arrange
        Long bookId = 999L;
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> adminBookService.getBookById(bookId))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining("Book not found");
        
        verify(bookRepository).findById(bookId);
        verifyNoInteractions(dtoMappingService);
    }

    @Test
    @DisplayName("書籍作成 - 正常系")
    void createBookSuccess() {
        // Arrange
        CreateBookRequest request = createBookRequest();
        Book savedBook = createSampleBook();
        AdminBookView expectedView = createSampleAdminBookViews().get(0);
        
        when(bookRepository.existsByIsbn(request.getIsbn())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        when(dtoMappingService.toAdminBookView(savedBook)).thenReturn(expectedView);
        
        // Act
        AdminBookView result = adminBookService.createBook(request, 1L);
        
        // Assert
        assertThat(result).isEqualTo(expectedView);
        verify(bookRepository).existsByIsbn(request.getIsbn());
        verify(bookRepository).save(any(Book.class));
        verify(dtoMappingService).toAdminBookView(savedBook);
    }

    @Test
    @DisplayName("書籍作成 - ISBN重複エラー")
    void createBookDuplicateIsbn() {
        // Arrange
        CreateBookRequest request = createBookRequest();
        
        when(bookRepository.existsByIsbn(request.getIsbn())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> adminBookService.createBook(request, 1L))
            .isInstanceOf(DuplicateIsbnException.class)
            .hasMessageContaining("ISBN already exists");
        
        verify(bookRepository).existsByIsbn(request.getIsbn());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("書籍更新 - 正常系")
    void updateBookSuccess() {
        // Arrange
        Long bookId = 1L;
        UpdateBookRequest request = createUpdateBookRequest();
        Book existingBook = createSampleBook();
        AdminBookView expectedView = createSampleAdminBookViews().get(0);
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenReturn(existingBook);
        when(dtoMappingService.toAdminBookView(existingBook)).thenReturn(expectedView);
        
        // Act
        AdminBookView result = adminBookService.updateBook(bookId, request, 1L);
        
        // Assert
        assertThat(result).isEqualTo(expectedView);
        verify(bookRepository).findById(bookId);
        verify(bookRepository).save(existingBook);
        verify(dtoMappingService).toAdminBookView(existingBook);
    }

    @Test
    @DisplayName("書籍更新 - 存在しない書籍")
    void updateBookNotFound() {
        // Arrange
        Long bookId = 999L;
        UpdateBookRequest request = createUpdateBookRequest();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> adminBookService.updateBook(bookId, request, 1L))
            .isInstanceOf(BookNotFoundException.class);
        
        verify(bookRepository).findById(bookId);
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("書籍削除 - 正常系")
    void deleteBookSuccess() {
        // Arrange
        Long bookId = 1L;
        Book existingBook = createSampleBook();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenReturn(existingBook);
        
        // Act
        adminBookService.deleteBook(bookId, 1L);
        
        // Assert
        verify(bookRepository).findById(bookId);
        verify(bookRepository).save(existingBook);
        assertThat(existingBook.getStatus()).isEqualTo(Book.BookStatus.DELETED);
    }

    @Test
    @DisplayName("書籍削除 - 存在しない書籍")
    void deleteBookNotFound() {
        // Arrange
        Long bookId = 999L;
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> adminBookService.deleteBook(bookId, 1L))
            .isInstanceOf(BookNotFoundException.class);
        
        verify(bookRepository).findById(bookId);
        verify(bookRepository, never()).save(any());
    }

    // Helper methods
    private List<Book> createSampleBooks() {
        Book book1 = createSampleBook();
        Book book2 = Book.builder()
            .id(2L)
            .title("Spring Securityガイド")
            .author("佐藤太郎")
            .isbn("9784000000002")
            .price(new BigDecimal("2800"))
            .category("技術書")
            .description("Spring Securityの使い方を解説")
            .publisher("技術出版")
            .pages(320)
            .publicationDate(LocalDate.of(2024, 2, 1))
            .status(Book.BookStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return Arrays.asList(book1, book2);
    }

    private Book createSampleBook() {
        return Book.builder()
            .id(1L)
            .title("Spring Bootガイド")
            .author("山田花子")
            .isbn("9784000000001")
            .price(new BigDecimal("3200"))
            .category("技術書")
            .description("Spring Bootの基本から応用まで")
            .publisher("技術出版")
            .pages(450)
            .publicationDate(LocalDate.of(2024, 1, 15))
            .status(Book.BookStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private List<AdminBookView> createSampleAdminBookViews() {
        AdminBookView view1 = AdminBookView.builder()
            .id(1L)
            .title("Spring Bootガイド")
            .author("山田花子")
            .isbn("9784000000001")
            .price(new BigDecimal("3200"))
            .category("技術書")
            .status("ACTIVE")
            .stockQuantity(15)
            .soldQuantity(85)
            .rating(4.5)
            .reviewCount(25)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return Arrays.asList(view1);
    }

    private CreateBookRequest createBookRequest() {
        return CreateBookRequest.builder()
            .title("Spring Bootガイド")
            .author("山田花子")
            .isbn("9784000000001")
            .price(new BigDecimal("3200"))
            .category("技術書")
            .description("Spring Bootの基本から応用まで")
            .publisher("技術出版")
            .pages(450)
            .publicationDate(LocalDate.of(2024, 1, 15))
            .initialStock(100)
            .build();
    }

    private UpdateBookRequest createUpdateBookRequest() {
        return UpdateBookRequest.builder()
            .title("Spring Bootガイド 改訂版")
            .price(new BigDecimal("3500"))
            .description("Spring Bootの基本から応用まで 改訂版")
            .status("ACTIVE")
            .build();
    }
}