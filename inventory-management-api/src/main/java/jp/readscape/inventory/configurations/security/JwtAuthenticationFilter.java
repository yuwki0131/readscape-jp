package jp.readscape.inventory.configurations.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT認証フィルター
 * HTTPリクエストからJWTトークンを抽出し、認証処理を行う
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // トークンタイプを確認（アクセストークンのみ有効）
                String tokenType = jwtTokenProvider.getTokenType(token);
                if ("access".equals(tokenType)) {
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                    if (email != null && roles != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // ロールを権限に変換
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        // 認証オブジェクトを作成
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(email, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // SecurityContextに設定
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("Successfully authenticated user: {} with roles: {}", email, roles);
                    }
                } else {
                    log.warn("Invalid token type: {}. Expected: access", tokenType);
                }
            } else if (token != null) {
                log.warn("Invalid JWT token");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            // エラー時はSecurityContextをクリア
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTPリクエストからJWTトークンを抽出する
     *
     * @param request HTTPリクエスト
     * @return JWTトークン（存在しない場合はnull）
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * フィルターを適用しないパスを指定
     * 認証不要なエンドポイントに対してはフィルターをスキップ
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 認証不要なパス
        return path.startsWith("/api/auth/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator/health") ||
               path.equals("/api/books") ||
               (path.startsWith("/api/books/") && "GET".equals(request.getMethod()));
    }
}