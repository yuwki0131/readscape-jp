package jp.readscape.consumer.domain.orders.model;

import jakarta.persistence.*;
import jp.readscape.consumer.domain.books.model.Book;
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

    @Column(name = "book_author", nullable = false)
    private String bookAuthor;

    @Column(name = "book_isbn")
    private String bookIsbn;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // 書籍情報をスナップショットとして保存
        if (book != null) {
            if (bookTitle == null) bookTitle = book.getTitle();
            if (bookAuthor == null) bookAuthor = book.getAuthor();
            if (bookIsbn == null) bookIsbn = book.getIsbn();
            if (unitPrice == null) unitPrice = book.getPrice();
        }
        
        // 小計を計算
        calculateSubtotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateSubtotal();
    }

    // ビジネスロジック

    /**
     * 小計を計算
     */
    public void calculateSubtotal() {
        if (unitPrice != null && quantity != null) {
            subtotal = BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(quantity));
        } else {
            subtotal = BigDecimal.ZERO;
        }
    }

    /**
     * 小計を取得（計算して返す）
     */
    public BigDecimal getSubtotal() {
        if (subtotal == null) {
            calculateSubtotal();
        }
        return subtotal != null ? subtotal : BigDecimal.ZERO;
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
     * 数量を設定し、小計を再計算
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 単価を設定し、小計を再計算
     */
    public void setUnitPrice(Integer unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * アイテムが有効かチェック
     */
    public boolean isValid() {
        return bookTitle != null && !bookTitle.isEmpty() &&
               bookAuthor != null && !bookAuthor.isEmpty() &&
               quantity != null && quantity > 0 &&
               unitPrice != null && unitPrice >= 0;
    }

    /**
     * 書籍情報の表示名を取得
     */
    public String getBookDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (bookTitle != null) {
            sb.append(bookTitle);
        }
        if (bookAuthor != null) {
            sb.append(" - ").append(bookAuthor);
        }
        return sb.toString();
    }

    /**
     * このアイテムの総重量を取得（配送計算用）
     * 仮定：1冊あたり300g
     */
    public int getTotalWeight() {
        return quantity != null ? quantity * 300 : 0; // グラム単位
    }

    /**
     * 注文アイテムの説明文を生成
     */
    public String getDescription() {
        return String.format("%s × %d冊", getBookDisplayName(), quantity != null ? quantity : 0);
    }
}