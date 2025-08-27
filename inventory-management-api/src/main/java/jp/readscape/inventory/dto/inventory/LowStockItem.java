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
@Schema(description = "低在庫アイテム情報")
public class LowStockItem {

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

    @Schema(description = "現在在庫数", example = "5")
    private Integer stockQuantity;

    @Schema(description = "低在庫閾値", example = "10")
    private Integer lowStockThreshold;

    @Schema(description = "不足数", example = "5")
    private Integer shortfallQuantity;

    @Schema(description = "緊急度", example = "HIGH")
    private AlertLevel alertLevel;

    @Schema(description = "在庫切れまでの日数予測", example = "3")
    private Integer daysUntilOutOfStock;

    @Schema(description = "最終入荷日", example = "2024-01-15T10:00:00")
    private LocalDateTime lastRestockDate;

    @Schema(description = "推奨発注数", example = "50")
    private Integer recommendedOrderQuantity;

    // fromメソッドはDtoMappingServiceに移動されました

    public enum AlertLevel {
        LOW,        // 低
        MEDIUM,     // 中
        HIGH,       // 高
        CRITICAL    // 緊急
    }
}