package jp.readscape.consumer.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ログインリクエスト")
public class LoginRequest {
    
    @NotBlank(message = "ユーザー名またはメールアドレスは必須です")
    @Size(min = 1, max = 100, message = "ユーザー名またはメールアドレスは1-100文字で入力してください")
    @Schema(description = "ユーザー名またはメールアドレス", example = "john_doe")
    private String usernameOrEmail;
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, max = 128, message = "パスワードは8-128文字で入力してください")
    @Schema(description = "パスワード", example = "password123")
    private String password;
}