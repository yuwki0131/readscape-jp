package jp.readscape.api.interfaces.controller.users;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    @GetMapping
    public String getUsers() {
        return "Hello, Users!";
    }
}
