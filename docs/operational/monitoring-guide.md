# Readscape-JP 監視・運用ガイド

## 概要

Readscape-JPシステムの安定稼働を確保するための包括的な監視戦略、アラート設定、運用手順について説明します。24/7の安定したサービス提供を目的とした監視体制を構築します。

## 監視アーキテクチャ

### システム監視スタック

```mermaid
graph TB
    subgraph "Applications"
        App1[Consumer API<br/>:8080]
        App2[Inventory API<br/>:8081]
        DB[(PostgreSQL<br/>:5432)]
        Redis[(Redis<br/>:6379)]
    end

    subgraph "Metrics Collection"
        Prometheus[Prometheus<br/>:9090]
        Node[Node Exporter<br/>:9100]
        Postgres[Postgres Exporter<br/>:9187]
        Redis[Redis Exporter<br/>:9121]
    end

    subgraph "Monitoring & Alerting"
        Grafana[Grafana<br/>:3000]
        AlertManager[Alert Manager<br/>:9093]
    end

    subgraph "Log Management"
        Loki[Loki<br/>:3100]
        Promtail[Promtail]
    end

    subgraph "Notifications"
        Slack[Slack Webhooks]
        Email[Email SMTP]
        PagerDuty[PagerDuty API]
    end

    App1 --> Prometheus
    App2 --> Prometheus
    DB --> Postgres
    Redis --> Redis
    
    Node --> Prometheus
    Postgres --> Prometheus
    Redis --> Prometheus
    
    Prometheus --> Grafana
    Prometheus --> AlertManager
    
    App1 --> Loki
    App2 --> Loki
    
    AlertManager --> Slack
    AlertManager --> Email
    AlertManager --> PagerDuty
```

## メトリクス定義

### 1. アプリケーションメトリクス

#### Spring Boot Actuator設定

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      show-components: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        step: PT1M
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 100ms,200ms,500ms,1s,2s
```

#### カスタムメトリクス

```java
// MetricsConfiguration.java
@Configuration
public class MetricsConfiguration {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
}

// BookService.java - ビジネスメトリクス
@Service
public class BookService {
    
    private final Counter bookSearchCounter;
    private final Timer bookSearchTimer;
    private final Gauge stockLevelGauge;
    
    public BookService(MeterRegistry meterRegistry) {
        this.bookSearchCounter = Counter.builder("book.search.count")
            .description("Number of book searches")
            .tag("type", "keyword")
            .register(meterRegistry);
            
        this.bookSearchTimer = Timer.builder("book.search.duration")
            .description("Book search duration")
            .register(meterRegistry);
            
        this.stockLevelGauge = Gauge.builder("book.stock.level")
            .description("Current stock levels")
            .register(meterRegistry, this, BookService::getTotalStockLevel);
    }
    
    @Timed(name = "book.search", description = "Time taken to search books")
    public List<Book> searchBooks(String keyword) {
        bookSearchCounter.increment();
        return Timer.Sample.start(bookSearchTimer)
            .stop(() -> bookRepository.findByKeyword(keyword));
    }
    
    private Double getTotalStockLevel() {
        return bookRepository.getTotalStockQuantity().doubleValue();
    }
}
```

### 2. インフラメトリクス

#### システムリソース監視

```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/rules:/etc/prometheus/rules
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin_password
    volumes:
      - grafana-storage:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources

  node-exporter:
    image: prom/node-exporter:latest
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'

  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:latest
    environment:
      DATA_SOURCE_NAME: "postgresql://monitor_user:monitor_password@postgres:5432/readscape?sslmode=disable"
    ports:
      - "9187:9187"

volumes:
  grafana-storage:
