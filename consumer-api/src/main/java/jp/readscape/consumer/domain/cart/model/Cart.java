package jp.readscape.consumer.domain.cart.model;

import jakarta.persistence.*;
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
@Table(name = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

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
     * カートアイテムを追加
     */
    public void addItem(CartItem item) {
        // 既存のアイテムがあるかチェック
        CartItem existingItem = findItemByBookId(item.getBook().getId());
        if (existingItem != null) {
            // 既存アイテムの数量を更新
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
        } else {
            // 新しいアイテムを追加
            item.setCart(this);
            items.add(item);
        }
        updatedAt = LocalDateTime.now();
    }

    /**
     * カートアイテムを削除
     */
    public void removeItem(Long bookId) {
        items.removeIf(item -> item.getBook().getId().equals(bookId));
        updatedAt = LocalDateTime.now();
    }

    /**
     * アイテム数量を更新
     */
    public void updateItemQuantity(Long bookId, Integer quantity) {
        CartItem item = findItemByBookId(bookId);
        if (item != null) {
            if (quantity <= 0) {
                removeItem(bookId);
            } else {
                item.setQuantity(quantity);
                updatedAt = LocalDateTime.now();
            }
        }
    }

    /**
     * カートを空にする
     */
    public void clear() {
        items.clear();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 書籍IDでアイテムを検索
     */
    private CartItem findItemByBookId(Long bookId) {
        return items.stream()
                .filter(item -> item.getBook().getId().equals(bookId))
                .findFirst()
                .orElse(null);
    }

    /**
     * カート内アイテム数を取得
     */
    public int getTotalItemCount() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * カート合計金額を取得
     */
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * カートが空かどうか
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * カート内の重複をチェック
     */
    public boolean containsBook(Long bookId) {
        return findItemByBookId(bookId) != null;
    }

    /**
     * 合計金額の表示用フォーマット
     */
    public String getFormattedTotalAmount() {
        return String.format("¥%,.0f", getTotalAmount());
    }
}