package jp.readscape.inventory.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * リフレッシュトークンレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "リフレッシュトークンレスポンス")
public class RefreshTokenResponse {

    @Schema(description = "新しいアクセストークン")
    private String accessToken;

    @Schema(description = "新しいリフレッシュトークン")
    private String refreshToken;

    @Schema(description = "トークン種別", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "アクセストークンの有効期限（秒）", example = "1800")
    private long expiresIn;
}