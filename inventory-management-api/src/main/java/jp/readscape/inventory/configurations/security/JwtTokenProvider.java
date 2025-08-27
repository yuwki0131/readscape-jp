package jp.readscape.inventory.configurations.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT トークンの生成・検証を行うプロバイダークラス
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:PT30M}") Duration accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:P7D}") Duration refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * アクセストークンを生成する
     *
     * @param email ユーザーのメールアドレス
     * @param roles ユーザーのロール一覧
     * @return JWT トークン
     */
    public String createAccessToken(String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * リフレッシュトークンを生成する
     *
     * @param email ユーザーのメールアドレス
     * @return JWT リフレッシュトークン
     */
    public String createRefreshToken(String email) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * トークンの有効性を検証する
     *
     * @param token JWT トークン
     * @return 有効な場合 true
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * トークンからメールアドレスを取得する
     *
     * @param token JWT トークン
     * @return メールアドレス
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }

    /**
     * トークンからロール一覧を取得する
     *
     * @param token JWT トークン
     * @return ロール一覧
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("roles", List.class);
    }

    /**
     * トークンの種類を取得する
     *
     * @param token JWT トークン
     * @return トークンタイプ (access/refresh)
     */
    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("type", String.class);
    }

    /**
     * トークンの有効期限を取得する
     *
     * @param token JWT トークン
     * @return 有効期限
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getExpiration();
    }

    /**
     * トークンが期限切れかどうかを確認する
     *
     * @param token JWT トークン
     * @return 期限切れの場合 true
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationFromToken(token);
        return expiration.before(new Date());
    }
}