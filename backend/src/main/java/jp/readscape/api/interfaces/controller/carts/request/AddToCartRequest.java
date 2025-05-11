package jp.readscape.api.interfaces.controller.carts.request;

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long bookId;
    private int quantity;
}
