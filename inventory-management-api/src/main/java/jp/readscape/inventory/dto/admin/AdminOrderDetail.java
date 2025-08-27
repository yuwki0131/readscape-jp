package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.readscape.inventory.domain.orders.model.Order;
import jp.readscape.inventory.domain.orders.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理者向け注文詳細データ")
public class AdminOrderDetail {

    @Schema(description = "注文ID", example = "1")
    private Long id;

    @Schema(description = "注文番号", example = "ORD-20241225-1234")
    private String orderNumber;

    @Schema(description = "ユーザー情報")
    private UserInfo user;

    @Schema(description = "注文ステータス", example = "PENDING")
    private Order.OrderStatus status;

    @Schema(description = "注文ステータス表示名", example = "注文受付")
    private String statusDisplayName;

    @Schema(description = "合計金額", example = "4500.00")
    private BigDecimal totalAmount;

    @Schema(description = "合計金額（表示用）", example = "¥4,500")
    private String formattedTotalAmount;

    @Schema(description = "注文アイテム一覧")
    private List<OrderItemInfo> items;

    @Schema(description = "配送情報")
    private ShippingInfo shipping;

    @Schema(description = "支払い情報")
    private PaymentInfo payment;

    @Schema(description = "備考", example = "お急ぎでお願いします")
    private String notes;

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

    @Schema(description = "キャンセル可能フラグ", example = "true")
    private boolean cancellable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        @Schema(description = "ユーザーID", example = "1")
        private Long id;
        
        @Schema(description = "ユーザー名", example = "tanaka_taro")
        private String username;
        
        @Schema(description = "メールアドレス", example = "tanaka@example.com")
        private String email;
        
        @Schema(description = "フルネーム", example = "田中太郎")
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        @Schema(description = "注文アイテムID", example = "1")
        private Long id;
        
        @Schema(description = "書籍ID", example = "1")
        private Long bookId;
        
        @Schema(description = "書籍タイトル", example = "Spring Boot実践ガイド")
        private String bookTitle;
        
        @Schema(description = "著者", example = "田中太郎")
        private String bookAuthor;
        
        @Schema(description = "ISBN", example = "9784798142470")
        private String bookIsbn;
        
        @Schema(description = "数量", example = "2")
        private Integer quantity;
        
        @Schema(description = "単価", example = "2980")
        private Integer unitPrice;
        
        @Schema(description = "小計", example = "5960.00")
        private BigDecimal subtotal;
        
        @Schema(description = "単価（表示用）", example = "¥2,980")
        private String formattedUnitPrice;
        
        @Schema(description = "小計（表示用）", example = "¥5,960")
        private String formattedSubtotal;

        // fromメソッドはDtoMappingServiceに移動されました
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingInfo {
        @Schema(description = "配送先住所", example = "東京都渋谷区...")
        private String address;
        
        @Schema(description = "配送先電話番号", example = "090-1234-5678")
        private String phone;
        
        @Schema(description = "発送日時", example = "2024-01-21T10:00:00")
        private LocalDateTime shippedDate;
        
        @Schema(description = "配送完了日時", example = "2024-01-22T14:30:00")
        private LocalDateTime deliveredDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        @Schema(description = "支払い方法", example = "クレジットカード")
        private String method;
        
        @Schema(description = "支払い状況", example = "完了")
        private String status;
    }

    // fromメソッドはDtoMappingServiceに移動されました
}