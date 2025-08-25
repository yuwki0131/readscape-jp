package jp.readscape.consumer.domain.reviews.model;

import jakarta.persistence.*;
import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.users.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"book_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "review_title")
    private String title;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_verified_purchase")
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

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
     * 評価が有効な範囲（1-5）かチェック
     */
    public boolean isValidRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }

    /**
     * レビューが有効かチェック
     */
    public boolean isValid() {
        return isValidRating() && 
               book != null && 
               user != null;
    }

    /**
     * 評価の星表示を取得
     */
    public String getRatingStars() {
        if (rating == null || rating < 1) {
            return "☆☆☆☆☆";
        }
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= rating) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
    }

    /**
     * レビューにタイトルがあるかチェック
     */
    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }

    /**
     * レビューにコメントがあるかチェック
     */
    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }

    /**
     * 購入確認済みレビューかチェック
     */
    public boolean isVerified() {
        return Boolean.TRUE.equals(isVerifiedPurchase);
    }

    /**
     * 役立つボタンを押された回数を増やす
     */
    public void increaseHelpfulCount() {
        if (helpfulCount == null) {
            helpfulCount = 0;
        }
        helpfulCount++;
        updatedAt = LocalDateTime.now();
    }

    /**
     * レビューの概要を取得（表示用）
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(getRatingStars());
        
        if (hasTitle()) {
            summary.append(" - ").append(title);
        }
        
        if (isVerified()) {
            summary.append(" [購入済み]");
        }
        
        return summary.toString();
    }

    /**
     * レビューコメントの短縮版を取得
     */
    public String getTruncatedComment(int maxLength) {
        if (!hasComment()) {
            return "";
        }
        
        if (comment.length() <= maxLength) {
            return comment;
        }
        
        return comment.substring(0, maxLength) + "...";
    }

    /**
     * レビューの表示用日付フォーマット
     */
    public String getFormattedCreatedDate() {
        return createdAt != null ? createdAt.toLocalDate().toString() : "";
    }

    /**
     * レビュー投稿者の表示名を取得（匿名化対応）
     */
    public String getReviewerDisplayName() {
        if (user == null || user.getUsername() == null) {
            return "匿名ユーザー";
        }
        
        String username = user.getUsername();
        if (username.length() <= 2) {
            return username.charAt(0) + "*";
        } else if (username.length() <= 4) {
            return username.charAt(0) + "**" + username.charAt(username.length() - 1);
        } else {
            return username.substring(0, 2) + "***" + username.charAt(username.length() - 1);
        }
    }
}