package jp.readscape.consumer.dto.carts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "カート追加リクエスト")
public class AddToCartRequest {

    @NotNull(message = "書籍IDは必須です")
    @Schema(description = "書籍ID", example = "1")
    private Long bookId;

    @NotNull(message = "数量は必須です")
    @Min(value = 1, message = "数量は1以上である必要があります")
    @Schema(description = "数量", example = "2")
    private Integer quantity;
}