package jp.readscape.consumer.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "リフレッシュトークンリクエスト")
public class RefreshTokenRequest {
    
    @NotBlank(message = "リフレッシュトークンは必須です")
    @Schema(description = "リフレッシュトークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}