```

## アラート設定

### 1. 重要度別アラート

#### Critical アラート（即座対応）

```yaml
# monitoring/rules/critical-alerts.yml
groups:
- name: critical-alerts
  rules:
  - alert: ServiceDown
    expr: up{job=~"readscape-.*"} == 0
    for: 1m
    labels:
      severity: critical
      team: platform
    annotations:
      summary: "Service {{ $labels.job }} is down"
      description: "Service {{ $labels.job }} on {{ $labels.instance }} has been down for more than 1 minute"
      runbook_url: "https://docs.readscape.jp/runbooks/service-down"
      
  - alert: DatabaseConnectionPool
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
    for: 2m
    labels:
      severity: critical
      team: platform
    annotations:
      summary: "Database connection pool nearly exhausted"
      description: "Connection pool usage is {{ $value | humanizePercentage }}"
      
  - alert: HighErrorRate
    expr: |
      (
        sum(rate(http_requests_total{status=~"5.."}[5m])) /
        sum(rate(http_requests_total[5m]))
      ) > 0.05
    for: 3m
    labels:
      severity: critical
      team: backend
    annotations:
      summary: "High 5xx error rate"
      description: "5xx error rate is {{ $value | humanizePercentage }}"
      
  - alert: DiskSpaceUsage
    expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100 < 10
    for: 5m
    labels:
      severity: critical
      team: platform
    annotations:
      summary: "Low disk space"
      description: "Disk space usage is above 90% on {{ $labels.instance }}"
```

#### Warning アラート（計画的対応）

```yaml
# monitoring/rules/warning-alerts.yml
groups:
- name: warning-alerts
  rules:
  - alert: HighResponseTime
    expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.5
    for: 5m
    labels:
      severity: warning
      team: backend
    annotations:
      summary: "High API response time"
      description: "95th percentile response time is {{ $value }}s"
      
  - alert: HighMemoryUsage
    expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) > 0.8
    for: 10m
    labels:
      severity: warning
      team: platform
    annotations:
      summary: "High memory usage"
      description: "Memory usage is {{ $value | humanizePercentage }}"
      
  - alert: LowStockAlert
    expr: book_stock_level < 10
    for: 0m
    labels:
      severity: warning
      team: business
    annotations:
      summary: "Low stock level detected"
      description: "Book {{ $labels.book_title }} has only {{ $value }} items in stock"
```

### 2. 通知設定

#### Slack通知設定

```yaml
# monitoring/alertmanager.yml
global:
  slack_api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 5m
  repeat_interval: 1h
  receiver: 'default-receiver'
  routes:
  - match:
      severity: critical
    receiver: 'critical-alerts'
    group_wait: 0s
    repeat_interval: 5m
  - match:
      severity: warning
    receiver: 'warning-alerts'
    repeat_interval: 30m

receivers:
- name: 'default-receiver'
  slack_configs:
  - channel: '#readscape-alerts'
    title: 'Readscape-JP Alert'
    text: |
      {{ range .Alerts }}
      *Alert:* {{ .Annotations.summary }}
      *Description:* {{ .Annotations.description }}
      *Severity:* {{ .Labels.severity }}
      {{ end }}

- name: 'critical-alerts'
  slack_configs:
  - channel: '#readscape-critical'
    title: '🚨 CRITICAL: Readscape-JP'
    text: |
      <!channel>
      {{ range .Alerts }}
      *🔥 CRITICAL ALERT*
      *Summary:* {{ .Annotations.summary }}
      *Description:* {{ .Annotations.description }}
      *Runbook:* {{ .Annotations.runbook_url }}
      {{ end }}
  pagerduty_configs:
  - service_key: 'YOUR_PAGERDUTY_SERVICE_KEY'
    description: "{{ .GroupLabels.alertname }}: {{ .CommonAnnotations.summary }}"

- name: 'warning-alerts'
  slack_configs:
  - channel: '#readscape-warnings'
    title: '⚠️ Warning: Readscape-JP'
    text: |
      {{ range .Alerts }}
      *⚠️ Warning Alert*
      *Summary:* {{ .Annotations.summary }}
      *Description:* {{ .Annotations.description }}
      {{ end }}
