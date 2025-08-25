package jp.readscape.consumer.dto.users;

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
public class OrderSummary {

    private Long id;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String shippingAddress;
    private LocalDateTime deliveryDate;

    public String getFormattedTotalAmount() {
        return totalAmount != null ? String.format("¥%,.0f", totalAmount) : "¥0";
    }

    public static OrderSummary from(Order order) {
        return OrderSummary.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getOrderDate())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItemCount())
                .shippingAddress(order.getShippingAddress())
                .deliveryDate(order.getDeliveredDate())
                .build();
    }
}