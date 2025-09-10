package jp.readscape.consumer.dto.reviews;

import jp.readscape.consumer.domain.reviews.model.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {

    private Long id;
    private Integer rating;
    private String title;
    private String shortComment;
    private String reviewerName;
    private Boolean isVerifiedPurchase;
    private Integer helpfulCount;
    private LocalDateTime createdAt;

    public static ReviewSummary from(Review review) {
        return ReviewSummary.builder()
                .id(review.getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .shortComment(review.getTruncatedComment(100))
                .reviewerName(review.getReviewerDisplayName())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .build();
    }

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

    public String getFormattedCreatedDate() {
        return createdAt != null ? createdAt.toLocalDate().toString() : "";
    }
}