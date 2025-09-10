package jp.readscape.inventory.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jp.readscape.inventory.domain.inventory.model.StockHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "在庫更新リクエスト")
public class StockUpdateRequest {

    @NotNull(message = "更新タイプは必須です")
    @Schema(description = "在庫変更タイプ", example = "INBOUND")
    private StockHistory.StockChangeType type;

    @NotNull(message = "数量は必須です")
    @Schema(description = "変更数量（負の数も可）", example = "50")
    private Integer quantity;

    @NotBlank(message = "理由は必須です")
    @Size(max = 500, message = "理由は500文字以内で入力してください")
    @Schema(description = "在庫変更理由", example = "新規入荷")
    private String reason;

    @Size(max = 100, message = "参照番号は100文字以内で入力してください")
    @Schema(description = "参照番号（注文番号、入荷番号等）", example = "IN-20241225-001")
    private String referenceNumber;

    // バリデーション用メソッド
    public boolean isInboundOperation() {
        return type == StockHistory.StockChangeType.INBOUND ||
               type == StockHistory.StockChangeType.RETURN_FROM_CUSTOMER ||
               type == StockHistory.StockChangeType.ADJUSTMENT_INCREASE ||
               type == StockHistory.StockChangeType.TRANSFER_IN;
    }

    public boolean isOutboundOperation() {
        return type == StockHistory.StockChangeType.OUTBOUND ||
               type == StockHistory.StockChangeType.DAMAGED ||
               type == StockHistory.StockChangeType.ADJUSTMENT_DECREASE ||
               type == StockHistory.StockChangeType.TRANSFER_OUT;
    }

    /**
     * 実際の在庫変動量を取得
     * 入庫系は正の値、出庫系は負の値に正規化
     */
    public Integer getNormalizedQuantity() {
        if (isInboundOperation()) {
            return Math.abs(quantity);
        } else if (isOutboundOperation()) {
            return -Math.abs(quantity);
        } else {
            return quantity;
        }
    }
}