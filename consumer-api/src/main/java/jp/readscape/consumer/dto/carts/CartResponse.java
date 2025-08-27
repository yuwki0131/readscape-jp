package jp.readscape.consumer.dto.carts;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "カート情報レスポンス")
public class CartResponse {

    @Schema(description = "カートID", example = "1")
    private Long cartId;

    @Schema(description = "カート内商品一覧")
    private List<CartItemResponse> items;

    @Schema(description = "合計商品数", example = "3")
    private int totalItemCount;

    @Schema(description = "合計金額", example = "4500")
    private BigDecimal totalAmount;

    @Schema(description = "合計金額（表示用）", example = "¥4,500")
    private String formattedTotalAmount;

    @Schema(description = "カートが空かどうか", example = "false")
    private boolean isEmpty;
}