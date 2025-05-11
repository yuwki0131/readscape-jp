package jp.readscape.api.interfaces.controller.orders.response;

import lombok.Data;

@Data
public class CreateOrderResponse {
    private Long orderId;
    private String message;
}
