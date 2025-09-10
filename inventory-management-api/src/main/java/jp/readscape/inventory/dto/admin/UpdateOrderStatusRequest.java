package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jp.readscape.inventory.domain.orders.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "注文ステータス更新リクエスト")
public class UpdateOrderStatusRequest {

    @NotNull(message = "新しいステータスは必須です")
    @Schema(description = "新しい注文ステータス", example = "CONFIRMED")
    private Order.OrderStatus status;

    @Schema(description = "ステータス変更理由", example = "在庫確認完了")
    private String reason;

    @Schema(description = "備考", example = "緊急配送で対応")
    private String notes;
}