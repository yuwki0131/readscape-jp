package jp.readscape.inventory.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインリクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ログインリクエスト")
public class LoginRequest {

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @Schema(description = "メールアドレス", example = "admin@readscape.jp")
    private String email;

    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください")
    @Schema(description = "パスワード", example = "SecurePass123!")
    private String password;

    @Schema(description = "ログイン状態を記憶するかどうか", example = "false")
    private boolean rememberMe = false;
}