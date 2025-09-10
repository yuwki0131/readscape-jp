package jp.readscape.consumer.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jp.readscape.consumer.services.security.TokenBlacklistService;
import jp.readscape.consumer.exceptions.JwtException;
import jp.readscape.consumer.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${spring.security.jwt.secret:}")
    private String secret;

    @Value("${spring.security.jwt.expiration:3600000}") // 1時間（ミリ秒）
    private Long expiration;
    
    @Value("${spring.security.jwt.refresh-expiration:2592000000}") // 30日間（ミリ秒）
    private Long refreshExpiration;
    
    // レガシー：メモリベースのトークン無効化（TokenBlacklistServiceに移行済み）
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();
    
    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret key must be configured. Please set jwt.secret property with at least 256 bits (32 characters) of secure random data."
            );
        }
        
        if (secret.length() < SecurityConstants.MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT secret key must be at least 256 bits (" + SecurityConstants.MIN_JWT_SECRET_LENGTH + " characters) long for security. Current length: " + secret.length()
            );
        }
        
        // 弱いデフォルト値のチェック
        if (secret.contains("mySecretKey") || secret.contains("defaultSecret") || 
            secret.equals("secretkey") || secret.matches("^[a-zA-Z0-9]*$") && secret.length() < SecurityConstants.RECOMMENDED_JWT_SECRET_LENGTH) {
            log.warn("JWT secret appears to be weak. Consider using a cryptographically secure random key.");
        }
        
        log.info("JWT service initialized with secure secret key (length: {} characters)", secret.length());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> {
            Object roles = claims.get("roles");
            if (roles instanceof List) {
                return (List<String>) roles;
            }
            return Collections.emptyList();
        });
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // ユーザーのロール情報を追加
        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .toList();
        claims.put("roles", roles);
        claims.put("type", "access");
        
        return generateToken(claims, userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return Jwts
                .builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token) && !isTokenInvalidated(token);
    }
    
    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String type = (String) claims.get("type");
            return "refresh".equals(type) && !isTokenExpired(token) && !isTokenInvalidated(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.warn("JWT token security violation detected");
            throw new JwtException.SecurityException();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("JWT token has expired");
            throw new JwtException.ExpiredException();
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.debug("Malformed JWT token provided");
            throw new JwtException.MalformedException();
        } catch (Exception e) {
            log.warn("JWT token validation failed - type: {}", e.getClass().getSimpleName());
            throw new JwtException.InvalidException();
        }
    }
    
    public void invalidateToken(String token) {
        try {
            // 新しいブラックリストサービスを使用
            Date expiration = extractExpiration(token);
            LocalDateTime expirationDateTime = expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            
            tokenBlacklistService.blacklistToken(token, expirationDateTime);
            
            // レガシーサポートのためメモリベースも併用
            invalidatedTokens.add(token);
            
            log.debug("Token invalidated successfully");
        } catch (Exception e) {
            log.warn("Failed to invalidate token properly: {}", e.getMessage());
            // フォールバックとしてメモリベースのみ使用
            invalidatedTokens.add(token);
        }
    }
    
    private boolean isTokenInvalidated(String token) {
        // 新しいブラックリストサービスをチェック
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);
        
        // レガシーサポートのためメモリベースもチェック
        boolean isLegacyInvalidated = invalidatedTokens.contains(token);
        
        return isBlacklisted || isLegacyInvalidated;
    }
    
    public Long getAccessTokenExpiration() {
        return expiration / 1000; // 秒単位で返す
    }
    
    public Long getRefreshTokenExpiration() {
        return refreshExpiration / 1000; // 秒単位で返す
    }
}