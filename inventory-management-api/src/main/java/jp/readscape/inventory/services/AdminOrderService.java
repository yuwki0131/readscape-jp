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
     * 注文一覧取得（管理者向け）- ソート対応
     */
    public Page<AdminOrderView> getOrders(String status, int page, int size, String sortBy, String sortDir) {
        log.debug("Getting admin orders - status: {}, page: {}, size: {}, sortBy: {}, sortDir: {}", 
                 status, page, size, sortBy, sortDir);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
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
     * 注文一覧取得（後方互換用）
     */
    public Page<AdminOrderView> getOrders(String status, int page, int size) {
        return getOrders(status, page, size, "orderDate", "desc");
    }

    /**
     * 注文詳細取得
     */
    public AdminOrderDetail getOrderById(Long orderId) {
        log.debug("Getting order detail: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("注文が見つかりません: " + orderId));

        return dtoMappingService.mapToAdminOrderDetail(order);
    }

    /**
     * 注文詳細取得（後方互換用）
     */
    public AdminOrderDetail getOrderDetail(Long orderId) {
        return getOrderById(orderId);
    }

    /**
     * 注文ステータス更新（UpdateOrderStatusRequest使用）
     */
    @Transactional
    public void updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.debug("Updating order status: {} -> {}", orderId, request.getNewStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("注文が見つかりません: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(request.getNewStatus().toUpperCase());

        // ステータス遷移の妥当性をチェック
        validateStatusTransition(oldStatus, newStatus);

        // ステータス更新
        order.updateStatus(newStatus);
        
        // 備考があれば更新
        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            order.setNotes(request.getReason());
        }

        orderRepository.save(order);
        
        log.info("Order status updated: {} ({} -> {})", orderId, oldStatus, newStatus);
    }

    /**
     * 注文ステータス更新（レガシーメソッド）
     */
    @Transactional
    public void updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long userId) {
        updateOrderStatus(orderId, request);
    }

    /**
     * 処理待ち注文一覧取得（制限付き）
     */
    public List<PendingOrder> getPendingOrders(int limit) {
        log.debug("Getting pending orders - limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("orderDate").ascending());
        Page<Order> pendingOrders = orderRepository.findByStatusIn(
            List.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED), pageable);
        
        return pendingOrders.getContent().stream()
                .map(dtoMappingService::mapToPendingOrder)
                .collect(Collectors.toList());
    }

    /**
     * 処理待ち注文一覧取得（制限なし）
     */
    public List<PendingOrder> getPendingOrders() {
        return getPendingOrders(100); // デフォルト100件
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
    public Page<AdminOrderView> searchOrders(String query, int page, int size) {
        log.debug("Searching orders with query: {}", query);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        
        // 注文番号、顧客名、メールアドレスで検索
        Page<Order> orders = orderRepository.findByOrderNumberContainingIgnoreCaseOrUser_UsernameContainingIgnoreCaseOrUser_EmailContainingIgnoreCase(
            query, query, query, pageable);
        
        return orders.map(dtoMappingService::mapToAdminOrderView);
    }

    /**
     * レガシー検索メソッド
     */
    public Page<AdminOrderView> searchOrders(String keyword, int page, int size) {
        return searchOrders(keyword, page, size);
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
     * 注文キャンセル
     */
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        log.debug("Cancelling order: {} with reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("注文が見つかりません: " + orderId));

        // キャンセル可能な状態かチェック
        if (order.getStatus() == Order.OrderStatus.SHIPPED || 
            order.getStatus() == Order.OrderStatus.DELIVERED ||
            order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("この注文はキャンセルできません。現在のステータス: " + order.getStatus());
        }

        // ステータス更新
        order.updateStatus(Order.OrderStatus.CANCELLED);
        
        // キャンセル理由を記録
        if (reason != null && !reason.trim().isEmpty()) {
            String currentNotes = order.getNotes() != null ? order.getNotes() : "";
            order.setNotes(currentNotes + "\n[キャンセル理由] " + reason);
        }

        orderRepository.save(order);
        log.info("Order cancelled: {}", orderId);
    }

    /**
     * 注文統計取得（日数指定）
     */
    public OrderStatistics getOrderStatistics(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return getOrderStatistics(startDate, endDate);
    }

    /**
     * 注文統計取得（期間指定）
     */
    public OrderStatistics getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting order statistics - start: {}, end: {}", startDate, endDate);

        // 簡略化された統計計算（実際のRepositoryメソッドが複雑な場合の代替）
        Page<Order> ordersInRange = orderRepository.findByOrderDateBetween(
            startDate, endDate, PageRequest.of(0, Integer.MAX_VALUE));
        
        long totalOrders = ordersInRange.getTotalElements();
        java.math.BigDecimal totalAmount = ordersInRange.getContent().stream()
                .map(order -> order.getTotalAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        double averageAmount = totalOrders > 0 ? totalAmount.divide(
            java.math.BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP).doubleValue() : 0.0;

        // ステータス別集計
        java.util.Map<Order.OrderStatus, Long> statusBreakdown = ordersInRange.getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Order::getStatus, 
                    java.util.stream.Collectors.counting()));

        return OrderStatistics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalOrders(totalOrders)
                .totalAmount(totalAmount)
                .averageAmount(averageAmount)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    /**
     * 配送ラベルデータ取得
     */
    public ShippingLabelData getShippingLabelData(Long orderId) {
        log.debug("Getting shipping label data for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("注文が見つかりません: " + orderId));

        if (order.getStatus() != Order.OrderStatus.CONFIRMED && 
            order.getStatus() != Order.OrderStatus.PROCESSING && 
            order.getStatus() != Order.OrderStatus.SHIPPED) {
            throw new IllegalStateException("配送ラベルを生成できない注文ステータスです: " + order.getStatus());
        }

        return ShippingLabelData.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getUser().getUsername())
                .customerEmail(order.getUser().getEmail())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .items(order.getOrderItems().stream()
                        .map(item -> ShippingLabelData.ShippingItem.builder()
                                .bookTitle(item.getBook().getTitle())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .collect(Collectors.toList()))
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

    @lombok.Builder
    @lombok.Data
    public static class ShippingLabelData {
        private Long orderId;
        private String orderNumber;
        private String customerName;
        private String customerEmail;
        private String shippingAddress;
        private java.math.BigDecimal totalAmount;
        private LocalDateTime orderDate;
        private List<ShippingItem> items;

        @lombok.Builder
        @lombok.Data
        public static class ShippingItem {
            private String bookTitle;
            private Integer quantity;
            private java.math.BigDecimal unitPrice;
        }
    }
}