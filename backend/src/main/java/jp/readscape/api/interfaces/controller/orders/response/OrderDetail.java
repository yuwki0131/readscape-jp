package jp.readscape.api.interfaces.controller.orders.response;

import lombok.Data;

@Data
public class OrderDetail {
    private Long orderId;
    private String date;
    private int totalPrice;
    private String status;
    private java.util.List<CartItem> items;
}
