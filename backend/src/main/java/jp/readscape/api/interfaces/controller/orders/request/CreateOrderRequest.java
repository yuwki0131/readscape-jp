package jp.readscape.api.interfaces.controller.orders.request;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private String paymentMethod;
    private String address;
}
