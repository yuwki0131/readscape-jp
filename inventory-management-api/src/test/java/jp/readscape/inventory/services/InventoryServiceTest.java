package jp.readscape.inventory.services;

import jp.readscape.inventory.domain.books.model.Book;
import jp.readscape.inventory.domain.books.repository.BookRepository;
import jp.readscape.inventory.domain.inventory.model.StockHistory;
import jp.readscape.inventory.domain.inventory.repository.StockHistoryRepository;
import jp.readscape.inventory.dto.inventory.InventoryItem;
import jp.readscape.inventory.dto.inventory.StockUpdateRequest;
import jp.readscape.inventory.dto.inventory.StockTransactionResponse;
import jp.readscape.inventory.exceptions.BookNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
@DisplayName("InventoryService Test")
class InventoryServiceTest {

    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private StockHistoryRepository stockHistoryRepository;
    
    @Mock
    private DtoMappingService dtoMappingService;
    
    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("在庫一覧取得 - 正常系")
    void getInventorySuccess() {
        // Arrange
        List<Book> books = createSampleBooks();
        List<InventoryItem> expectedItems = createSampleInventoryItems();
        
        when(bookRepository.findAll(any(Sort.class))).thenReturn(books);
        when(dtoMappingService.mapToInventoryItem(any(Book.class)))
            .thenReturn(expectedItems.get(0))
            .thenReturn(expectedItems.get(1));
        
        // Act
        List<InventoryItem> result = inventoryService.getInventory();
        
        // Assert
        assertThat(result).hasSize(2);
        verify(bookRepository).findAll(any(Sort.class));
        verify(dtoMappingService, times(2)).mapToInventoryItem(any(Book.class));
    }

    @Test
    @DisplayName("在庫更新 - INBOUND（入庫）正常系")
    void updateStockInboundSuccess() {
        // Arrange
        Long bookId = 1L;
        Long userId = 1L;
        Book book = createSampleBook();
        book.setStockQuantity(50);
        
        StockUpdateRequest request = StockUpdateRequest.builder()
            .type("INBOUND")
            .quantity(30)
            .reason("新規入庫")
            .notes("定期入庫処理")
            .build();
        
        StockHistory savedHistory = createSampleStockHistory(bookId, userId);
        StockTransactionResponse expectedResponse = createExpectedTransactionResponse();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(stockHistoryRepository.save(any(StockHistory.class))).thenReturn(savedHistory);
        when(dtoMappingService.toStockTransactionResponse(savedHistory)).thenReturn(expectedResponse);
        
        // Act
        StockTransactionResponse result = inventoryService.updateStock(bookId, request, userId);
        
        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(book.getStockQuantity()).isEqualTo(80); // 50 + 30
        verify(bookRepository).findById(bookId);
        verify(bookRepository).save(book);
        verify(stockHistoryRepository).save(any(StockHistory.class));
        verify(dtoMappingService).toStockTransactionResponse(savedHistory);
    }

    @Test
    @DisplayName("在庫更新 - OUTBOUND（出庫）正常系")
    void updateStockOutboundSuccess() {
        // Arrange
        Long bookId = 1L;
        Long userId = 1L;
        Book book = createSampleBook();
        book.setStockQuantity(50);
        
        StockUpdateRequest request = StockUpdateRequest.builder()
            .type("OUTBOUND")
            .quantity(20)
            .reason("販売出庫")
            .notes("注文処理")
            .build();
        
        StockHistory savedHistory = createSampleStockHistory(bookId, userId);
        StockTransactionResponse expectedResponse = createExpectedTransactionResponse();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(stockHistoryRepository.save(any(StockHistory.class))).thenReturn(savedHistory);
        when(dtoMappingService.toStockTransactionResponse(savedHistory)).thenReturn(expectedResponse);
        
        // Act
        StockTransactionResponse result = inventoryService.updateStock(bookId, request, userId);
        
        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(book.getStockQuantity()).isEqualTo(30); // 50 - 20
        verify(bookRepository).findById(bookId);
        verify(bookRepository).save(book);
        verify(stockHistoryRepository).save(any(StockHistory.class));
    }

    @Test
    @DisplayName("在庫更新 - ADJUSTMENT（調整）正常系")
    void updateStockAdjustmentSuccess() {
        // Arrange
        Long bookId = 1L;
        Long userId = 1L;
        Book book = createSampleBook();
        book.setStockQuantity(50);
        
        StockUpdateRequest request = StockUpdateRequest.builder()
            .type("ADJUSTMENT")
            .quantity(45) // 絶対値で設定
            .reason("在庫調整")
            .notes("棚卸し調整")
            .build();
        
        StockHistory savedHistory = createSampleStockHistory(bookId, userId);
        StockTransactionResponse expectedResponse = createExpectedTransactionResponse();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(stockHistoryRepository.save(any(StockHistory.class))).thenReturn(savedHistory);
        when(dtoMappingService.toStockTransactionResponse(savedHistory)).thenReturn(expectedResponse);
        
        // Act
        StockTransactionResponse result = inventoryService.updateStock(bookId, request, userId);
        
        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(book.getStockQuantity()).isEqualTo(45); // 調整後の値
        verify(bookRepository).findById(bookId);
        verify(bookRepository).save(book);
        verify(stockHistoryRepository).save(any(StockHistory.class));
    }

