package jp.readscape.inventory.domain.books.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(unique = true)
    private String isbn;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 10;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;

    private String publisher;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @Column(name = "average_rating")
    private BigDecimal averageRating;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookStatus status = BookStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ビジネスロジック

    /**
     * 在庫が低在庫閾値を下回っているかチェック
     */
    public boolean isLowStock() {
        return stockQuantity != null && stockQuantity <= lowStockThreshold;
    }

    /**
     * 在庫切れかチェック
     */
    public boolean isOutOfStock() {
        return stockQuantity == null || stockQuantity <= 0;
    }

    /**
     * 在庫を増加
     */
    public void increaseStock(Integer quantity) {
        if (quantity > 0) {
            this.stockQuantity = (this.stockQuantity != null ? this.stockQuantity : 0) + quantity;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 在庫を減少
     */
    public void decreaseStock(Integer quantity) {
        if (quantity > 0 && this.stockQuantity != null) {
            this.stockQuantity = Math.max(0, this.stockQuantity - quantity);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 在庫を設定
     */
    public void setStock(Integer newStock) {
        this.stockQuantity = Math.max(0, newStock);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 書籍ステータス更新
     */
    public void updateStatus(BookStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 評価情報更新
     */
    public void updateRating(BigDecimal averageRating, Integer reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.updatedAt = LocalDateTime.now();
    }

    public enum BookStatus {
        ACTIVE, INACTIVE, DISCONTINUED, OUT_OF_PRINT
    }
}