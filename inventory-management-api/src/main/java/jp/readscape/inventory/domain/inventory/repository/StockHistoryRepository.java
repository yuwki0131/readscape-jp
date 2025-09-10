package jp.readscape.inventory.domain.inventory.repository;

import jp.readscape.inventory.domain.inventory.model.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    /**
     * 書籍IDで履歴検索
     */
    Page<StockHistory> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    /**
     * ユーザーIDで履歴検索
     */
    Page<StockHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 在庫変動タイプ別検索
     */
    Page<StockHistory> findByTypeOrderByCreatedAtDesc(StockHistory.StockChangeType type, Pageable pageable);

    /**
     * 日付範囲での検索
     */
    Page<StockHistory> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable);

    /**
     * 書籍と日付範囲での検索
     */
    Page<StockHistory> findByBookIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long bookId, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable);

    /**
     * 参照番号での検索（注文番号等）
     */
    List<StockHistory> findByReferenceNumberOrderByCreatedAtDesc(String referenceNumber);

    /**
     * 書籍の最新在庫履歴
     */
    @Query("SELECT sh FROM StockHistory sh WHERE sh.book.id = :bookId " +
           "ORDER BY sh.createdAt DESC")
    List<StockHistory> findLatestByBookId(@Param("bookId") Long bookId, Pageable pageable);

    /**
     * 期間内の入荷合計
     */
    @Query("SELECT COALESCE(SUM(sh.quantityChange), 0) FROM StockHistory sh " +
           "WHERE sh.type IN ('INBOUND', 'RETURN_FROM_CUSTOMER', 'ADJUSTMENT_INCREASE') " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate")
    Integer getTotalInboundQuantity(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * 期間内の出荷合計
     */
    @Query("SELECT COALESCE(SUM(ABS(sh.quantityChange)), 0) FROM StockHistory sh " +
           "WHERE sh.type IN ('OUTBOUND', 'DAMAGED', 'ADJUSTMENT_DECREASE') " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate")
    Integer getTotalOutboundQuantity(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * 書籍別期間内在庫変動統計
     */
    @Query("SELECT sh.book.id, sh.book.title, " +
           "SUM(CASE WHEN sh.quantityChange > 0 THEN sh.quantityChange ELSE 0 END) as inbound, " +
           "SUM(CASE WHEN sh.quantityChange < 0 THEN ABS(sh.quantityChange) ELSE 0 END) as outbound " +
           "FROM StockHistory sh " +
           "WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sh.book.id, sh.book.title " +
           "ORDER BY (inbound + outbound) DESC")
    List<Object[]> getBookStockMovementStats(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 最も活発な在庫変動があった書籍
     */
    @Query("SELECT sh.book.id, sh.book.title, COUNT(sh) as movement_count " +
           "FROM StockHistory sh " +
           "WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sh.book.id, sh.book.title " +
           "ORDER BY movement_count DESC")
    List<Object[]> getMostActiveBooks(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate, 
                                    Pageable pageable);
}