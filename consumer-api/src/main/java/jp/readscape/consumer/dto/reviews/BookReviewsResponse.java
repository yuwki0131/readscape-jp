package jp.readscape.consumer.dto.reviews;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookReviewsResponse {

    private Long bookId;
    private String bookTitle;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private RatingDistribution ratingDistribution;
    private List<ReviewResponse> reviews;
    private Integer currentPage;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private Integer rating5Count;
        private Integer rating4Count;
        private Integer rating3Count;
        private Integer rating2Count;
        private Integer rating1Count;

        public Integer getTotalCount() {
            return (rating5Count != null ? rating5Count : 0) +
                   (rating4Count != null ? rating4Count : 0) +
                   (rating3Count != null ? rating3Count : 0) +
                   (rating2Count != null ? rating2Count : 0) +
                   (rating1Count != null ? rating1Count : 0);
        }

        public Double getRating5Percentage() {
            int total = getTotalCount();
            return total > 0 ? (rating5Count != null ? rating5Count : 0) * 100.0 / total : 0.0;
        }

        public Double getRating4Percentage() {
            int total = getTotalCount();
            return total > 0 ? (rating4Count != null ? rating4Count : 0) * 100.0 / total : 0.0;
        }

        public Double getRating3Percentage() {
            int total = getTotalCount();
            return total > 0 ? (rating3Count != null ? rating3Count : 0) * 100.0 / total : 0.0;
        }

        public Double getRating2Percentage() {
            int total = getTotalCount();
            return total > 0 ? (rating2Count != null ? rating2Count : 0) * 100.0 / total : 0.0;
        }

        public Double getRating1Percentage() {
            int total = getTotalCount();
            return total > 0 ? (rating1Count != null ? rating1Count : 0) * 100.0 / total : 0.0;
        }
    }

    public String getFormattedAverageRating() {
        return averageRating != null ? String.format("%.1f", averageRating) : "0.0";
    }

    public String getAverageRatingStars() {
        if (averageRating == null) {
            return "☆☆☆☆☆";
        }
        
        double rating = averageRating.doubleValue();
        StringBuilder stars = new StringBuilder();
        
        for (int i = 1; i <= 5; i++) {
            if (rating >= i) {
                stars.append("★");
            } else if (rating >= i - 0.5) {
                stars.append("⭐"); // 半星
            } else {
                stars.append("☆");
            }
        }
        
        return stars.toString();
    }
}