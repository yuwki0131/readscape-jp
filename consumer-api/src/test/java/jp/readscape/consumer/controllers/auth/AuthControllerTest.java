package jp.readscape.consumer.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.readscape.consumer.dto.auth.LoginRequest;
import jp.readscape.consumer.dto.auth.LoginResponse;
import jp.readscape.consumer.dto.auth.RefreshTokenRequest;
import jp.readscape.consumer.dto.auth.RefreshTokenResponse;
import jp.readscape.consumer.dto.auth.RegisterRequest;
import jp.readscape.consumer.dto.users.UserProfile;
import jp.readscape.consumer.services.JwtService;
import jp.readscape.consumer.services.UserService;
import jp.readscape.consumer.exceptions.InvalidCredentialsException;
import jp.readscape.consumer.exceptions.TokenExpiredException;
import jp.readscape.consumer.exceptions.DuplicateEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("ログイン - 正常系")
    void loginSuccess() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("test@example.com")
            .password("password123")
            .build();

        LoginResponse response = LoginResponse.builder()
            .accessToken("jwt-access-token")
            .refreshToken("jwt-refresh-token")
            .expiresIn(3600)
            .user(createSampleUserProfile())
            .build();

        when(userService.login(eq(request), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.name").value("テストユーザー"));

        verify(userService).login(eq(request), any());
    }

    @Test
    @DisplayName("ログイン - 認証失敗")
    void loginInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("test@example.com")
            .password("wrongpassword")
            .build();

        when(userService.login(eq(request), any()))
            .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(userService).login(eq(request), any());
    }

    @Test
    @DisplayName("ログイン - バリデーションエラー")
    void loginValidationError() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
            .email("invalid-email") // 不正なメールアドレス
            .password("123") // 短すぎるパスワード
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").exists());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("ユーザー登録 - 正常系")
    void registerSuccess() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
            .email("newuser@example.com")
            .password("password123")
            .name("新規ユーザー")
            .build();

        UserProfile userProfile = createSampleUserProfile();
        userProfile.setEmail("newuser@example.com");
        userProfile.setName("新規ユーザー");

        when(userService.register(request)).thenReturn(userProfile);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.name").value("新規ユーザー"));

        verify(userService).register(request);
    }

    @Test
    @DisplayName("ユーザー登録 - メールアドレス重複")
    void registerDuplicateEmail() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
            .email("existing@example.com")
            .password("password123")
            .name("テストユーザー")
            .build();

        when(userService.register(request))
            .thenThrow(new DuplicateEmailException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userService).register(request);
    }

    @Test
    @DisplayName("トークンリフレッシュ - 正常系")
    void refreshTokenSuccess() throws Exception {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken("valid-refresh-token")
            .build();

        RefreshTokenResponse response = RefreshTokenResponse.builder()
            .token("new-access-token")
            .refreshToken("new-refresh-token")
            .expiresIn(3600)
            .build();

        when(jwtService.refreshToken(request.getRefreshToken())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(jwtService).refreshToken(request.getRefreshToken());
    }

    @Test
    @DisplayName("トークンリフレッシュ - 期限切れ")
    void refreshTokenExpired() throws Exception {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken("expired-refresh-token")
            .build();

        when(jwtService.refreshToken(request.getRefreshToken()))
            .thenThrow(new TokenExpiredException("Refresh token expired"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("Refresh token expired"));

        verify(jwtService).refreshToken(request.getRefreshToken());
    }

    @Test
    @DisplayName("ログアウト - 正常系")
    void logoutSuccess() throws Exception {
        // Arrange
        String authToken = "Bearer valid-jwt-token";
        
        doNothing().when(userService).logout(any());

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("ログアウトしました"));

        verify(userService).logout(any());
    }

    @Test
    @DisplayName("パスワード変更 - 正常系")
    void changePasswordSuccess() throws Exception {
        // Arrange
        String authToken = "Bearer valid-jwt-token";
        String newPassword = "newpassword123";
        
        doNothing().when(userService).changePassword(any(), eq(newPassword));

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("newPassword", newPassword))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("パスワードを変更しました"));

        verify(userService).changePassword(any(), eq(newPassword));
    }

    @Test
    @DisplayName("パスワードリセット要求 - 正常系")
    void requestPasswordResetSuccess() throws Exception {
        // Arrange
        String email = "test@example.com";
        
        doNothing().when(userService).requestPasswordReset(email);

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("パスワードリセット用のメールを送信しました"));

        verify(userService).requestPasswordReset(email);
    }

    @Test
    @DisplayName("メールアドレス確認 - 正常系")
    void verifyEmailSuccess() throws Exception {
        // Arrange
        String token = "email-verification-token";
        
        doNothing().when(userService).verifyEmail(token);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-email")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("メールアドレスが確認されました"));

        verify(userService).verifyEmail(token);
    }

    // Helper methods
    private UserProfile createSampleUserProfile() {
        return UserProfile.builder()
            .id(1L)
            .usernameOrEmail("test@example.com")
            .name("テストユーザー")
            .phoneNumber("090-1234-5678")
            .address("東京都渋谷区")
            .birthDate("1990-01-01")
            .gender("MALE")
            .emailVerified(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}