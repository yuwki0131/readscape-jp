package jp.readscape.api.interfaces.controller.orders.response;

import jp.readscape.api.domain.carts.model.CartItem;
import lombok.Data;

@Data
public class OrderDetail {
    private Long orderId;
    private String date;
    private int totalPrice;
    private String status;
    private java.util.List<CartItem> items;
}
