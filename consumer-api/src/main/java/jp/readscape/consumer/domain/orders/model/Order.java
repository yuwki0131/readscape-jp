package jp.readscape.consumer.domain.orders.model;

import jakarta.persistence.*;
import jp.readscape.consumer.constants.OrderConstants;
import jp.readscape.consumer.domain.users.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_phone")
    private String shippingPhone;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "shipped_date")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ビジネスロジック

    /**
     * 注文番号を生成
     */
    private String generateOrderNumber() {
        return OrderConstants.ORDER_NUMBER_PREFIX + System.currentTimeMillis();
    }

    /**
     * 注文アイテムを追加
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
        recalculateAmounts();
    }

    /**
     * 金額の再計算
     */
    public void recalculateAmounts() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.itemCount = items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 注文ステータスを更新
     */
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        switch (newStatus) {
            case SHIPPED:
                if (shippedDate == null) {
                    shippedDate = LocalDateTime.now();
                }
                break;
            case DELIVERED:
                if (deliveredDate == null) {
                    deliveredDate = LocalDateTime.now();
                }
                break;
        }
    }

    /**
     * 注文が編集可能かチェック
     */
    public boolean isEditable() {
        return status == OrderStatus.PENDING;
    }

    /**
     * 注文がキャンセル可能かチェック
     */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    /**
     * 合計金額の表示用フォーマット
     */
    public String getFormattedTotalAmount() {
        return totalAmount != null ? String.format("¥%,.0f", totalAmount) : "¥0";
    }

    /**
     * 注文日の表示用フォーマット
     */
    public String getFormattedOrderDate() {
        return orderDate != null ? orderDate.toString() : "";
    }

    /**
     * 配送予定日を取得（注文日から営業日後と仮定）
     */
    public LocalDateTime getEstimatedDeliveryDate() {
        return orderDate != null ? orderDate.plusDays(OrderConstants.CANCELLATION_PERIOD_DAYS) : null;
    }

    /**
     * 注文ステータスの日本語表示
     */
    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "不明";
    }

    // 注文ステータス列挙型
    public enum OrderStatus {
        PENDING("注文受付中"),
        CONFIRMED("注文確定"),
        PROCESSING("処理中"),
        SHIPPED("配送中"),
        DELIVERED("配送完了"),
        CANCELLED("キャンセル済み");

        private final String displayName;

        OrderStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}