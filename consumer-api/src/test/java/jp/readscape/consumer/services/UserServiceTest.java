package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.dto.auth.LoginRequest;
import jp.readscape.consumer.dto.auth.LoginResponse;
import jp.readscape.consumer.dto.users.RegisterUserRequest;
import jp.readscape.consumer.dto.users.UserProfile;
import jp.readscape.consumer.exceptions.UserNotFoundException;
import jp.readscape.consumer.services.security.SecurityAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService テスト")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("ユーザー登録 - 成功")
    void registerUser_Success() {
        // Given
        RegisterUserRequest registerRequest = RegisterUserRequest.builder()
                .usernameOrEmail("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        User savedUser = createTestUser(1L, "testuser", "test@example.com", "encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserProfile result = userService.registerUser(registerRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(securityAuditService).logUserRegistration(eq("test@example.com"), anyString());
    }

    @Test
    @DisplayName("ユーザー登録 - ユーザー名重複")
    void registerUser_DuplicateUsername() {
        // Given
        RegisterUserRequest registerRequest = RegisterUserRequest.builder()
                .username("existinguser")
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に使用されているユーザー名です");

        verify(userRepository).existsByUsername("existinguser");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("ユーザー登録 - メールアドレス重複")
    void registerUser_DuplicateEmail() {
        // Given
        RegisterUserRequest registerRequest = RegisterUserRequest.builder()
                .usernameOrEmail("testuser")
                .email("existing@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に使用されているメールアドレスです");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("existing@example.com");
    }

    @Test
    @DisplayName("ログイン - 成功")
    void authenticateUser_Success() {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("password123")
                .build();

        User user = createTestUser(1L, "testuser", "test@example.com", "encodedPassword");
        Authentication authentication = mock(Authentication.class);

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.authenticateUser(loginRequest.getUsernameOrEmail(), loginRequest.getPassword());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findByUsernameOrEmail("testuser");
    }

    @Test
    @DisplayName("ログイン - 認証失敗")
    void authenticateUser_BadCredentials() {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When
        Optional<User> result = userService.authenticateUser(loginRequest.getUsernameOrEmail(), loginRequest.getPassword());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ユーザー名でユーザー検索 - 成功")
    void findByUsername_Success() {
        // Given
        String username = "testuser";
        User user = createTestUser(1L, username, "test@example.com", "encodedPassword");
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByUsername(username);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);

        verify(userRepository).findByUsernameOrEmail(username);
    }

    @Test
    @DisplayName("ユーザー名でユーザー検索 - 見つからない")
    void findByUsername_NotFound() {
        // Given
        String username = "nonexistentuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findByUsername(username))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found: " + username);

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("メールアドレスでユーザー検索 - 成功")
    void findByEmail_Success() {
        // Given
        String email = "test@example.com";
        User user = createTestUser(1L, "testuser", email, "encodedPassword");
        when(userRepository.findByUsernameOrEmail(email)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByUsername(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);

        verify(userRepository).findByUsernameOrEmail(email);
    }

    @Test
    @DisplayName("ユーザープロフィール取得 - 成功")
    void getUserProfile_Success() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, "testuser", "test@example.com", "encodedPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserProfile result = userService.getUserProfile(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo("CONSUMER");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("ユーザー名存在チェック - 存在する")
    void existsByUsername_True() {
        // Given
        String username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When
        boolean result = userService.existsByUsername(username);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("ユーザー名存在チェック - 存在しない")
    void existsByUsername_False() {
        // Given
        String username = "newuser";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // When
        boolean result = userService.existsByUsername(username);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("メールアドレス存在チェック - 存在する")
    void existsByEmail_True() {
        // Given
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        boolean result = userService.existsByEmail(email);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    // Helper method
    private User createTestUser(Long id, String username, String email, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setRole(UserRole.CONSUMER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}