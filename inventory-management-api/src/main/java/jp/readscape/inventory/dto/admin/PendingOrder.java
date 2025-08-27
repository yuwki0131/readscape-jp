package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.readscape.inventory.domain.orders.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "処理待ち注文情報")
public class PendingOrder {

    @Schema(description = "注文ID", example = "1")
    private Long id;

    @Schema(description = "注文番号", example = "ORD-20241225-1234")
    private String orderNumber;

    @Schema(description = "ユーザー名", example = "tanaka_taro")
    private String username;

    @Schema(description = "ユーザーメール", example = "tanaka@example.com")
    private String userEmail;

    @Schema(description = "合計金額", example = "4500.00")
    private BigDecimal totalAmount;

    @Schema(description = "合計金額（表示用）", example = "¥4,500")
    private String formattedTotalAmount;

    @Schema(description = "商品数", example = "3")
    private int itemCount;

    @Schema(description = "注文日時", example = "2024-01-20T15:30:00")
    private LocalDateTime orderDate;

    @Schema(description = "経過時間（時間）", example = "25")
    private long hoursElapsed;

    @Schema(description = "緊急度", example = "HIGH")
    private Priority priority;

    @Schema(description = "配送先都道府県", example = "東京都")
    private String shippingPrefecture;

    // fromメソッドはDtoMappingServiceに移動されました

    public enum Priority {
        LOW,        // 低
        MEDIUM,     // 中
        HIGH,       // 高
        CRITICAL    // 緊急
    }
}