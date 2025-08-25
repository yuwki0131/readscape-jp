package jp.readscape.consumer.dto.books;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "書籍サマリー情報")
public class BookSummary {

    @Schema(description = "書籍ID", example = "1")
    private Long id;

    @Schema(description = "書籍タイトル", example = "Spring Boot実践入門")
    private String title;

    @Schema(description = "著者名", example = "技術太郎")
    private String author;

    @Schema(description = "価格（円）", example = "3200")
    private Integer price;

    @Schema(description = "カテゴリー", example = "技術書")
    private String category;

    @Schema(description = "平均評価", example = "4.5")
    @JsonProperty("average_rating")
    private BigDecimal averageRating;

    @Schema(description = "レビュー数", example = "12")
    @JsonProperty("review_count")
    private Integer reviewCount;

    @Schema(description = "画像URL", example = "https://images.readscape.jp/books/spring-boot.jpg")
    @JsonProperty("image_url")
    private String imageUrl;

    @Schema(description = "在庫の有無", example = "true")
    @JsonProperty("in_stock")
    private Boolean inStock;

    // ビジネスロジック用メソッド
    
    @Schema(description = "フォーマットされた価格", example = "¥3,200")
    @JsonProperty("formatted_price")
    public String getFormattedPrice() {
        return price != null ? String.format("¥%,d", price) : "¥0";
    }

    @Schema(description = "評価があるかどうか", example = "true")
    @JsonProperty("has_reviews")
    public Boolean getHasReviews() {
        return reviewCount != null && reviewCount > 0;
    }

    @Schema(description = "平均評価の星表示", example = "★★★★☆")
    @JsonProperty("rating_stars")
    public String getRatingStars() {
        if (averageRating == null) {
            return "☆☆☆☆☆";
        }
        
        int fullStars = averageRating.intValue();
        boolean hasHalfStar = averageRating.subtract(BigDecimal.valueOf(fullStars)).compareTo(BigDecimal.valueOf(0.5)) >= 0;
        
        StringBuilder stars = new StringBuilder();
        
        // 満点の星
        for (int i = 0; i < fullStars; i++) {
            stars.append("★");
        }
        
        // 半分の星
        if (hasHalfStar && fullStars < 5) {
            stars.append("☆");
            fullStars++;
        }
        
        // 空の星
        for (int i = fullStars; i < 5; i++) {
            stars.append("☆");
        }
        
        return stars.toString();
    }
}