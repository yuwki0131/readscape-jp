package jp.readscape.api.interfaces.controller.users.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String email;
}
