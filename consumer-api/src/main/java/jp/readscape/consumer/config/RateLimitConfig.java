package jp.readscape.consumer.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class RateLimitConfig implements WebMvcConfigurer {

    @Value("${rate.limit.anonymous:100}")
    private int anonymousLimit;

    @Value("${rate.limit.authenticated:1000}")
    private int authenticatedLimit;

    @Value("${rate.limit.window:3600}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/actuator/**");
    }

    private class RateLimitInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String clientId = getClientIdentifier(request);
            boolean isAuthenticated = isAuthenticated(request);
            
            Bucket bucket = cache.computeIfAbsent(clientId, k -> createBucket(isAuthenticated));
            
            if (bucket.tryConsume(1)) {
                return true;
            } else {
                log.warn("Rate limit exceeded for client: {}, authenticated: {}", clientId, isAuthenticated);
                response.setStatus(429);
                response.setHeader("Content-Type", "application/json");
                try {
                    response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"リクエスト制限を超過しました\"}");
                } catch (Exception e) {
                    log.error("Error writing rate limit response", e);
                }
                return false;
            }
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 認証済みユーザーはトークンベースで識別
            return "auth_" + authHeader.substring(0, Math.min(50, authHeader.length()));
        }
        
        // 未認証ユーザーはIPアドレスで識別
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp = (xForwardedFor != null) ? xForwardedFor.split(",")[0] : request.getRemoteAddr();
        return "ip_" + clientIp;
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private Bucket createBucket(boolean isAuthenticated) {
        int limit = isAuthenticated ? authenticatedLimit : anonymousLimit;
        
        Bandwidth bandwidth = Bandwidth.classic(limit, 
                Refill.intervally(limit, Duration.ofSeconds(windowSeconds)));
        
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}