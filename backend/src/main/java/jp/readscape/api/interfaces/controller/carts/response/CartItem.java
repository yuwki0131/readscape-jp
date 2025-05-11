package jp.readscape.api.interfaces.controller.carts.response;

import lombok.Data;

@Data
public class CartItem {
    private Long bookId;
    private String title;
    private int price;
    private int quantity;
}
