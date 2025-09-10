package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.readscape.inventory.domain.books.model.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理者向け書籍表示用データ")
public class AdminBookView {

    @Schema(description = "書籍ID", example = "1")
    private Long id;

    @Schema(description = "タイトル", example = "Spring Boot実践ガイド")
    private String title;

    @Schema(description = "著者", example = "田中太郎")
    private String author;

    @Schema(description = "ISBN", example = "9784798142470")
    private String isbn;

    @Schema(description = "価格", example = "2980")
    private Integer price;

    @Schema(description = "在庫数", example = "25")
    private Integer stockQuantity;

    @Schema(description = "低在庫閾値", example = "10")
    private Integer lowStockThreshold;

    @Schema(description = "カテゴリ", example = "プログラミング")
    private String category;

    @Schema(description = "出版社", example = "技術評論社")
    private String publisher;

    @Schema(description = "平均評価", example = "4.3")
    private BigDecimal averageRating;

    @Schema(description = "レビュー数", example = "15")
    private Integer reviewCount;

    @Schema(description = "ステータス", example = "ACTIVE")
    private Book.BookStatus status;

    @Schema(description = "低在庫フラグ", example = "true")
    private boolean isLowStock;

    @Schema(description = "在庫切れフラグ", example = "false")
    private boolean isOutOfStock;

    @Schema(description = "作成日時", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新日時", example = "2024-01-20T14:45:00")
    private LocalDateTime updatedAt;

    @Schema(description = "在庫ステータス表示", example = "低在庫")
    private String stockStatus;

    @Schema(description = "価格表示用", example = "¥2,980")
    private String formattedPrice;

    // fromメソッドはDtoMappingServiceに移動されました
}