```

## ログ管理

### 1. 構造化ログ設定

#### Logback設定（本番環境）

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="prod">
        <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/readscape/application.json</file>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                    <pattern>
                        <pattern>
                            {
                                "service": "consumer-api",
                                "version": "${app.version:-unknown}",
                                "environment": "${spring.profiles.active:-unknown}",
                                "host": "${HOSTNAME:-unknown}"
                            }
                        </pattern>
                    </pattern>
                </providers>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/readscape/application.%d{yyyy-MM-dd}.%i.json.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>10GB</totalSizeCap>
            </rollingPolicy>
        </appender>
        
        <appender name="AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/readscape/audit.json</file>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <message/>
                    <mdc/>
                </providers>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/readscape/audit.%d{yyyy-MM-dd}.json.gz</fileNamePattern>
                <maxHistory>365</maxHistory>
            </rollingPolicy>
        </appender>
        
        <logger name="AUDIT" level="INFO" additivity="false">
            <appender-ref ref="AUDIT_FILE"/>
        </logger>
        
        <root level="INFO">
            <appender-ref ref="JSON_FILE"/>
        </root>
    </springProfile>
</configuration>
```

#### 構造化ログ実装

```java
// AuditLogger.java
@Component
@Slf4j
public class AuditLogger {
    
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");
    
    public void logUserAction(String action, Long userId, String resource, Object details) {
        try {
            MDC.put("action", action);
            MDC.put("userId", userId != null ? userId.toString() : "anonymous");
            MDC.put("resource", resource);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("sessionId", getSessionId());
            MDC.put("ipAddress", getClientIP());
            
            ObjectMapper mapper = new ObjectMapper();
            String detailsJson = mapper.writeValueAsString(details);
            
            AUDIT_LOGGER.info("User action executed: {}", detailsJson);
            
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        } finally {
            MDC.clear();
        }
    }
    
    // 使用例
    @PostMapping("/books")
    public ResponseEntity<Book> createBook(@RequestBody CreateBookRequest request) {
        Book book = bookService.createBook(request);
        
        auditLogger.logUserAction(
            "CREATE_BOOK",
            getCurrentUserId(),
            "books",
            Map.of(
                "bookId", book.getId(),
                "title", book.getTitle(),
                "isbn", book.getIsbn()
            )
        );
        
        return ResponseEntity.ok(book);
    }
}
```

### 2. ログ分析・検索

#### Loki設定

```yaml
# loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
    final_sleep: 0s

schema_config:
  configs:
    - from: 2024-01-01
      store: boltdb
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb:
    directory: /loki/index
  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 0s

table_manager:
  retention_deletes_enabled: false
  retention_period: 0s
```

#### ログクエリ例

```logql
# エラーログの検索
{service="consumer-api"} |= "ERROR" | json | line_format "{{.timestamp}} {{.level}} {{.message}}"

# 特定ユーザーのアクション追跡
{service="consumer-api"} | json | userId="12345" | line_format "{{.timestamp}} {{.action}} {{.resource}}"

# 高レスポンス時間のリクエスト
{service="consumer-api"} | json | duration > 1000 | line_format "{{.timestamp}} {{.method}} {{.path}} {{.duration}}ms"

# 認証失敗の検索
{service="consumer-api"} | json | action="LOGIN_FAILURE" | line_format "{{.timestamp}} {{.ipAddress}} {{.details}}"
```

## ダッシュボード設計

### 1. 運用ダッシュボード（SRE向け）

```json
{
  "dashboard": {
    "title": "Readscape-JP Operations Dashboard",
    "tags": ["readscape", "operations"],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "title": "Service Health",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"readscape-.*\"}",
            "legendFormat": "{{ job }}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{ method }} {{ handler }}"
          }
        ]
      },
      {
        "title": "Response Time Percentiles", 
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "p50"
          },
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "p95"
          },
          {
            "expr": "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "p99"
          }
        ]
      },
      {
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active Connections"
          },
          {
            "expr": "hikaricp_connections_max",
            "legendFormat": "Max Connections"
          }
        ]
      },
      {
        "title": "JVM Memory Usage",
        "type": "graph", 
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Heap Max"
          }
        ]
      }
    ]
  }
}
```

### 2. ビジネスダッシュボード

```json
{
  "dashboard": {
    "title": "Readscape-JP Business Metrics",
    "panels": [
      {
        "title": "Daily Orders",
        "type": "stat",
        "targets": [
          {
            "expr": "increase(orders_total[24h])",
            "legendFormat": "Orders Today"
          }
        ]
      },
      {
        "title": "Revenue Trend",
        "type": "graph",
        "targets": [
          {
            "expr": "increase(revenue_total[1h])",
            "legendFormat": "Hourly Revenue"
          }
        ]
      },
      {
        "title": "Top Selling Books",
        "type": "table",
        "targets": [
          {
            "expr": "topk(10, increase(book_sales_total[24h]))",
            "format": "table"
          }
        ]
      }
    ]
  }
}
```

