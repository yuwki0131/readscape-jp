package jp.readscape.inventory.domain.inventory.model;

import jakarta.persistence.*;
import jp.readscape.inventory.domain.books.model.Book;
import jp.readscape.inventory.domain.users.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockChangeType type;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    private String reason;

    @Column(name = "reference_number")
    private String referenceNumber; // 注文番号、入荷番号等

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ビジネスロジック

    public boolean isInbound() {
        return type == StockChangeType.INBOUND || 
               type == StockChangeType.RETURN_FROM_CUSTOMER ||
               type == StockChangeType.ADJUSTMENT_INCREASE;
    }

    public boolean isOutbound() {
        return type == StockChangeType.OUTBOUND || 
               type == StockChangeType.DAMAGED ||
               type == StockChangeType.ADJUSTMENT_DECREASE;
    }

    public String getFormattedChange() {
        String sign = quantityChange >= 0 ? "+" : "";
        return sign + quantityChange;
    }

    public enum StockChangeType {
        INBOUND,                // 入荷
        OUTBOUND,               // 出荷
        RETURN_FROM_CUSTOMER,   // 顧客からの返品
        DAMAGED,                // 破損
        ADJUSTMENT_INCREASE,    // 調整（増加）
        ADJUSTMENT_DECREASE,    // 調整（減少）
        TRANSFER_IN,            // 移動（受入）
        TRANSFER_OUT           // 移動（出庫）
    }
}