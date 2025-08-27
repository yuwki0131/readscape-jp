package jp.readscape.inventory.services;

import jp.readscape.inventory.domain.orders.model.Order;
import jp.readscape.inventory.domain.orders.repository.OrderRepository;
import jp.readscape.inventory.dto.admin.*;
import jp.readscape.inventory.exceptions.OrderNotFoundException;
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
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final DtoMappingService dtoMappingService;

    /**
     * 注文一覧取得（管理者向け）
     */
    public Page<AdminOrderView> getOrders(String status, int page, int size) {
        log.debug("Getting admin orders - status: {}, page: {}, size: {}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        
        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatus(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", status);
                orders = orderRepository.findAll(pageable);
            }
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(dtoMappingService::mapToAdminOrderView);
    }

    /**
     * 注文詳細取得
     */
    public AdminOrderDetail getOrderDetail(Long orderId) {
        log.debug("Getting order detail: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("注文が見つかりません: " + orderId));

        return dtoMappingService.mapToAdminOrderDetail(order);
    }

    /**
     * 注文ステータス更新
     */
    @Transactional
    public void updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long userId) {
        log.debug("Updating order status: {} -> {}", orderId, request.getStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("注文が見つかりません: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();
        Order.OrderStatus newStatus = request.getStatus();

        // ステータス遷移の妥当性をチェック
        validateStatusTransition(oldStatus, newStatus);

        // ステータス更新
        order.updateStatus(newStatus);
        
        // 備考があれば更新
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            order.setNotes(request.getNotes());
        }

        orderRepository.save(order);
        
        log.info("Order status updated: {} ({} -> {}) by user: {}", 
                 orderId, oldStatus, newStatus, userId);
    }

    /**
     * 処理待ち注文一覧取得
     */
    public List<PendingOrder> getPendingOrders() {
        log.debug("Getting pending orders");

        List<Order> pendingOrders = orderRepository.findPendingOrders();
        return pendingOrders.stream()
                .map(dtoMappingService::mapToPendingOrder)
                .collect(Collectors.toList());
    }

    /**
     * 確定待ち注文一覧取得
     */
    public List<AdminOrderView> getConfirmedOrders() {
        log.debug("Getting confirmed orders");

        List<Order> confirmedOrders = orderRepository.findConfirmedOrders();
        return confirmedOrders.stream()
                .map(dtoMappingService::mapToAdminOrderView)
                .collect(Collectors.toList());
    }

    /**
     * 注文検索
     */
    public Page<AdminOrderView> searchOrders(String keyword, int page, int size) {
        log.debug("Searching orders with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orders = orderRepository.findByKeyword(keyword, pageable);
        
        return orders.map(dtoMappingService::mapToAdminOrderView);
    }

    /**
     * 期間別注文取得
     */
    public Page<AdminOrderView> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate, 
                                                    String status, int page, int size) {
        log.debug("Getting orders by date range - start: {}, end: {}, status: {}", startDate, endDate, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        
        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatusAndOrderDateBetween(orderStatus, startDate, endDate, pageable);
            } catch (IllegalArgumentException e) {
                orders = orderRepository.findByOrderDateBetween(startDate, endDate, pageable);
            }
        } else {
            orders = orderRepository.findByOrderDateBetween(startDate, endDate, pageable);
        }

        return orders.map(dtoMappingService::mapToAdminOrderView);
    }

    /**
     * 遅延注文取得
     */
    public List<AdminOrderView> getDelayedOrders() {
        log.debug("Getting delayed orders");

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(3);
        List<Order> delayedOrders = orderRepository.findDelayedOrders(thresholdDate);
        
        return delayedOrders.stream()
                .map(dtoMappingService::mapToAdminOrderView)
                .collect(Collectors.toList());
    }

    /**
     * 注文統計取得
     */
    public OrderStatistics getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting order statistics - start: {}, end: {}", startDate, endDate);

        Object[] stats = orderRepository.getOrderStatistics(startDate, endDate);
        List<Object[]> statusStats = orderRepository.getOrderCountByStatus();

        if (stats == null || stats.length < 3) {
            return OrderStatistics.empty();
        }

        Long orderCount = ((Number) stats[0]).longValue();
        BigDecimal totalAmount = stats[1] != null ? new java.math.BigDecimal(stats[1].toString()) : java.math.BigDecimal.ZERO;
        Double averageAmount = stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0;

        return OrderStatistics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalOrders(orderCount)
                .totalAmount(totalAmount)
                .averageAmount(averageAmount)
                .statusBreakdown(buildStatusBreakdown(statusStats))
                .build();
    }

    /**
     * 今日の注文統計
     */
    public TodayOrderStatistics getTodayOrderStatistics() {
        log.debug("Getting today's order statistics");

        Long todayOrderCount = orderRepository.countTodaysOrders();
        
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        
        Object[] todayStats = orderRepository.getOrderStatistics(todayStart, todayEnd);
        
        return TodayOrderStatistics.builder()
                .todayOrderCount(todayOrderCount)
                .todayTotalAmount(todayStats != null && todayStats.length > 1 && todayStats[1] != null 
                    ? new java.math.BigDecimal(todayStats[1].toString()) 
                    : java.math.BigDecimal.ZERO)
                .build();
    }

    // プライベートメソッド

    /**
     * ステータス遷移の妥当性をチェック
     */
    private void validateStatusTransition(Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        // 基本的な遷移ルールをチェック
        switch (oldStatus) {
            case PENDING -> {
                if (newStatus != Order.OrderStatus.CONFIRMED && 
                    newStatus != Order.OrderStatus.CANCELLED) {
                    throw new IllegalArgumentException("PENDING状態からは CONFIRMED または CANCELLED にのみ変更可能です");
                }
            }
            case CONFIRMED -> {
                if (newStatus != Order.OrderStatus.PROCESSING && 
                    newStatus != Order.OrderStatus.CANCELLED) {
                    throw new IllegalArgumentException("CONFIRMED状態からは PROCESSING または CANCELLED にのみ変更可能です");
                }
            }
            case PROCESSING -> {
                if (newStatus != Order.OrderStatus.SHIPPED) {
                    throw new IllegalArgumentException("PROCESSING状態からは SHIPPED にのみ変更可能です");
                }
            }
            case SHIPPED -> {
                if (newStatus != Order.OrderStatus.DELIVERED) {
                    throw new IllegalArgumentException("SHIPPED状態からは DELIVERED にのみ変更可能です");
                }
            }
            case DELIVERED, CANCELLED -> {
                throw new IllegalArgumentException("完了またはキャンセルされた注文のステータスは変更できません");
            }
        }
    }

    /**
     * ステータス別注文数の分析データを構築
     */
    private java.util.Map<Order.OrderStatus, Long> buildStatusBreakdown(List<Object[]> statusStats) {
        return statusStats.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> (Order.OrderStatus) row[0],
                    row -> ((Number) row[1]).longValue()
                ));
    }

    // 内部クラス（統計用）

    @lombok.Builder
    @lombok.Data
    public static class OrderStatistics {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Long totalOrders;
        private java.math.BigDecimal totalAmount;
        private Double averageAmount;
        private java.util.Map<Order.OrderStatus, Long> statusBreakdown;

        public static OrderStatistics empty() {
            return OrderStatistics.builder()
                    .totalOrders(0L)
                    .totalAmount(java.math.BigDecimal.ZERO)
                    .averageAmount(0.0)
                    .statusBreakdown(new java.util.HashMap<>())
                    .build();
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class TodayOrderStatistics {
        private Long todayOrderCount;
        private java.math.BigDecimal todayTotalAmount;
    }
}