## 運用手順書

### 1. 日次運用チェックリスト

```markdown
# 日次運用チェックリスト

## システム健全性確認（毎朝9:00）

### インフラ確認
- [ ] すべてのサービスが稼働中か確認
- [ ] CPU使用率 < 70%
- [ ] メモリ使用率 < 80%  
- [ ] ディスク使用率 < 80%
- [ ] データベース接続正常

### アプリケーション確認
- [ ] API レスポンス時間正常（P95 < 500ms）
- [ ] エラー率正常（< 1%）
- [ ] 前日の注文処理完了
- [ ] バックアップ正常完了

### セキュリティ確認
- [ ] 異常なアクセスパターンなし
- [ ] 認証失敗率正常
- [ ] セキュリティアップデート確認

### ビジネス指標確認
- [ ] 前日の売上・注文数確認
- [ ] 新規ユーザー登録数確認
- [ ] カスタマーサポート問い合わせ状況
```

### 2. 週次運用タスク

```bash
#!/bin/bash
# 週次運用スクリプト

echo "=== Readscape-JP Weekly Maintenance ==="

# 1. ログローテーション確認
echo "Checking log rotation..."
du -sh /var/log/readscape/
find /var/log/readscape/ -name "*.gz" -mtime +30 -delete

# 2. データベースメンテナンス
echo "Database maintenance..."
docker-compose exec postgres psql -U postgres -d readscape -c "
    ANALYZE;
    REINDEX DATABASE readscape;
    VACUUM ANALYZE;
"

# 3. 古いバックアップの削除
echo "Cleaning old backups..."
find /backups/readscape/ -name "*.sql" -mtime +30 -delete

# 4. セキュリティスキャン
echo "Security scan..."
docker run --rm -v $(pwd):/app securecodewarrior/docker-security-scan /app

# 5. 依存関係の脆弱性チェック
echo "Dependency vulnerability check..."
./gradlew dependencyCheckAnalyze

# 6. パフォーマンステスト
echo "Performance test..."
artillery run performance-tests/load-test.yml

# 7. レポート生成
echo "Generating weekly report..."
python scripts/generate-weekly-report.py

echo "Weekly maintenance completed!"
```

### 3. 月次運用タスク

```bash
#!/bin/bash
# 月次運用スクリプト

echo "=== Readscape-JP Monthly Operations ==="

# 1. 容量計画
echo "Capacity planning analysis..."
python scripts/capacity-analysis.py --period=30d

# 2. コスト分析
echo "Cost analysis..."
aws ce get-cost-and-usage \
    --time-period Start=$(date -d "1 month ago" +%Y-%m-01),End=$(date +%Y-%m-01) \
    --granularity MONTHLY \
    --metrics BlendedCost

# 3. セキュリティ監査
echo "Security audit..."
./scripts/security-audit.sh

# 4. パフォーマンス分析
echo "Performance trend analysis..."
python scripts/performance-analysis.py --period=30d

# 5. データベース統計更新
echo "Updating database statistics..."
docker-compose exec postgres psql -U postgres -d readscape -c "
    UPDATE pg_stat_user_tables SET n_mod_since_analyze = 0;
    ANALYZE;
"

echo "Monthly operations completed!"
```

## インシデント対応手順

### 1. インシデント分類

```yaml
インシデントレベル:
  P0_Critical:
    定義: 全サービス停止、データ損失リスク
    対応時間: 15分以内に対応開始
    エスカレーション: 即座にCTO、CEO通知
    
  P1_High:
    定義: 主要機能停止、大幅な性能劣化
    対応時間: 1時間以内に対応開始
    エスカレーション: 開発リーダー、事業責任者通知
    
  P2_Medium:
    定義: 一部機能停止、軽微な性能問題
    対応時間: 4時間以内に対応開始
    エスカレーション: 開発チーム内で対応
    
  P3_Low:
    定義: 非クリティカルな問題
    対応時間: 24時間以内に対応開始
    エスカレーション: 定期会議で報告
```

