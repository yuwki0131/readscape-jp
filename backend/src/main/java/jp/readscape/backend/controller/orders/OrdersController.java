package jp.readscape.backend.controller.orders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    @GetMapping
    public String getOrders() {
        return "Hello, Orders!";
    }
}
