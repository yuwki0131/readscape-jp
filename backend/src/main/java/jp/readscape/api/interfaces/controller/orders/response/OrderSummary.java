package jp.readscape.api.interfaces.controller.orders.response;

import lombok.Data;

@Data
public class OrderSummary {
    private Long orderId;
    private String date;
    private int totalPrice;
    private String status;
}
