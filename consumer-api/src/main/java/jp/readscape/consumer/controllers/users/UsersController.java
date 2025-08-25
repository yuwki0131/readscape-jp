package jp.readscape.consumer.controllers.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.users.*;
import jp.readscape.consumer.services.JwtService;
import jp.readscape.consumer.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "ユーザー管理API")
public class UsersController {

    private final UserService userService;
    private final JwtService jwtService;

    @Operation(
        summary = "ユーザー登録",
        description = "新規ユーザーを登録します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "ユーザー登録成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラーまたは重複エラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<jp.readscape.consumer.dto.ApiResponse> registerUser(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        log.info("POST /api/users/register - username: {}", request.getUsername());

        try {
            UserProfile userProfile = userService.registerUser(request);
            log.info("User registered successfully: {}", userProfile.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(jp.readscape.consumer.dto.ApiResponse.success("ユーザー登録が完了しました"));
        } catch (IllegalArgumentException e) {
            log.warn("User registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        }
    }

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
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {
        log.info("POST /api/users/login - usernameOrEmail: {}", request.getUsernameOrEmail());

        Optional<User> userOpt = userService.authenticateUser(
                request.getUsernameOrEmail(), 
                request.getPassword()
        );

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = jwtService.generateToken(user);
            UserProfile userProfile = UserProfile.from(user);
            
            LoginResponse response = LoginResponse.of(token, userProfile);
            log.info("User logged in successfully: {}", user.getUsername());
            
            return ResponseEntity.ok(response);
        } else {
            log.warn("Login failed for user: {}", request.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(jp.readscape.consumer.dto.ApiResponse.error("ユーザー名またはパスワードが正しくありません"));
        }
    }

    @Operation(
        summary = "プロフィール取得",
        description = "認証済みユーザーのプロフィール情報を取得します",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "プロフィール取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    })
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfile> getProfile(Authentication auth) {
        log.info("GET /api/users/profile - user: {}", auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            UserProfile profile = userService.getUserProfile(user.getId());
            
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.error("Profile not found for user: {}", auth.getName());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "プロフィール更新",
        description = "認証済みユーザーのプロフィール情報を更新します",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "プロフィール更新成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    })
    @PutMapping("/profile")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication auth
    ) {
        log.info("PUT /api/users/profile - user: {}", auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            UserProfile updatedProfile = userService.updateUserProfile(user.getId(), request);
            
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            log.warn("Profile update failed for user {}: {}", auth.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "注文履歴取得",
        description = "認証済みユーザーの注文履歴を取得します",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注文履歴取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    })
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummary>> getOrders(Authentication auth) {
        log.info("GET /api/users/orders - user: {}", auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            List<OrderSummary> orders = userService.getUserOrders(user.getId());
            
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            log.error("Orders not found for user: {}", auth.getName());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "ユーザー名重複チェック",
        description = "指定されたユーザー名が既に使用されているかチェックします"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "チェック完了"),
        @ApiResponse(responseCode = "409", description = "ユーザー名が既に使用されています")
    })
    @GetMapping("/check-username")
    public ResponseEntity<jp.readscape.consumer.dto.ApiResponse> checkUsername(
            @Parameter(description = "チェックするユーザー名", required = true)
            @RequestParam String username
    ) {
        log.info("GET /api/users/check-username - username: {}", username);

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(jp.readscape.consumer.dto.ApiResponse.error("ユーザー名は必須です"));
        }

        boolean exists = userService.existsByUsername(username.trim());
        
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(jp.readscape.consumer.dto.ApiResponse.error("ユーザー名が既に使用されています"));
        } else {
            return ResponseEntity.ok(jp.readscape.consumer.dto.ApiResponse.success("ユーザー名は利用可能です"));
        }
    }

    @Operation(
        summary = "メールアドレス重複チェック",
        description = "指定されたメールアドレスが既に使用されているかチェックします"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "チェック完了"),
        @ApiResponse(responseCode = "409", description = "メールアドレスが既に使用されています")
    })
    @GetMapping("/check-email")
    public ResponseEntity<jp.readscape.consumer.dto.ApiResponse> checkEmail(
            @Parameter(description = "チェックするメールアドレス", required = true)
            @RequestParam String email
    ) {
        log.info("GET /api/users/check-email - email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(jp.readscape.consumer.dto.ApiResponse.error("メールアドレスは必須です"));
        }

        boolean exists = userService.existsByEmail(email.trim());
        
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(jp.readscape.consumer.dto.ApiResponse.error("メールアドレスが既に使用されています"));
        } else {
            return ResponseEntity.ok(jp.readscape.consumer.dto.ApiResponse.success("メールアドレスは利用可能です"));
        }
    }
}