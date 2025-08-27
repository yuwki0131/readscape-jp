package jp.readscape.consumer.dto.carts;

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
@Schema(description = "カート内商品情報")
public class CartItemResponse {

    @Schema(description = "カートアイテムID", example = "1")
    private Long cartItemId;

    @Schema(description = "書籍ID", example = "1")
    private Long bookId;

    @Schema(description = "書籍タイトル", example = "Spring Boot入門")
    private String bookTitle;

    @Schema(description = "著者名", example = "田中太郎")
    private String bookAuthor;

    @Schema(description = "書籍の表紙画像URL", example = "https://example.com/images/book1.jpg")
    private String bookImageUrl;

    @Schema(description = "単価", example = "1500")
    private Integer unitPrice;

    @Schema(description = "単価（表示用）", example = "¥1,500")
    private String formattedUnitPrice;

    @Schema(description = "数量", example = "2")
    private Integer quantity;

    @Schema(description = "小計", example = "3000")
    private BigDecimal subtotal;

    @Schema(description = "小計（表示用）", example = "¥3,000")
    private String formattedSubtotal;

    @Schema(description = "在庫数", example = "10")
    private Integer stockQuantity;

    @Schema(description = "在庫が十分にあるか", example = "true")
    private boolean stockAvailable;
}