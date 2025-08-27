package jp.readscape.inventory.services;

import jp.readscape.inventory.constants.BusinessConstants;
import jp.readscape.inventory.domain.books.model.Book;
import jp.readscape.inventory.domain.inventory.model.StockHistory;
import jp.readscape.inventory.domain.orders.model.Order;
import jp.readscape.inventory.domain.orders.model.OrderItem;
import jp.readscape.inventory.dto.admin.*;
import jp.readscape.inventory.dto.inventory.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTOマッピングを専門とするサービス
 * Single Responsibility Principleに従い、マッピング処理に特化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DtoMappingService {

    private final OrderStatusTransitionService statusTransitionService;

    // Book関連マッピング

    /**
     * BookからAdminBookViewへの変換
     */
    public AdminBookView mapToAdminBookView(Book book) {
        if (book == null) {
            return null;
        }

        AdminBookView view = AdminBookView.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .price(book.getPrice())
                .stockQuantity(book.getStockQuantity())
                .lowStockThreshold(book.getLowStockThreshold())
                .category(book.getCategory())
                .publisher(book.getPublisher())
                .averageRating(book.getAverageRating())
                .reviewCount(book.getReviewCount())
                .status(book.getStatus())
                .isLowStock(book.isLowStock())
                .isOutOfStock(book.isOutOfStock())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();

        // 表示用フィールドを設定
        view.setStockStatus(determineStockStatus(book));
        view.setFormattedPrice(formatPrice(book.getPrice()));

        return view;
    }

    /**
     * BookからInventoryItemへの変換
     */
    public InventoryItem mapToInventoryItem(Book book) {
        if (book == null) {
            return null;
        }

        InventoryItem item = InventoryItem.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .stockQuantity(book.getStockQuantity())
                .lowStockThreshold(book.getLowStockThreshold())
                .isLowStock(book.isLowStock())
                .isOutOfStock(book.isOutOfStock())
                .status(book.getStatus())
                .lastUpdatedAt(book.getUpdatedAt())
                .build();

        item.setStockStatus(determineStockStatus(book));
        return item;
    }

    /**
     * BookからLowStockItemへの変換
     */
    public LowStockItem mapToLowStockItem(Book book) {
        if (book == null) {
            return null;
        }

        LowStockItem item = LowStockItem.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .stockQuantity(book.getStockQuantity())
                .lowStockThreshold(book.getLowStockThreshold())
                .build();

        // 計算フィールドを設定
        item.setShortfallQuantity(calculateShortfall(book));
        item.setAlertLevel(determineAlertLevel(book));
        item.setRecommendedOrderQuantity(calculateRecommendedOrderQuantity(book));

        return item;
    }

    /**
     * BookからCreateBookResponseへの変換
     */
    public CreateBookResponse mapToCreateBookResponse(Book book) {
        if (book == null) {
            return null;
        }

        return CreateBookResponse.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .createdAt(book.getCreatedAt())
                .message("書籍が正常に作成されました")
                .build();
    }

    // Order関連マッピング

    /**
     * OrderからAdminOrderViewへの変換
     */
    public AdminOrderView mapToAdminOrderView(Order order) {
        if (order == null) {
            return null;
        }

        AdminOrderView view = AdminOrderView.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .userEmail(order.getUser().getEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .shippingAddress(order.getShippingAddress())
                .shippingPhone(order.getShippingPhone())
                .paymentMethod(order.getPaymentMethod())
                .orderDate(order.getOrderDate())
                .shippedDate(order.getShippedDate())
                .deliveredDate(order.getDeliveredDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();

        // 表示用フィールドを設定
        view.setStatusDisplayName(statusTransitionService.getDisplayName(order.getStatus()));
        view.setFormattedTotalAmount(formatBigDecimalAmount(order.getTotalAmount()));
        view.setUrgent(isUrgentOrder(order));
        view.setCanProcess(statusTransitionService.isCancellable(order.getStatus()) || 
                          order.getStatus() == Order.OrderStatus.CONFIRMED);

        return view;
    }

    /**
     * OrderからPendingOrderへの変換
     */
    public PendingOrder mapToPendingOrder(Order order) {
        if (order == null) {
            return null;
        }

        PendingOrder pending = PendingOrder.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .username(order.getUser().getUsername())
                .userEmail(order.getUser().getEmail())
                .totalAmount(order.getTotalAmount())
                .formattedTotalAmount(formatBigDecimalAmount(order.getTotalAmount()))
                .itemCount(order.getItems().size())
                .orderDate(order.getOrderDate())
                .build();

        // 経過時間を計算
        long elapsedHours = ChronoUnit.HOURS.between(order.getOrderDate(), LocalDateTime.now());
        pending.setHoursElapsed(elapsedHours);

        // 優先度を決定
        pending.setPriority(determinePriority(elapsedHours));

        // 配送先の都道府県を抽出
        pending.setShippingPrefecture(extractPrefecture(order.getShippingAddress()));

        return pending;
    }

    // StockHistory関連マッピング

    /**
     * StockHistoryからStockHistoryResponseへの変換
     */
    public StockHistoryResponse mapToStockHistoryResponse(StockHistory history) {
        if (history == null) {
            return null;
        }

        StockHistoryResponse response = StockHistoryResponse.builder()
                .id(history.getId())
                .bookId(history.getBook().getId())
                .bookTitle(history.getBook().getTitle())
                .userId(history.getUser().getId())
                .username(history.getUser().getUsername())
                .type(history.getType())
                .quantityChange(history.getQuantityChange())
                .quantityBefore(history.getQuantityBefore())
                .quantityAfter(history.getQuantityAfter())
                .reason(history.getReason())
                .referenceNumber(history.getReferenceNumber())
                .createdAt(history.getCreatedAt())
                .build();

        // 表示用フィールドを設定
        response.setTypeDisplayName(getStockChangeTypeDisplayName(history.getType()));
        response.setFormattedChange(history.getFormattedChange());

        return response;
    }

    /**
     * OrderからAdminOrderDetailへの変換
     */
    public AdminOrderDetail mapToAdminOrderDetail(Order order) {
        if (order == null) {
            return null;
        }

        AdminOrderDetail detail = AdminOrderDetail.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .user(AdminOrderDetail.UserInfo.builder()
                        .id(order.getUser().getId())
                        .username(order.getUser().getUsername())
                        .email(order.getUser().getEmail())
                        .fullName(order.getUser().getFullName())
                        .build())
                .status(order.getStatus())
                .statusDisplayName(statusTransitionService.getDisplayName(order.getStatus()))
                .totalAmount(order.getTotalAmount())
                .formattedTotalAmount(formatBigDecimalAmount(order.getTotalAmount()))
                .items(order.getItems().stream()
                        .map(this::mapToOrderItemInfo)
                        .collect(Collectors.toList()))
                .shipping(AdminOrderDetail.ShippingInfo.builder()
                        .address(order.getShippingAddress())
                        .phone(order.getShippingPhone())
                        .shippedDate(order.getShippedDate())
                        .deliveredDate(order.getDeliveredDate())
                        .build())
                .payment(AdminOrderDetail.PaymentInfo.builder()
                        .method(order.getPaymentMethod())
                        .status("完了") // 簡略化
                        .build())
                .notes(order.getNotes())
                .orderDate(order.getOrderDate())
                .shippedDate(order.getShippedDate())
                .deliveredDate(order.getDeliveredDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .cancellable(statusTransitionService.isCancellable(order.getStatus()))
                .build();

        return detail;
    }

    /**
     * OrderItemからOrderItemInfoへの変換
     */
    private AdminOrderDetail.OrderItemInfo mapToOrderItemInfo(OrderItem item) {
        return AdminOrderDetail.OrderItemInfo.builder()
                .id(item.getId())
                .bookId(item.getBook().getId())
                .bookTitle(item.getBookTitle())
                .bookAuthor(item.getBookAuthor())
                .bookIsbn(item.getBookIsbn())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .formattedUnitPrice(item.getFormattedUnitPrice())
                .formattedSubtotal(item.getFormattedSubtotal())
                .build();
    }

    // プライベートヘルパーメソッド

    private String determineStockStatus(Book book) {
        if (book.isOutOfStock()) {
            return "在庫切れ";
        } else if (book.isLowStock()) {
            return "低在庫";
        } else {
            return "正常";
        }
    }

    private String formatPrice(Integer price) {
        if (price == null) {
            return "¥0";
        }
        return String.format("¥%,d", price);
    }

    private String formatBigDecimalAmount(BigDecimal amount) {
        if (amount == null) {
            return "¥0";
        }
        return String.format("¥%,.0f", amount);
    }

    private int calculateShortfall(Book book) {
        int currentStock = book.getStockQuantity() != null ? book.getStockQuantity() : 0;
        int threshold = book.getLowStockThreshold() != null ? book.getLowStockThreshold() : BusinessConstants.DEFAULT_LOW_STOCK_THRESHOLD;
        return Math.max(0, threshold - currentStock);
    }

    private LowStockItem.AlertLevel determineAlertLevel(Book book) {
        Integer stock = book.getStockQuantity() != null ? book.getStockQuantity() : 0;
        Integer threshold = book.getLowStockThreshold() != null ? book.getLowStockThreshold() : BusinessConstants.DEFAULT_LOW_STOCK_THRESHOLD;

        if (stock <= 0) {
            return LowStockItem.AlertLevel.CRITICAL;
        } else if (stock <= threshold / BusinessConstants.ALERT_LEVEL_DIVISOR) {
            return LowStockItem.AlertLevel.HIGH;
        } else if (stock <= threshold) {
            return LowStockItem.AlertLevel.MEDIUM;
        } else {
            return LowStockItem.AlertLevel.LOW;
        }
    }

    private int calculateRecommendedOrderQuantity(Book book) {
        int currentStock = book.getStockQuantity() != null ? book.getStockQuantity() : 0;
        int threshold = book.getLowStockThreshold() != null ? book.getLowStockThreshold() : BusinessConstants.DEFAULT_LOW_STOCK_THRESHOLD;
        int recommendedStock = threshold * BusinessConstants.RECOMMENDED_ORDER_MULTIPLIER;
        return Math.max(0, recommendedStock - currentStock);
    }

    private boolean isUrgentOrder(Order order) {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(BusinessConstants.URGENT_ORDER_THRESHOLD_DAYS);
        return order.getOrderDate().isBefore(thresholdDate) && statusTransitionService.isActive(order.getStatus());
    }

    private PendingOrder.Priority determinePriority(long hoursElapsed) {
        if (hoursElapsed >= BusinessConstants.HIGH_PRIORITY_THRESHOLD_HOURS) {
            return PendingOrder.Priority.CRITICAL;
        } else if (hoursElapsed >= BusinessConstants.MEDIUM_PRIORITY_THRESHOLD_HOURS) {
            return PendingOrder.Priority.HIGH;
        } else if (hoursElapsed >= BusinessConstants.LOW_PRIORITY_THRESHOLD_HOURS) {
            return PendingOrder.Priority.MEDIUM;
        } else {
            return PendingOrder.Priority.LOW;
        }
    }

    private String extractPrefecture(String address) {
        // 簡略化された実装（実際のプロジェクトでは外部ライブラリを使用することを推奨）
        if (address == null || address.isEmpty()) {
            return "不明";
        }
        
        String[] prefectures = {
            "北海道", "青森県", "岩手県", "宮城県", "秋田県", "山形県", "福島県",
            "茨城県", "栃木県", "群馬県", "埼玉県", "千葉県", "東京都", "神奈川県",
            "新潟県", "富山県", "石川県", "福井県", "山梨県", "長野県", "岐阜県",
            "静岡県", "愛知県", "三重県", "滋賀県", "京都府", "大阪府", "兵庫県",
            "奈良県", "和歌山県", "鳥取県", "島根県", "岡山県", "広島県", "山口県",
            "徳島県", "香川県", "愛媛県", "高知県", "福岡県", "佐賀県", "長崎県",
            "熊本県", "大分県", "宮崎県", "鹿児島県", "沖縄県"
        };
        
        for (String prefecture : prefectures) {
            if (address.contains(prefecture)) {
                return prefecture;
            }
        }
        
        return "不明";
    }

    private String getStockChangeTypeDisplayName(StockHistory.StockChangeType type) {
        return switch (type) {
            case INBOUND -> "入荷";
            case OUTBOUND -> "出荷";
            case RETURN_FROM_CUSTOMER -> "返品";
            case DAMAGED -> "破損";
            case ADJUSTMENT_INCREASE -> "調整（増加）";
            case ADJUSTMENT_DECREASE -> "調整（減少）";
            case TRANSFER_IN -> "移動（受入）";
            case TRANSFER_OUT -> "移動（出庫）";
        };
    }
}