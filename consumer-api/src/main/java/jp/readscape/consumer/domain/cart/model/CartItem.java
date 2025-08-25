package jp.readscape.consumer.domain.cart.model;

import jakarta.persistence.*;
import jp.readscape.consumer.domain.books.model.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // 書籍の価格を単価として設定
        if (book != null && unitPrice == null) {
            unitPrice = book.getPrice();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ビジネスロジック

    /**
     * 小計を計算
     */
    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 小計の表示用フォーマット
     */
    public String getFormattedSubtotal() {
        return String.format("¥%,.0f", getSubtotal());
    }

    /**
     * 単価の表示用フォーマット
     */
    public String getFormattedUnitPrice() {
        return unitPrice != null ? String.format("¥%,d", unitPrice) : "¥0";
    }

    /**
     * 数量を増やす
     */
    public void increaseQuantity(int amount) {
        if (amount > 0) {
            this.quantity += amount;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 数量を減らす
     */
    public void decreaseQuantity(int amount) {
        if (amount > 0 && this.quantity > amount) {
            this.quantity -= amount;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 在庫チェック
     */
    public boolean isQuantityAvailable() {
        if (book == null || book.getStockQuantity() == null) {
            return false;
        }
        return quantity <= book.getStockQuantity();
    }

    /**
     * アイテムが有効かチェック
     */
    public boolean isValid() {
        return book != null && 
               quantity != null && quantity > 0 && 
               unitPrice != null && unitPrice >= 0 &&
               isQuantityAvailable();
    }

    /**
     * 書籍情報の更新（価格変更など）
     */
    public void updateBookInfo() {
        if (book != null) {
            this.unitPrice = book.getPrice();
            this.updatedAt = LocalDateTime.now();
        }
    }
}