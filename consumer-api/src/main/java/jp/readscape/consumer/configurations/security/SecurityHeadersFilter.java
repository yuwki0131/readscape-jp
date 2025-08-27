package jp.readscape.consumer.configurations.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // XSS対策
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        // コンテンツセキュリティポリシー
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none';"
        );
        
        // HTTPS強制（本番環境でのみ）
        String profile = System.getProperty("spring.profiles.active", "dev");
        if (!"dev".equals(profile) && !"test".equals(profile)) {
            httpResponse.setHeader("Strict-Transport-Security", 
                "max-age=31536000; includeSubDomains; preload");
        }
        
        // リファラーポリシー
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // 権限ポリシー
        httpResponse.setHeader("Permissions-Policy", 
            "accelerometer=(), " +
            "camera=(), " +
            "geolocation=(), " +
            "gyroscope=(), " +
            "magnetometer=(), " +
            "microphone=(), " +
            "payment=(), " +
            "usb=()");
        
        // SameSite Cookieの設定（CSRF対策）
        // 注意: JWTトークンベースの認証を使用しているため、現在はCookieを使用していない
        // 将来的にCookieを使用する場合のための設定
        String cookieHeader = httpResponse.getHeader("Set-Cookie");
        if (cookieHeader != null && !cookieHeader.contains("SameSite")) {
            httpResponse.setHeader("Set-Cookie", cookieHeader + "; SameSite=Strict");
        }
        
        chain.doFilter(request, response);
    }
}