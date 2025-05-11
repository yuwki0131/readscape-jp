package jp.readscape.api.interfaces.controller.carts.response;

import lombok.Data;

@Data
public class CartResponse {
    private java.util.List<CartItem> items;
    private int totalPrice;
}
