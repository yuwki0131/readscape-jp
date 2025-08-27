package jp.readscape.inventory.domain.orders.repository;

import jp.readscape.inventory.domain.orders.model.Order;
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
     * ステータス別注文検索
     */
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    /**
     * ユーザー別注文検索
     */
    Page<Order> findByUserIdOrderByOrderDateDesc(Long userId, Pageable pageable);

    /**
     * 注文番号で検索
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 期間別注文検索
     */
    Page<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * ステータスと期間での注文検索
     */
    Page<Order> findByStatusAndOrderDateBetween(
            Order.OrderStatus status, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable);

    /**
     * 処理待ち注文（PENDING状態）
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' ORDER BY o.orderDate ASC")
    List<Order> findPendingOrders();

    /**
     * 確定待ち注文（CONFIRMED状態）
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'CONFIRMED' ORDER BY o.orderDate ASC")
    List<Order> findConfirmedOrders();

    /**
     * 今日の注文件数
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE")
    Long countTodaysOrders();

    /**
     * 期間内の注文統計
     */
    @Query("SELECT COUNT(o), SUM(o.totalAmount), AVG(o.totalAmount) FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status != 'CANCELLED'")
    Object[] getOrderStatistics(@Param("startDate") LocalDateTime startDate, 
                               @Param("endDate") LocalDateTime endDate);

    /**
     * ステータス別注文数
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderCountByStatus();

    /**
     * 月別売上統計
     */
    @Query("SELECT YEAR(o.orderDate), MONTH(o.orderDate), COUNT(o), SUM(o.totalAmount) " +
           "FROM Order o WHERE o.status != 'CANCELLED' " +
           "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) " +
           "ORDER BY YEAR(o.orderDate) DESC, MONTH(o.orderDate) DESC")
    List<Object[]> getMonthlySalesStatistics();

    /**
     * 最近の注文
     */
    List<Order> findTop10ByOrderByOrderDateDesc();

    /**
     * キーワード検索（注文番号、ユーザー名、配送先）
     */
    @Query("SELECT o FROM Order o JOIN o.user u WHERE " +
           "o.orderNumber ILIKE %:keyword% OR " +
           "u.username ILIKE %:keyword% OR " +
           "u.email ILIKE %:keyword% OR " +
           "o.shippingAddress ILIKE %:keyword%")
    Page<Order> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 遅延注文（3日以上PROCESSING状態）
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PROCESSING' " +
           "AND o.orderDate < :thresholdDate ORDER BY o.orderDate ASC")
    List<Order> findDelayedOrders(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * ユーザーの注文統計
     */
    @Query("SELECT COUNT(o), SUM(o.totalAmount) FROM Order o " +
           "WHERE o.user.id = :userId AND o.status != 'CANCELLED'")
    Object[] getUserOrderStatistics(@Param("userId") Long userId);
}