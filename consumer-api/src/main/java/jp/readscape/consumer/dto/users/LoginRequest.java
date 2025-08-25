package jp.readscape.consumer.dto.users;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "ユーザー名またはメールアドレスは必須です")
    private String usernameOrEmail;

    @NotBlank(message = "パスワードは必須です")
    private String password;
}