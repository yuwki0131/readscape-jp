package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.readscape.inventory.domain.orders.model.Order;
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
@Schema(description = "管理者向け注文表示用データ")
public class AdminOrderView {

    @Schema(description = "注文ID", example = "1")
    private Long id;

    @Schema(description = "注文番号", example = "ORD-20241225-1234")
    private String orderNumber;

    @Schema(description = "ユーザーID", example = "1")
    private Long userId;

    @Schema(description = "ユーザー名", example = "tanaka_taro")
    private String username;

    @Schema(description = "ユーザーメール", example = "tanaka@example.com")
    private String userEmail;

    @Schema(description = "注文ステータス", example = "PENDING")
    private Order.OrderStatus status;

    @Schema(description = "注文ステータス表示名", example = "注文受付")
    private String statusDisplayName;

    @Schema(description = "合計金額", example = "4500.00")
    private BigDecimal totalAmount;

    @Schema(description = "合計金額（表示用）", example = "¥4,500")
    private String formattedTotalAmount;

    @Schema(description = "商品数", example = "3")
    private int itemCount;

    @Schema(description = "配送先住所", example = "東京都渋谷区...")
    private String shippingAddress;

    @Schema(description = "配送先電話番号", example = "090-1234-5678")
    private String shippingPhone;

    @Schema(description = "支払い方法", example = "クレジットカード")
    private String paymentMethod;

    @Schema(description = "注文日時", example = "2024-01-20T15:30:00")
    private LocalDateTime orderDate;

    @Schema(description = "発送日時", example = "2024-01-21T10:00:00")
    private LocalDateTime shippedDate;

    @Schema(description = "配送完了日時", example = "2024-01-22T14:30:00")
    private LocalDateTime deliveredDate;

    @Schema(description = "作成日時", example = "2024-01-20T15:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新日時", example = "2024-01-21T10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "緊急フラグ", example = "false")
    private boolean isUrgent;

    @Schema(description = "処理可能フラグ", example = "true")
    private boolean canProcess;

    // fromメソッドはDtoMappingServiceに移動されました
}