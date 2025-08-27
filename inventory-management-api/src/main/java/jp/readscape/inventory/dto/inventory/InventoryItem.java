package jp.readscape.inventory.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.readscape.inventory.domain.books.model.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "在庫アイテム情報")
public class InventoryItem {

    @Schema(description = "書籍ID", example = "1")
    private Long bookId;

    @Schema(description = "タイトル", example = "Spring Boot実践ガイド")
    private String title;

    @Schema(description = "著者", example = "田中太郎")
    private String author;

    @Schema(description = "ISBN", example = "9784798142470")
    private String isbn;

    @Schema(description = "カテゴリ", example = "プログラミング")
    private String category;

    @Schema(description = "現在在庫数", example = "25")
    private Integer stockQuantity;

    @Schema(description = "低在庫閾値", example = "10")
    private Integer lowStockThreshold;

    @Schema(description = "低在庫フラグ", example = "false")
    private boolean isLowStock;

    @Schema(description = "在庫切れフラグ", example = "false")
    private boolean isOutOfStock;

    @Schema(description = "在庫ステータス", example = "正常")
    private String stockStatus;

    @Schema(description = "書籍ステータス", example = "ACTIVE")
    private Book.BookStatus status;

    @Schema(description = "最終更新日時", example = "2024-01-20T15:30:00")
    private LocalDateTime lastUpdatedAt;

    // fromメソッドはDtoMappingServiceに移動されました
}