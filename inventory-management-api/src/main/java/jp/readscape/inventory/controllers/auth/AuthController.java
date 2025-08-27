package jp.readscape.inventory.controllers.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jp.readscape.inventory.dto.auth.LoginRequest;
import jp.readscape.inventory.dto.auth.LoginResponse;
import jp.readscape.inventory.dto.auth.RefreshTokenRequest;
import jp.readscape.inventory.dto.auth.RefreshTokenResponse;
import jp.readscape.inventory.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 認証コントローラー
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "認証API", description = "ユーザー認証に関するAPI")
public class AuthController {

    private final AuthService authService;

    /**
     * ユーザーログイン
     */
    @Operation(summary = "ログイン", description = "メールアドレスとパスワードでログインし、JWTトークンを取得")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ログイン成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト形式エラー"),
        @ApiResponse(responseCode = "401", description = "認証失敗"),
        @ApiResponse(responseCode = "429", description = "ログイン試行回数上限")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        log.info("Login attempt from IP: {} for email: {}", clientIp, request.getEmail());

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * トークンリフレッシュ
     */
    @Operation(summary = "トークン更新", description = "リフレッシュトークンを使用してアクセストークンを更新")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "トークン更新成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト形式エラー"),
        @ApiResponse(responseCode = "401", description = "無効なリフレッシュトークン")
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        log.debug("Token refresh attempt from IP: {}", clientIp);

        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * ログアウト
     */
    @Operation(summary = "ログアウト", description = "現在のセッションを終了し、トークンを無効化")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ログアウト成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要")
    })
    @PostMapping("/logout")
    public ResponseEntity<jp.readscape.inventory.dto.ApiResponse> logout(
            HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        log.info("Logout attempt from IP: {}", clientIp);

        // 現在の実装では、クライアント側でトークンを削除することで対応
        // 将来的にはTokenBlacklistServiceを実装してサーバーサイドでも無効化
        
        return ResponseEntity.ok(
            jp.readscape.inventory.dto.ApiResponse.success("ログアウトしました")
        );
    }

    /**
     * 現在のユーザー情報取得
     */
    @Operation(summary = "現在のユーザー情報", description = "認証済みユーザーの情報を取得")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ユーザー情報取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要")
    })
    @GetMapping("/me")
    public ResponseEntity<jp.readscape.inventory.dto.ApiResponse> getCurrentUser(
            HttpServletRequest request) {
        
        // SecurityContextからユーザー情報を取得
        String email = (String) request.getAttribute("userEmail");
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(jp.readscape.inventory.dto.ApiResponse.error("認証が必要です"));
        }

        return ResponseEntity.ok(
            jp.readscape.inventory.dto.ApiResponse.success("現在のユーザー: " + email)
        );
    }

    /**
     * パスワード変更
     */
    @Operation(summary = "パスワード変更", description = "認証済みユーザーのパスワードを変更")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "パスワード変更成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト形式エラー"),
        @ApiResponse(responseCode = "401", description = "認証が必要")
    })
    @PostMapping("/change-password")
    public ResponseEntity<jp.readscape.inventory.dto.ApiResponse> changePassword(
            HttpServletRequest request) {
        
        // 現在の実装では未対応
        // 将来的にChangePasswordRequestDTOとPasswordServiceを実装
        
        return ResponseEntity.status(501)
                .body(jp.readscape.inventory.dto.ApiResponse.error("この機能は未実装です"));
    }

    /**
     * クライアントIPアドレスを取得
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}