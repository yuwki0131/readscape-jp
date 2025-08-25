package jp.readscape.consumer.domain.orders.repository;

import jp.readscape.consumer.domain.orders.model.Order;
import jp.readscape.consumer.domain.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 注文番号で検索
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * ユーザーIDで注文一覧を取得
     */
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    /**
     * ユーザーで注文一覧を取得（ページング）
     */
    Page<Order> findByUserOrderByOrderDateDesc(User user, Pageable pageable);

    /**
     * ユーザーIDと注文IDで検索（認可チェック用）
     */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * 注文ステータスで検索
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * 注文日範囲で検索
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * ユーザーの注文統計
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countOrdersByUserId(@Param("userId") Long userId);

    /**
     * ユーザーの注文総額
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId")
    Double getTotalAmountByUserId(@Param("userId") Long userId);

    /**
     * 最近の注文を取得
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 特定のステータスの注文数を取得
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countOrdersByStatus(@Param("status") Order.OrderStatus status);

    /**
     * 配送待ちの注文を取得
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('CONFIRMED', 'PROCESSING') ORDER BY o.orderDate ASC")
    List<Order> findPendingShipmentOrders();

    /**
     * 配送期限を過ぎた注文を取得
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'SHIPPED' AND o.shippedDate < :cutoffDate")
    List<Order> findOverdueOrders(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 特定の書籍を含む注文を検索
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items oi WHERE oi.book.id = :bookId")
    List<Order> findOrdersByBookId(@Param("bookId") Long bookId);

    /**
     * 月別注文統計
     */
    @Query("SELECT EXTRACT(YEAR FROM o.orderDate) as year, EXTRACT(MONTH FROM o.orderDate) as month, COUNT(o) as count " +
           "FROM Order o WHERE o.orderDate >= :startDate " +
           "GROUP BY EXTRACT(YEAR FROM o.orderDate), EXTRACT(MONTH FROM o.orderDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyOrderStatistics(@Param("startDate") LocalDateTime startDate);
}