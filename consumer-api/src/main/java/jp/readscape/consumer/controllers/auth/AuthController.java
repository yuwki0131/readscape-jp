package jp.readscape.consumer.controllers.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.auth.LoginRequest;
import jp.readscape.consumer.dto.auth.LoginResponse;
import jp.readscape.consumer.dto.auth.RefreshTokenRequest;
import jp.readscape.consumer.dto.auth.RefreshTokenResponse;
import jp.readscape.consumer.services.JwtService;
import jp.readscape.consumer.services.UserService;
import jp.readscape.consumer.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "認証API")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @Operation(
        summary = "ログイン",
        description = "ユーザー認証を行い、JWTトークンを返します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ログイン成功"),
        @ApiResponse(responseCode = "401", description = "認証失敗",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String maskedIdentifier = SecurityUtils.maskUserIdentifier(request.getUsernameOrEmail());
        String clientIp = SecurityUtils.getClientIpAddress(httpRequest);
        log.info("POST /api/auth/login - user: {}, ip: {}", maskedIdentifier, SecurityUtils.maskIpAddress(clientIp));

        try {
            Optional<User> userOpt = userService.authenticateUser(
                    request.getUsernameOrEmail(), 
                    request.getPassword(),
                    clientIp
            );

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String accessToken = jwtService.generateToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);
                
                LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiration())
                    .user(LoginResponse.UserInfo.from(user))
                    .build();
                
                log.info("User logged in successfully: {}", SecurityUtils.maskUserIdentifier(user.getUsername()));
                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for user: {}", SecurityUtils.maskUserIdentifier(request.getUsernameOrEmail()));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(jp.readscape.consumer.dto.ApiResponse.error("ユーザー名またはパスワードが正しくありません"));
            }
        } catch (Exception e) {
            log.error("Login error for user {}: {}", SecurityUtils.maskUserIdentifier(request.getUsernameOrEmail()), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(jp.readscape.consumer.dto.ApiResponse.error("ログイン処理中にエラーが発生しました"));
        }
    }

    @Operation(
        summary = "トークン更新",
        description = "リフレッシュトークンを使用してアクセストークンを更新します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "トークン更新成功"),
        @ApiResponse(responseCode = "401", description = "無効なリフレッシュトークン")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/refresh");

        try {
            String refreshToken = request.getRefreshToken();
            
            if (!jwtService.isRefreshTokenValid(refreshToken)) {
                log.warn("Invalid refresh token provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(jp.readscape.consumer.dto.ApiResponse.error("無効なリフレッシュトークンです"));
            }

            String username = jwtService.extractUsername(refreshToken);
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                log.warn("User not found for refresh token: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(jp.readscape.consumer.dto.ApiResponse.error("ユーザーが見つかりません"));
            }

            User user = userOpt.get();
            
            // セキュリティ向上のため、古いリフレッシュトークンを無効化
            jwtService.invalidateToken(refreshToken);
            
            // 新しいアクセストークンとリフレッシュトークンを生成（ローテーション）
            String newAccessToken = jwtService.generateToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            
            RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .build();
            
            log.info("Token refreshed successfully for user: {}", SecurityUtils.maskUserIdentifier(username));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(jp.readscape.consumer.dto.ApiResponse.error("トークン更新に失敗しました"));
        }
    }

    @Operation(
        summary = "ログアウト",
        description = "ユーザーをログアウトし、トークンを無効化します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ログアウト成功"),
        @ApiResponse(responseCode = "400", description = "無効なリクエスト")
    })
    @PostMapping("/logout")
    public ResponseEntity<jp.readscape.consumer.dto.ApiResponse> logout(HttpServletRequest request) {
        log.info("POST /api/auth/logout");

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                jwtService.invalidateToken(token);
                log.info("User logged out successfully");
            }
            
            return ResponseEntity.ok(jp.readscape.consumer.dto.ApiResponse.success("ログアウトしました"));
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.ok(jp.readscape.consumer.dto.ApiResponse.success("ログアウトしました"));
        }
    }
}