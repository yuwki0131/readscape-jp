package jp.readscape.consumer.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.readscape.consumer.domain.users.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ログインレスポンス")
public class LoginResponse {
    
    @Schema(description = "アクセストークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "リフレッシュトークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    @Schema(description = "トークンタイプ", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "トークン有効期限（秒）", example = "86400")
    private Long expiresIn;
    
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
        
        @Schema(description = "ユーザー名", example = "john_doe")
        private String username;
        
        @Schema(description = "メールアドレス", example = "john@example.com")
        private String email;
        
        @Schema(description = "フルネーム", example = "John Doe")
        private String fullName;
        
        @Schema(description = "ユーザーロール", example = "CONSUMER")
        private String role;
        
        public static UserInfo from(User user) {
            return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
        }
    }
}