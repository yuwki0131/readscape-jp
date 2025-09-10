package jp.readscape.consumer.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.readscape.consumer.services.security.LoginAttemptService;
import jp.readscape.consumer.services.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "ヘルスチェックAPI")
public class HealthController implements HealthIndicator {

    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Health health() {
        return Health.up()
            .withDetail("authentication-system", "operational")
            .build();
    }

    @GetMapping("/health")
    public Map<String, Object> basicHealth() {
        return Map.of(
                "status", "UP",
                "service", "readscape-consumer-api",
                "timestamp", LocalDateTime.now(),
                "version", "1.0.0"
        );
    }

    @Operation(
        summary = "詳細ヘルスチェック",
        description = "認証システムの詳細なヘルスチェック情報を返します"
    )
    @GetMapping("/health/auth")
    public ResponseEntity<Map<String, Object>> getAuthHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // ログイン試行制限サービスの状態
            health.put("loginAttemptService", Map.of(
                "status", "UP"
            ));
            
            // トークンブラックリストサービスの状態
            TokenBlacklistService.BlacklistStats stats = tokenBlacklistService.getStats();
            health.put("tokenBlacklistService", Map.of(
                "status", "UP",
                "active_blacklisted_tokens", stats.getActiveTokens()
            ));
            
            // 総合ステータス
            health.put("overall_status", "UP");
            health.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            health.put("overall_status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }

    @Operation(
        summary = "認証システム統計情報",
        description = "認証システムの統計情報を返します"
    )
    @GetMapping("/health/auth/stats")
    public ResponseEntity<Map<String, Object>> getAuthStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // ブラックリスト統計
            TokenBlacklistService.BlacklistStats blacklistStats = tokenBlacklistService.getStats();
            stats.put("blacklisted_tokens", blacklistStats.getActiveTokens());
            
            // システム情報
            stats.put("system_info", Map.of(
                "jvm_memory_usage", getMemoryUsage(),
                "uptime", getUptime()
            ));
            
            stats.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
            return ResponseEntity.status(500).body(stats);
        }
        
        return ResponseEntity.ok(stats);
    }
    
    private Map<String, Long> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
            "total_memory", runtime.totalMemory(),
            "free_memory", runtime.freeMemory(),
            "used_memory", runtime.totalMemory() - runtime.freeMemory(),
            "max_memory", runtime.maxMemory()
        );
    }
    
    private long getUptime() {
        return System.currentTimeMillis() - 
               java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
    }
}