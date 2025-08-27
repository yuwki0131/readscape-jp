package jp.readscape.inventory.services;

import jp.readscape.inventory.configurations.security.JwtTokenProvider;
import jp.readscape.inventory.domain.users.model.User;
import jp.readscape.inventory.domain.users.repository.UserRepository;
import jp.readscape.inventory.dto.auth.LoginRequest;
import jp.readscape.inventory.dto.auth.LoginResponse;
import jp.readscape.inventory.dto.auth.RefreshTokenRequest;
import jp.readscape.inventory.dto.auth.RefreshTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 認証サービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * ユーザーログイン処理
     *
     * @param request ログインリクエスト
     * @return ログインレスポンス
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // ユーザーの存在確認
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + request.getEmail()));

        // アクティブユーザーかチェック
        if (!user.isEnabled()) {
            log.warn("Login attempt for inactive user: {}", request.getEmail());
            throw new BadCredentialsException("アカウントが無効です");
        }

        // パスワード検証
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
        }

        // 最終ログイン日時を更新（実際の実装では別テーブルに記録することを推奨）
        updateLastLoginTime(user);

        // JWTトークン生成
        List<String> roles = List.of(user.getRole().getAuthority());
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        log.info("Successful login for user: {}", request.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(1800) // 30分（実際の値は設定から取得）
                .user(buildUserInfo(user))
                .build();
    }

    /**
     * リフレッシュトークンによるトークン更新
     *
     * @param request リフレッシュトークンリクエスト
     * @return リフレッシュトークンレスポンス
     */
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // リフレッシュトークンの検証
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Invalid refresh token");
            throw new BadCredentialsException("無効なリフレッシュトークンです");
        }

        // トークンタイプの確認
        if (!"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            log.warn("Token type is not refresh");
            throw new BadCredentialsException("リフレッシュトークンではありません");
        }

        // ユーザー情報を取得
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("アカウントが無効です");
        }

        // 新しいトークンを生成
        List<String> roles = List.of(user.getRole().getAuthority());
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), roles);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        log.info("Token refreshed for user: {}", email);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(1800) // 30分
                .build();
    }

    /**
     * ユーザー情報の構築
     */
    private LoginResponse.UserInfo buildUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .roleDescription(user.getRole().getDescription())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    /**
     * 最終ログイン時刻を更新
     * 実際の実装では、ログイン履歴テーブルを作成することを推奨
     */
    private void updateLastLoginTime(User user) {
        // 現在の実装では何もしない
        // 実際の実装では、login_history テーブルに記録
        log.debug("Last login time updated for user: {}", user.getEmail());
    }
}