package jp.readscape.backend.controller.carts;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartsController {

    @GetMapping
    public String getCarts() {
        return "Hello, cart!";
    }
}