### 2. インシデント対応フロー

```mermaid
graph TD
    A[アラート受信] --> B[初期評価]
    B --> C{優先度判定}
    
    C -->|P0| D[即座にエスカレーション]
    C -->|P1| E[チームリーダー通知]
    C -->|P2-P3| F[担当者割り当て]
    
    D --> G[戦争室設置]
    E --> H[調査・分析]
    F --> H
    G --> H
    
    H --> I[原因特定]
    I --> J[修正実装]
    J --> K[テスト・検証]
    K --> L[本番適用]
    L --> M[復旧確認]
    M --> N[事後分析]
    N --> O[改善計画]
```

### 3. 復旧手順

#### データベース復旧

```bash
#!/bin/bash
# データベース緊急復旧スクリプト

set -e

BACKUP_FILE=$1
if [ -z "$BACKUP_FILE" ]; then
    echo "最新のバックアップファイルを検索中..."
    BACKUP_FILE=$(find /backups/readscape -name "*.sql" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2)
fi

echo "=== Database Recovery Process ==="
echo "Using backup: $BACKUP_FILE"

# 1. アプリケーション停止
echo "Stopping applications..."
kubectl scale deployment consumer-api --replicas=0
kubectl scale deployment inventory-api --replicas=0

# 2. データベース接続終了
echo "Terminating database connections..."
docker-compose exec postgres psql -U postgres -c "
    SELECT pg_terminate_backend(pid) 
    FROM pg_stat_activity 
    WHERE datname = 'readscape' AND pid <> pg_backend_pid();
"

# 3. データベース復旧
echo "Restoring database..."
docker-compose exec postgres dropdb -U postgres readscape
docker-compose exec postgres createdb -U postgres readscape
docker-compose exec postgres pg_restore -U postgres -d readscape < "$BACKUP_FILE"

# 4. 整合性チェック
echo "Checking database integrity..."
docker-compose exec postgres psql -U postgres -d readscape -c "
    SELECT schemaname, tablename, attname, n_distinct, correlation 
    FROM pg_stats 
    WHERE schemaname = 'public'
    ORDER BY tablename, attname;
"

# 5. アプリケーション起動
echo "Starting applications..."
kubectl scale deployment consumer-api --replicas=3
kubectl scale deployment inventory-api --replicas=2

# 6. ヘルスチェック
echo "Waiting for applications to start..."
kubectl wait --for=condition=available deployment/consumer-api --timeout=300s
kubectl wait --for=condition=available deployment/inventory-api --timeout=300s

# 7. 機能テスト
echo "Running smoke tests..."
./scripts/smoke-test.sh

echo "Database recovery completed successfully!"
```

## パフォーマンス監視

### 1. APM（Application Performance Monitoring）

#### Java APM設定（New Relic例）

```yaml
# newrelic.yml
common: &default_settings
  license_key: '<%= license_key %>'
  app_name: 'Readscape-JP Consumer API'
  
  distributed_tracing:
    enabled: true
    
  application_logging:
    enabled: true
    forwarding:
      enabled: true
    local_decorating:
      enabled: true

production:
  <<: *default_settings
  
development:
  <<: *default_settings
  app_name: 'Readscape-JP Consumer API (Development)'
```

#### JVM監視

```java
// JvmMetricsConfiguration.java
@Configuration
public class JvmMetricsConfiguration {
    
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }
    
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }
    
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }
    
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
```

### 2. データベース監視

#### PostgreSQL監視設定

```sql
-- 監視用ビューの作成
CREATE VIEW pg_stat_statements_summary AS
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements
ORDER BY total_time DESC;

-- スロークエリ監視
CREATE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_time / calls as avg_time_ms,
    rows / calls as avg_rows
FROM pg_stat_statements
WHERE total_time / calls > 1000  -- 1秒以上のクエリ
ORDER BY avg_time_ms DESC;

-- 接続数監視
CREATE VIEW connection_stats AS
SELECT 
    state,
    COUNT(*) as connection_count
FROM pg_stat_activity
WHERE datname = 'readscape'
GROUP BY state;
```

この包括的な監視・運用ガイドにより、Readscape-JPシステムの安定した24/7運用を実現できます。