package jp.readscape.api.interfaces.controller.users.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
