package jp.readscape.consumer.configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.readscape.consumer.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Authorizationヘッダーのチェック
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // JWTトークンの抽出
        jwt = authHeader.substring(7);
        
        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            log.debug("JWT token extraction failed: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ユーザー認証（スレッドセーフティ強化）
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // UserDetailsの取得をキャッシュして重複ロードを防ぐ
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // SecurityContextの重複チェック（ダブルチェック）
                    SecurityContext context = SecurityContextHolder.getContext();
                    if (context.getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        context.setAuthentication(authToken);

                        log.debug("JWT authentication successful for user: {}", username);
                    }
                }
            } catch (Exception e) {
                log.debug("JWT authentication failed for user {}: {}", username, e.getMessage());
                // 認証失敗時はSecurityContextをクリア
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}