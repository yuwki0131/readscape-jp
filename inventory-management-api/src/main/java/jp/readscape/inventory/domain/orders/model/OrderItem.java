package jp.readscape.inventory.domain.orders.model;

import jakarta.persistence.*;
import jp.readscape.inventory.domain.books.model.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "book_title", nullable = false)
    private String bookTitle;

    @Column(name = "book_author")
    private String bookAuthor;

    @Column(name = "book_isbn")
    private String bookIsbn;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        // 書籍情報を保存（履歴保持のため）
        if (book != null) {
            if (bookTitle == null) bookTitle = book.getTitle();
            if (bookAuthor == null) bookAuthor = book.getAuthor();
            if (bookIsbn == null) bookIsbn = book.getIsbn();
            if (unitPrice == null) unitPrice = book.getPrice();
        }
        
        // 小計計算
        if (subtotal == null) {
            calculateSubtotal();
        }
    }

    // ビジネスロジック

    /**
     * 小計計算
     */
    public void calculateSubtotal() {
        if (unitPrice != null && quantity != null) {
            this.subtotal = BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    /**
     * 小計取得（計算結果を返す）
     */
    public BigDecimal getSubtotal() {
        if (subtotal == null) {
            calculateSubtotal();
        }
        return subtotal;
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
}