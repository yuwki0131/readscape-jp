package jp.readscape.inventory.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ログインレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ログインレスポンス")
public class LoginResponse {

    @Schema(description = "アクセストークン")
    private String accessToken;

    @Schema(description = "リフレッシュトークン")
    private String refreshToken;

    @Schema(description = "トークン種別", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "アクセストークンの有効期限（秒）", example = "1800")
    private long expiresIn;

    @Schema(description = "ユーザー情報")
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "ユーザー情報")
    public static class UserInfo {
        
        @Schema(description = "ユーザーID", example = "1")
        private Long id;

        @Schema(description = "ユーザー名", example = "admin")
        private String username;

        @Schema(description = "メールアドレス", example = "admin@readscape.jp")
        private String email;

        @Schema(description = "氏名", example = "管理者 太郎")
        private String fullName;

        @Schema(description = "ロール", example = "ADMIN")
        private String role;

        @Schema(description = "ロール説明", example = "システム管理者")
        private String roleDescription;

        @Schema(description = "最終ログイン日時", example = "2024-01-15T10:30:00")
        private LocalDateTime lastLoginAt;
    }
}