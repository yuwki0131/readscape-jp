package jp.readscape.inventory.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * リフレッシュトークンリクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "リフレッシュトークンリクエスト")
public class RefreshTokenRequest {

    @NotBlank(message = "リフレッシュトークンは必須です")
    @Schema(description = "リフレッシュトークン", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}