package jp.readscape.consumer.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private UserProfile user;


    public static LoginResponse of(String token, UserProfile user) {
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(user)
                .build();
    }
}