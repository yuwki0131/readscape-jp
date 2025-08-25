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
public class ReviewResponse {

    private Long id;
    private Integer rating;
    private String title;
    private String comment;
    private String reviewerName;
    private Boolean isVerifiedPurchase;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .reviewerName(review.getReviewerDisplayName())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
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

    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }

    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }
}