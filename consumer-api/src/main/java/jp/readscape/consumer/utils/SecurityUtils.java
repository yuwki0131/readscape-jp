package jp.readscape.consumer.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityUtils {
    
    /**
     * ユーザー識別子をマスキングします
     * 例: user@example.com -> u***@example.com
     *     username123 -> u***e123
     */
    public static String maskUserIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 3) {
            return "***";
        }
        
        // メールアドレスの場合
        if (identifier.contains("@")) {
            String[] parts = identifier.split("@");
            if (parts.length == 2) {
                String localPart = parts[0];
                String domain = parts[1];
                
                if (localPart.length() <= 3) {
                    return "***@" + domain;
                }
                
                return localPart.charAt(0) + "***" + "@" + domain;
            }
        }
        
        // 通常のユーザー名の場合
        if (identifier.length() <= 4) {
            return identifier.charAt(0) + "***";
        }
        
        return identifier.charAt(0) + "***" + identifier.substring(identifier.length() - 1);
    }
    
    /**
     * IPアドレスを取得します（プロキシ対応）
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        // プロキシ経由の場合の実IPアドレス取得
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-Forの最初のIPアドレスを取得
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // 他のヘッダーもチェック
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        // 最後の手段としてRemoteAddrを使用
        return request.getRemoteAddr();
    }
    
    /**
     * IPアドレスをマスキングします（プライバシー保護）
     */
    public static String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress)) {
            return "unknown";
        }
        
        // IPv4の場合 (例: 192.168.1.100 -> 192.168.***.***) 
        if (ipAddress.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***.**";
            }
        }
        
        // IPv6の場合（簡単なマスキング）
        if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            if (parts.length >= 3) {
                return parts[0] + ":" + parts[1] + ":***:***";
            }
        }
        
        return "***.***.***";
    }
    
    /**
     * User-Agentをマスキングします
     */
    public static String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        
        // 最初の30文字のみ表示
        if (userAgent.length() > 30) {
            return userAgent.substring(0, 30) + "...";
        }
        
        return userAgent;
    }
}