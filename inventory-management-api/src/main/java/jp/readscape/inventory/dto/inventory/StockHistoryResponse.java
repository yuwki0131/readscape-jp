package jp.readscape.inventory.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.readscape.inventory.domain.inventory.model.StockHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "在庫履歴レスポンス")
public class StockHistoryResponse {

    @Schema(description = "履歴ID", example = "1")
    private Long id;

    @Schema(description = "書籍ID", example = "1")
    private Long bookId;

    @Schema(description = "書籍タイトル", example = "Spring Boot実践ガイド")
    private String bookTitle;

    @Schema(description = "操作者ユーザーID", example = "1")
    private Long userId;

    @Schema(description = "操作者ユーザー名", example = "admin")
    private String username;

    @Schema(description = "変更タイプ", example = "INBOUND")
    private StockHistory.StockChangeType type;

    @Schema(description = "変更タイプ表示名", example = "入荷")
    private String typeDisplayName;

    @Schema(description = "変更数量", example = "50")
    private Integer quantityChange;

    @Schema(description = "変更前在庫数", example = "20")
    private Integer quantityBefore;

    @Schema(description = "変更後在庫数", example = "70")
    private Integer quantityAfter;

    @Schema(description = "変更理由", example = "新規入荷")
    private String reason;

    @Schema(description = "参照番号", example = "IN-20241225-001")
    private String referenceNumber;

    @Schema(description = "変更日時", example = "2024-01-20T15:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "変更数量（表示用）", example = "+50")
    private String formattedChange;

    // fromメソッドはDtoMappingServiceに移動されました
}