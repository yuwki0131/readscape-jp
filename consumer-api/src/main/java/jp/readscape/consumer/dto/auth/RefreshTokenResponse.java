package jp.readscape.consumer.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "リフレッシュトークンレスポンス")
public class RefreshTokenResponse {
    
    @Schema(description = "新しいアクセストークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "新しいリフレッシュトークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    @Schema(description = "トークンタイプ", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "トークン有効期限（秒）", example = "86400")
    private Long expiresIn;
}