    @Test
    @DisplayName("在庫更新 - 存在しない書籍")
    void updateStockBookNotFound() {
        // Arrange
        Long bookId = 999L;
        Long userId = 1L;
        StockUpdateRequest request = StockUpdateRequest.builder()
            .type("INBOUND")
            .quantity(30)
            .reason("入庫")
            .build();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> inventoryService.updateStock(bookId, request, userId))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining("Book not found");
        
        verify(bookRepository).findById(bookId);
        verifyNoInteractions(stockHistoryRepository);
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("在庫更新 - 不正なトランザクションタイプ")
    void updateStockInvalidTransactionType() {
        // Arrange
        Long bookId = 1L;
        Long userId = 1L;
        Book book = createSampleBook();
        
        StockUpdateRequest request = StockUpdateRequest.builder()
            .type("INVALID_TYPE")
            .quantity(30)
            .reason("テスト")
            .build();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        
        // Act & Assert
        assertThatThrownBy(() -> inventoryService.updateStock(bookId, request, userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid transaction type");
        
        verify(bookRepository).findById(bookId);
        verify(bookRepository, never()).save(any());
        verifyNoInteractions(stockHistoryRepository);
    }

    @Test
    @DisplayName("在庫履歴取得 - 正常系")
    void getStockHistorySuccess() {
        // Arrange
        Long bookId = 1L;
        List<StockHistory> histories = createSampleStockHistories();
        Page<StockHistory> historyPage = new PageImpl<>(histories);
        
        when(stockHistoryRepository.findByBookIdOrderByCreatedAtDesc(eq(bookId), any()))
            .thenReturn(historyPage);
        when(dtoMappingService.toStockTransactionResponse(any(StockHistory.class)))
            .thenReturn(createExpectedTransactionResponse());
        
        // Act
        Page<StockTransactionResponse> result = inventoryService.getStockHistory(bookId, 0, 10);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(stockHistoryRepository).findByBookIdOrderByCreatedAtDesc(eq(bookId), any());
        verify(dtoMappingService, times(2)).toStockTransactionResponse(any(StockHistory.class));
    }

    @Test
    @DisplayName("低在庫商品取得 - 正常系")
    void getLowStockItemsSuccess() {
        // Arrange
        List<Book> lowStockBooks = createLowStockBooks();
        List<InventoryItem> expectedItems = createLowStockInventoryItems();
        
        when(bookRepository.findLowStockBooks()).thenReturn(lowStockBooks);
        when(dtoMappingService.mapToInventoryItem(any(Book.class)))
            .thenReturn(expectedItems.get(0));
        
        // Act
        List<InventoryItem> result = inventoryService.getLowStockItems();
        
        // Assert
        assertThat(result).hasSize(1);
        verify(bookRepository).findLowStockBooks();
        verify(dtoMappingService).mapToInventoryItem(any(Book.class));
    }

    // Helper methods
    private List<Book> createSampleBooks() {
        Book book1 = createSampleBook();
        Book book2 = Book.builder()
            .id(2L)
            .title("Spring Securityガイド")
            .author("佐藤太郎")
            .stockQuantity(25)
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
            .stockQuantity(50)
            .status(Book.BookStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private List<InventoryItem> createSampleInventoryItems() {
        InventoryItem item1 = InventoryItem.builder()
            .bookId(1L)
            .title("Spring Bootガイド")
            .currentStock(50)
            .minStockLevel(10)
            .isLowStock(false)
            .build();
        
        InventoryItem item2 = InventoryItem.builder()
            .bookId(2L)
            .title("Spring Securityガイド")
            .currentStock(25)
            .minStockLevel(10)
            .isLowStock(false)
            .build();
        
        return Arrays.asList(item1, item2);
    }

    private StockHistory createSampleStockHistory(Long bookId, Long userId) {
        return StockHistory.builder()
            .id(1L)
            .bookId(bookId)
            .transactionType(StockHistory.TransactionType.INBOUND)
            .quantity(30)
            .previousStock(50)
            .newStock(80)
            .reason("新規入庫")
            .processedBy(userId)
            .processedAt(LocalDateTime.now())
            .build();
    }

    private StockTransactionResponse createExpectedTransactionResponse() {
        return StockTransactionResponse.builder()
            .transactionId(1L)
            .bookId(1L)
            .type("INBOUND")
            .quantity(30)
            .previousStock(50)
            .newStock(80)
            .reason("新規入庫")
            .processedBy(1L)
            .processedAt(LocalDateTime.now())
            .build();
    }

    private List<StockHistory> createSampleStockHistories() {
        StockHistory history1 = createSampleStockHistory(1L, 1L);
        StockHistory history2 = StockHistory.builder()
            .id(2L)
            .bookId(1L)
            .transactionType(StockHistory.TransactionType.OUTBOUND)
            .quantity(10)
            .previousStock(80)
            .newStock(70)
            .reason("販売出庫")
            .processedBy(1L)
            .processedAt(LocalDateTime.now().minusDays(1))
            .build();
        
        return Arrays.asList(history1, history2);
    }

    private List<Book> createLowStockBooks() {
        Book lowStockBook = Book.builder()
            .id(1L)
            .title("在庫少商品")
            .stockQuantity(3)
            .minStockLevel(10)
            .build();
        
        return Arrays.asList(lowStockBook);
    }

    private List<InventoryItem> createLowStockInventoryItems() {
        InventoryItem item = InventoryItem.builder()
            .bookId(1L)
            .title("在庫少商品")
            .currentStock(3)
            .minStockLevel(10)
            .isLowStock(true)
            .urgency("HIGH")
            .build();
        
        return Arrays.asList(item);
    }
}