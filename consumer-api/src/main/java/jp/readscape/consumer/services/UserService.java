package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.domain.orders.repository.OrderRepository;
import jp.readscape.consumer.dto.users.LoginRequest;
import jp.readscape.consumer.dto.users.LoginResponse;
import jp.readscape.consumer.dto.users.OrderSummary;
import jp.readscape.consumer.dto.users.RegisterUserRequest;
import jp.readscape.consumer.dto.users.UpdateProfileRequest;
import jp.readscape.consumer.dto.users.UserProfile;
import jp.readscape.consumer.services.security.LoginAttemptService;
import jp.readscape.consumer.services.security.PasswordSecurityService;
import jp.readscape.consumer.services.security.SecurityAuditService;
import jp.readscape.consumer.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordSecurityService passwordSecurityService;
    private final SecurityAuditService securityAuditService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        return userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameOrEmail(username);
    }

    /**
     * ユーザー登録
     */
    @Transactional
    public UserProfile registerUser(RegisterUserRequest request) {
        log.debug("Registering new user with username: {}", request.getUsername());

        // 重複チェック
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("ユーザー名が既に使用されています: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("メールアドレスが既に使用されています: " + request.getEmail());
        }

        // パスワード強度チェック
        PasswordSecurityService.PasswordStrengthResult strengthResult = 
            passwordSecurityService.checkPasswordStrength(request.getPassword());
        if (!strengthResult.isValid()) {
            throw new IllegalArgumentException(strengthResult.getMessage());
        }

        // よくあるパスワードチェック
        if (passwordSecurityService.isCommonPassword(request.getPassword())) {
            throw new IllegalArgumentException("よく使用されるパスワードのため使用できません。より安全なパスワードを設定してください。");
        }

        // パスワードの暗号化
        String encodedPassword = passwordSecurityService.encodePassword(request.getPassword());

        // ユーザー作成
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(UserRole.CONSUMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
        securityAuditService.logUserRegistration(savedUser.getEmail(), "unknown");

        return UserProfile.from(savedUser);
    }

    /**
     * ユーザー認証
     */
    public Optional<User> authenticateUser(String usernameOrEmail, String rawPassword) {
        return authenticateUser(usernameOrEmail, rawPassword, "unknown");
    }
    
    /**
     * ユーザー認証（IPアドレス付き）
     */
    public Optional<User> authenticateUser(String usernameOrEmail, String rawPassword, String ipAddress) {
        log.debug("Authenticating user: {}", SecurityUtils.maskUserIdentifier(usernameOrEmail));

        // ログイン試行制限チェック
        if (isAccountLocked(usernameOrEmail, ipAddress)) {
            return Optional.empty();
        }

        // 実際の認証処理
        Optional<User> authenticationResult = performAuthentication(usernameOrEmail, rawPassword);
        
        // 認証結果の処理
        if (authenticationResult.isPresent()) {
            handleSuccessfulAuthentication(usernameOrEmail, authenticationResult.get(), ipAddress);
            return authenticationResult;
        } else {
            handleFailedAuthentication(usernameOrEmail, ipAddress);
            return Optional.empty();
        }
    }
    
    /**
     * アカウントロック状態をチェック
     */
    private boolean isAccountLocked(String usernameOrEmail, String ipAddress) {
        if (loginAttemptService.isBlocked(usernameOrEmail)) {
            long remainingTime = loginAttemptService.getRemainingLockTime(usernameOrEmail);
            securityAuditService.logFailedLogin(usernameOrEmail, ipAddress, 
                "Account locked due to too many failed attempts. Remaining time: " + remainingTime + " minutes");
            log.warn("Account locked for excessive login attempts: {}", SecurityUtils.maskUserIdentifier(usernameOrEmail));
            return true;
        }
        return false;
    }
    
    /**
     * 実際の認証処理を実行
     */
    private Optional<User> performAuthentication(String usernameOrEmail, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(usernameOrEmail);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.isEnabled() && passwordSecurityService.matches(rawPassword, user.getPassword())) {
                return Optional.of(user);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 認証成功時の処理
     */
    private void handleSuccessfulAuthentication(String usernameOrEmail, User user, String ipAddress) {
        log.debug("User authenticated successfully: {}", SecurityUtils.maskUserIdentifier(usernameOrEmail));
        loginAttemptService.recordSuccessfulLogin(usernameOrEmail);
        securityAuditService.logSuccessfulLogin(user.getEmail(), ipAddress);
    }
    
    /**
     * 認証失敗時の処理
     */
    private void handleFailedAuthentication(String usernameOrEmail, String ipAddress) {
        log.debug("User authentication failed: {}", SecurityUtils.maskUserIdentifier(usernameOrEmail));
        loginAttemptService.recordFailedLogin(usernameOrEmail);
        securityAuditService.logFailedLogin(usernameOrEmail, ipAddress, "Invalid credentials");
        
        // ログイン試行回数が上限に達した場合のログ
        if (loginAttemptService.isBlocked(usernameOrEmail)) {
            int loginAttempts = loginAttemptService.getAttempts(usernameOrEmail);
            securityAuditService.logAccountLocked(usernameOrEmail, ipAddress, loginAttempts);
        }
    }

    /**
     * プロフィール取得
     */
    public UserProfile getUserProfile(Long userId) {
        log.debug("Getting user profile for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        return UserProfile.from(user);
    }

    /**
     * プロフィール更新
     */
    @Transactional
    public UserProfile updateUserProfile(Long userId, UpdateProfileRequest request) {
        log.debug("Updating user profile for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        // メールアドレスの重複チェック（自分以外）
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("メールアドレスが既に使用されています: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // その他の情報更新
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        User updatedUser = userRepository.save(user);
        log.info("User profile updated successfully: {}", updatedUser.getUsername());

        return UserProfile.from(updatedUser);
    }

    /**
     * ユーザー注文履歴取得
     */
    public List<OrderSummary> getUserOrders(Long userId) {
        log.debug("Getting user orders for user id: {}", userId);

        // ユーザーの存在確認
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        // 注文履歴を取得してOrderSummaryに変換
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId)
                .stream()
                .map(OrderSummary::from)
                .toList();
    }

    /**
     * ユーザー名の存在チェック
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * メールアドレスの存在チェック
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * ユーザー検索
     */
    public List<UserProfile> searchUsers(String keyword) {
        log.debug("Searching users with keyword: {}", keyword);

        List<User> users = userRepository.findByKeyword(keyword);
        
        return users.stream()
                .map(UserProfile::from)
                .toList();
    }

    /**
     * アクティブユーザー一覧取得
     */
    public List<UserProfile> findActiveUsers() {
        log.debug("Finding all active users");

        List<User> users = userRepository.findByIsActiveTrue();
        
        return users.stream()
                .map(UserProfile::from)
                .toList();
    }

    /**
     * ユーザー無効化
     */
    @Transactional
    public void deactivateUser(Long userId) {
        log.debug("Deactivating user with id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User deactivated successfully: {}", user.getUsername());
    }
}