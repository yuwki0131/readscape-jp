package jp.readscape.inventory.domain.orders.model;

import jakarta.persistence.*;
import jp.readscape.inventory.domain.users.model.User;
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

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "shipping_phone")
    private String shippingPhone;

    @Column(name = "payment_method")
    private String paymentMethod;

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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        orderDate = LocalDateTime.now();
        
        if (orderNumber == null) {
            generateOrderNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ビジネスロジック

    /**
     * 注文アイテムを追加
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    /**
     * 合計金額を再計算
     */
    public void recalculateAmounts() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ステータス更新
     */
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        // ステータス変更に応じて日時を更新
        switch (newStatus) {
            case SHIPPED -> this.shippedDate = LocalDateTime.now();
            case DELIVERED -> this.deliveredDate = LocalDateTime.now();
        }
    }

    /**
     * キャンセル可能かチェック
     */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    /**
     * 処理済みかチェック
     */
    public boolean isProcessed() {
        return status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED;
    }

    /**
     * アクティブな注文かチェック
     */
    public boolean isActive() {
        return status != OrderStatus.CANCELLED;
    }

    /**
     * 注文番号を生成
     */
    private void generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%04d%02d%02d%02d%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute());
        this.orderNumber = "ORD-" + timestamp + "-" + String.format("%04d", (int)(Math.random() * 10000));
    }

    public enum OrderStatus {
        PENDING,    // 注文受付
        CONFIRMED,  // 注文確定
        PROCESSING, // 処理中
        SHIPPED,    // 配送中
        DELIVERED,  // 配送完了
        CANCELLED   // キャンセル
    }
}