package jp.readscape.consumer.dto.orders;

import jp.readscape.consumer.domain.orders.model.Order;
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
public class CreateOrderResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private LocalDateTime orderDate;

    public static CreateOrderResponse from(Order order) {
        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItemCount())
                .orderDate(order.getOrderDate())
                .build();
    }

    public String getFormattedTotalAmount() {
        return totalAmount != null ? String.format("¥%,.0f", totalAmount) : "¥0";
    }
}