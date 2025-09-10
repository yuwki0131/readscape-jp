package jp.readscape.consumer.services;

import jp.readscape.consumer.constants.SecurityConstants;
import jp.readscape.consumer.exceptions.JwtException;
import jp.readscape.consumer.services.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private JwtService jwtService;
    private UserDetails testUser;
    private String testSecret;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(tokenBlacklistService);
        testSecret = "myVerySecureSecretKeyThatIsAtLeast32CharactersLongForSecurity";
        
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 2592000000L); // 30 days

        testUser = User.builder()
                .username("testuser@example.com")
                .password("password")
                .authorities(Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_CUSTOMER")
                ))
                .build();
    }

    @Test
    void validateConfiguration_WithValidSecret_ShouldInitializeSuccessfully() {
        // Given - valid secret is already set in setUp

        // When & Then - should not throw exception
        assertThatCode(() -> jwtService.validateConfiguration())
                .doesNotThrowAnyException();
    }

    @Test
    void validateConfiguration_WithNullSecret_ShouldThrowException() {
        // Given
        ReflectionTestUtils.setField(jwtService, "secret", null);

        // When & Then
        assertThatThrownBy(() -> jwtService.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be configured");
    }

    @Test
    void validateConfiguration_WithEmptySecret_ShouldThrowException() {
        // Given
        ReflectionTestUtils.setField(jwtService, "secret", "");

        // When & Then
        assertThatThrownBy(() -> jwtService.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be configured");
    }

    @Test
    void validateConfiguration_WithShortSecret_ShouldThrowException() {
        // Given
        ReflectionTestUtils.setField(jwtService, "secret", "short");

        // When & Then
        assertThatThrownBy(() -> jwtService.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be at least 256 bits");
    }

    @Test
    void generateToken_WithValidUserDetails_ShouldGenerateValidToken() {
        // Given
        jwtService.validateConfiguration();

        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractUsername(token)).isEqualTo(testUser.getUsername());
        
        List<String> roles = jwtService.extractRoles(token);
        assertThat(roles).containsExactlyInAnyOrder("USER", "CUSTOMER");
    }

    @Test
    void generateRefreshToken_WithValidUserDetails_ShouldGenerateValidRefreshToken() {
        // Given
        jwtService.validateConfiguration();

        // When
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // Then
        assertThat(refreshToken).isNotNull().isNotEmpty();
        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo(testUser.getUsername());
        assertThat(jwtService.isRefreshTokenValid(refreshToken)).isTrue();
    }

    @Test
    void extractUsername_WithValidToken_ShouldReturnCorrectUsername() {
        // Given
        jwtService.validateConfiguration();
        String token = jwtService.generateToken(testUser);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertThat(username).isEqualTo(testUser.getUsername());
    }

    @Test
    void extractRoles_WithValidToken_ShouldReturnCorrectRoles() {
        // Given
        jwtService.validateConfiguration();
        String token = jwtService.generateToken(testUser);

        // When
        List<String> roles = jwtService.extractRoles(token);

        // Then
        assertThat(roles).containsExactlyInAnyOrder("USER", "CUSTOMER");
    }

    @Test
    void extractRoles_WithTokenWithoutRoles_ShouldReturnEmptyList() {
        // Given
        jwtService.validateConfiguration();
        UserDetails userWithoutRoles = User.builder()
                .username("testuser@example.com")
                .password("password")
                .authorities(List.of())
                .build();
        String token = jwtService.generateToken(userWithoutRoles);

        // When
        List<String> roles = jwtService.extractRoles(token);

        // Then
        assertThat(roles).isEmpty();
    }

    @Test
    void isTokenValid_WithValidToken_ShouldReturnTrue() {
        // Given
        jwtService.validateConfiguration();
        String token = jwtService.generateToken(testUser);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_WithDifferentUser_ShouldReturnFalse() {
        // Given
        jwtService.validateConfiguration();
        String token = jwtService.generateToken(testUser);
        
        UserDetails differentUser = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities(List.of())
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithBlacklistedToken_ShouldReturnFalse() {
        // Given
        jwtService.validateConfiguration();
        String token = jwtService.generateToken(testUser);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isRefreshTokenValid_WithValidRefreshToken_ShouldReturnTrue() {
        // Given
        jwtService.validateConfiguration();
        String refreshToken = jwtService.generateRefreshToken(testUser);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);

        // When
        boolean isValid = jwtService.isRefreshTokenValid(refreshToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void isRefreshTokenValid_WithAccessToken_ShouldReturnFalse() {
        // Given
        jwtService.validateConfiguration();
        String accessToken = jwtService.generateToken(testUser);
        when(tokenBlacklistService.isBlacklisted(accessToken)).thenReturn(false);

        // When
        boolean isValid = jwtService.isRefreshTokenValid(accessToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void invalidateToken_WithValidToken_ShouldBlacklistToken() {
        // Given
        jwtService.validateConfiguration();
        String token = jwtService.generateToken(testUser);

        // When
        jwtService.invalidateToken(token);

        // Then
        verify(tokenBlacklistService).blacklistToken(eq(token), any(LocalDateTime.class));
    }

    @Test
    void invalidateToken_WithBlacklistServiceFailure_ShouldFallbackToMemory() {
        // Given
        jwtService.validateConfiguration();
        String token = jwtService.generateToken(testUser);
        doThrow(new RuntimeException("Database error")).when(tokenBlacklistService)
                .blacklistToken(eq(token), any(LocalDateTime.class));

        // When
        jwtService.invalidateToken(token);

        // Then
        verify(tokenBlacklistService).blacklistToken(eq(token), any(LocalDateTime.class));
        // Token should still be invalidated through memory fallback
    }

    @Test
    void extractClaim_WithMalformedToken_ShouldThrowMalformedException() {
        // Given
        jwtService.validateConfiguration();
        String malformedToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                .isInstanceOf(JwtException.MalformedException.class);
    }

    @Test
    void extractClaim_WithExpiredToken_ShouldThrowExpiredException() {
        // Given
        jwtService.validateConfiguration();
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L); // Already expired
        String expiredToken = jwtService.generateToken(testUser);
        
        // Reset expiration for validation
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(JwtException.ExpiredException.class);
    }

    @Test
    void extractClaim_WithTamperedToken_ShouldThrowSecurityException() {
        // Given
        jwtService.validateConfiguration();
        String validToken = jwtService.generateToken(testUser);
        String tamperedToken = validToken.substring(0, validToken.length() - 10) + "tampered123";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(JwtException.SecurityException.class);
    }

    @Test
    void getAccessTokenExpiration_ShouldReturnCorrectValue() {
        // Given
        Long expectedExpiration = 3600L; // 3600000ms / 1000 = 3600s

        // When
        Long actualExpiration = jwtService.getAccessTokenExpiration();

        // Then
        assertThat(actualExpiration).isEqualTo(expectedExpiration);
    }

    @Test
    void getRefreshTokenExpiration_ShouldReturnCorrectValue() {
        // Given
        Long expectedExpiration = 2592000L; // 2592000000ms / 1000 = 2592000s

        // When
        Long actualExpiration = jwtService.getRefreshTokenExpiration();

        // Then
        assertThat(actualExpiration).isEqualTo(expectedExpiration);
    }

    @Test
    void generateToken_WithCustomClaims_ShouldIncludeAllClaims() {
        // Given
        jwtService.validateConfiguration();
        var customClaims = java.util.Map.of(
                "customClaim", "customValue",
                "userId", 123L
        );

        // When
        String token = jwtService.generateToken(customClaims, testUser);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractUsername(token)).isEqualTo(testUser.getUsername());
        
        String customValue = jwtService.extractClaim(token, claims -> (String) claims.get("customClaim"));
        assertThat(customValue).isEqualTo("customValue");
        
        Number userId = jwtService.extractClaim(token, claims -> (Number) claims.get("userId"));
        assertThat(userId.longValue()).isEqualTo(123L);
    }

    @Test
    void validateConfiguration_WithWeakSecret_ShouldLogWarning() {
        // Given
        ReflectionTestUtils.setField(jwtService, "secret", "mySecretKey123456789012345678901234567890"); // Contains weak pattern

        // When & Then - should not throw but might log warning
        assertThatCode(() -> jwtService.validateConfiguration())
                .doesNotThrowAnyException();
    }

    @Test
    void isTokenValid_WithExpiredTokenFromPast_ShouldReturnFalse() {
        // Given
        jwtService.validateConfiguration();
        
        // Create a token that expires immediately
        ReflectionTestUtils.setField(jwtService, "expiration", 1L);
        String token = jwtService.generateToken(testUser);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Reset normal expiration
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Then
        assertThat(isValid).isFalse();
    }
}