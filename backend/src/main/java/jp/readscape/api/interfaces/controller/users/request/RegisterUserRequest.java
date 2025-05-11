package jp.readscape.api.interfaces.controller.users.request;

import lombok.Data;

@Data
public class RegisterUserRequest {
    private String username;
    private String email;
    private String password;
}
