package jp.readscape.inventory.services;

import jp.readscape.inventory.constants.BusinessConstants;
import jp.readscape.inventory.domain.books.model.Book;
import jp.readscape.inventory.domain.books.repository.BookRepository;
import jp.readscape.inventory.domain.inventory.model.StockHistory;
import jp.readscape.inventory.domain.inventory.repository.StockHistoryRepository;
import jp.readscape.inventory.domain.users.model.User;
import jp.readscape.inventory.dto.inventory.*;
import jp.readscape.inventory.exceptions.BookNotFoundException;
import jp.readscape.inventory.exceptions.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final BookRepository bookRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final DtoMappingService dtoMappingService;

    /**
     * 在庫一覧取得
     */
    public List<InventoryItem> getInventory() {
        log.debug("Getting inventory list");

        List<Book> books = bookRepository.findAll(Sort.by("updatedAt").descending());
        return books.stream()
                .map(dtoMappingService::mapToInventoryItem)
                .collect(Collectors.toList());
    }

    /**
     * 在庫更新
     */
    @Transactional
    public void updateStock(Long bookId, StockUpdateRequest request, Long userId) {
        log.debug("Updating stock for book: {}, type: {}, quantity: {}", bookId, request.getType(), request.getQuantity());

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("書籍が見つかりません: " + bookId));

        Integer currentStock = book.getStockQuantity() != null ? book.getStockQuantity() : 0;
        Integer changeAmount = request.getNormalizedQuantity();
        Integer newStock = currentStock + changeAmount;

        // 出庫系操作で在庫不足の場合はエラー
        if (request.isOutboundOperation() && newStock < 0) {
            throw new InsufficientStockException(
                String.format("在庫が不足しています。現在在庫: %d, 要求数量: %d", 
                    currentStock, Math.abs(changeAmount))
            );
        }

        // 在庫を更新
        book.setStock(Math.max(0, newStock));
        bookRepository.save(book);

        // 在庫履歴を記録
        recordStockHistory(book, userId, request.getType(), changeAmount, 
                          currentStock, book.getStockQuantity(), request.getReason(), 
                          request.getReferenceNumber());

        log.info("Stock updated successfully for book {}: {} -> {}", bookId, currentStock, book.getStockQuantity());
    }

    /**
     * 低在庫商品一覧取得
     */
    public List<LowStockItem> getLowStockItems() {
        log.debug("Getting low stock items");

        List<Book> lowStockBooks = bookRepository.findLowStockBooks();
        return lowStockBooks.stream()
                .map(dtoMappingService::mapToLowStockItem)
                .collect(Collectors.toList());
    }

    /**
     * 在庫履歴取得
     */
    public Page<StockHistoryResponse> getStockHistory(Long bookId, LocalDateTime startDate, 
                                                     LocalDateTime endDate, int page, int size) {
        log.debug("Getting stock history - bookId: {}, startDate: {}, endDate: {}", bookId, startDate, endDate);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<StockHistory> historyPage;
        if (bookId != null && startDate != null && endDate != null) {
            historyPage = stockHistoryRepository.findByBookIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                bookId, startDate, endDate, pageable);
        } else if (bookId != null) {
            historyPage = stockHistoryRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable);
        } else if (startDate != null && endDate != null) {
            historyPage = stockHistoryRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startDate, endDate, pageable);
        } else {
            historyPage = stockHistoryRepository.findAll(pageable);
        }

        return historyPage.map(dtoMappingService::mapToStockHistoryResponse);
    }

    /**
     * 書籍別在庫詳細取得
     */
    public InventoryItem getBookInventory(Long bookId) {
        log.debug("Getting book inventory: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("書籍が見つかりません: " + bookId));

        return dtoMappingService.mapToInventoryItem(book);
    }

    /**
     * 在庫統計取得
     */
    public InventoryStatistics getInventoryStatistics() {
        log.debug("Getting inventory statistics");

        Object[] stats = bookRepository.getStockStatistics();
        if (stats == null || stats.length < BusinessConstants.EXPECTED_STATS_COUNT) {
            return InventoryStatistics.empty();
        }

        Long totalBooks = ((Number) stats[0]).longValue();
        Long totalStock = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
        Double averageStock = stats[BusinessConstants.AVERAGE_STOCK_INDEX] != null ? ((Number) stats[BusinessConstants.AVERAGE_STOCK_INDEX]).doubleValue() : 0.0;

        List<Book> lowStockBooks = bookRepository.findLowStockBooks();
        List<Book> outOfStockBooks = bookRepository.findOutOfStockBooks();

        return InventoryStatistics.builder()
                .totalBooks(totalBooks)
                .totalStock(totalStock)
                .averageStock(averageStock)
                .lowStockCount(lowStockBooks.size())
                .outOfStockCount(outOfStockBooks.size())
                .build();
    }

    /**
     * 期間内在庫変動統計
     */
    public StockMovementStatistics getStockMovementStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting stock movement statistics - start: {}, end: {}", startDate, endDate);

        Integer totalInbound = stockHistoryRepository.getTotalInboundQuantity(startDate, endDate);
        Integer totalOutbound = stockHistoryRepository.getTotalOutboundQuantity(startDate, endDate);

        return StockMovementStatistics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalInbound(totalInbound != null ? totalInbound : 0)
                .totalOutbound(totalOutbound != null ? totalOutbound : 0)
                .netMovement((totalInbound != null ? totalInbound : 0) - (totalOutbound != null ? totalOutbound : 0))
                .build();
    }

    /**
     * 一括在庫調整
     */
    @Transactional
    public void bulkStockAdjustment(List<BulkStockUpdateRequest> requests, Long userId) {
        log.debug("Processing bulk stock adjustment for {} items", requests.size());

        for (BulkStockUpdateRequest request : requests) {
            try {
                StockUpdateRequest stockUpdate = StockUpdateRequest.builder()
                        .type(request.getType())
                        .quantity(request.getQuantity())
                        .reason(request.getReason())
                        .referenceNumber(request.getReferenceNumber())
                        .build();
                
                updateStock(request.getBookId(), stockUpdate, userId);
            } catch (Exception e) {
                log.error("Failed to update stock for book {}: {}", request.getBookId(), e.getMessage());
                // 個別エラーは記録するが処理は継続
            }
        }

        log.info("Bulk stock adjustment completed for user: {}", userId);
    }

    // プライベートメソッド

    /**
     * 在庫履歴を記録
     */
    private void recordStockHistory(Book book, Long userId, StockHistory.StockChangeType type,
                                   Integer quantityChange, Integer quantityBefore, Integer quantityAfter,
                                   String reason, String referenceNumber) {
        StockHistory history = StockHistory.builder()
                .book(book)
                .user(User.builder().id(userId).build()) // プロキシオブジェクト
                .type(type)
                .quantityChange(quantityChange)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .reason(reason)
                .referenceNumber(referenceNumber)
                .build();

        stockHistoryRepository.save(history);
        log.debug("Stock history recorded: book={}, type={}, change={}", book.getId(), type, quantityChange);
    }

    // 内部クラス（統計用）

    @lombok.Builder
    @lombok.Data
    public static class InventoryStatistics {
        private Long totalBooks;
        private Long totalStock;
        private Double averageStock;
        private Integer lowStockCount;
        private Integer outOfStockCount;

        public static InventoryStatistics empty() {
            return InventoryStatistics.builder()
                    .totalBooks(0L)
                    .totalStock(0L)
                    .averageStock(0.0)
                    .lowStockCount(0)
                    .outOfStockCount(0)
                    .build();
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class StockMovementStatistics {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer totalInbound;
        private Integer totalOutbound;
        private Integer netMovement;
    }

    @lombok.Builder
    @lombok.Data
    public static class BulkStockUpdateRequest {
        private Long bookId;
        private StockHistory.StockChangeType type;
        private Integer quantity;
        private String reason;
        private String referenceNumber;
    }
}