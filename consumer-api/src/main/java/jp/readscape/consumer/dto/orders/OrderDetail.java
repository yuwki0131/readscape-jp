package jp.readscape.consumer.dto.orders;

import jp.readscape.consumer.domain.orders.model.Order;
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
public class OrderDetail {

    private Long id;
    private String orderNumber;
    private String status;
    private String statusDisplayName;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String shippingAddress;
    private String shippingPhone;
    private String paymentMethod;
    private String notes;
    private LocalDateTime orderDate;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;
    private LocalDateTime estimatedDeliveryDate;
    private List<OrderItemDetail> items;

    public static OrderDetail from(Order order) {
        List<OrderItemDetail> itemDetails = order.getItems().stream()
                .map(OrderItemDetail::from)
                .collect(Collectors.toList());

        return OrderDetail.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .statusDisplayName(order.getStatusDisplayName())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItemCount())
                .shippingAddress(order.getShippingAddress())
                .shippingPhone(order.getShippingPhone())
                .paymentMethod(order.getPaymentMethod())
                .notes(order.getNotes())
                .orderDate(order.getOrderDate())
                .shippedDate(order.getShippedDate())
                .deliveredDate(order.getDeliveredDate())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .items(itemDetails)
                .build();
    }

    public String getFormattedTotalAmount() {
        return totalAmount != null ? String.format("¥%,.0f", totalAmount) : "¥0";
    }

    public boolean isEditable() {
        return "PENDING".equals(status);
    }

    public boolean isCancellable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }
}