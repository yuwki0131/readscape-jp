# Readscape-JP KPI¡ûÁêİ<¬¤É

## ‚

Readscape-JP×í¸§¯ÈnÁê
h™š„9„’î„hW_KPIÍm>U¡	nš©,š¡k¢Y‹¬¤Éé¤ógY‹zÁüà×íÀ¯ÈÁüàK(ÁüàLqngœ’,šWÇü¿ÆÕ‹nzš’LD~Y

## KPIÕìüàïü¯

### 1. KPI^Sû

```mermaid
graph TB
    subgraph "Ó¸Í¹KPI"
        B1[ò
ûÎÊ]
        B2[æü¶ür—]
        B3[g¢€³¦]
        B4[4·§¢]
    end
    
    subgraph "×íÀ¯ÈKPI"
        P1[_ı)(‡]
        P2[æü¶ü¨ó²ü¸áóÈ]
        P3[³óĞü¸çó‡]
        P4[êÆó·çó‡]
    end
    
    subgraph "€SKPI"
        T1[·¹Æà'ı]
        T2[ï(']
        T3[»­åêÆ£]
        T4[³üÉÁê]
    end
    
    subgraph "K(KPI"
        O1[¤ó·ÇóÈşÜ]
        O2[Ç×í¤áóÈ]
        O3[ã–û¢éüÈ]
        O4[³¹È¹‡]
    end
```

## Ó¸Í¹KPI

### 1. ò
ûÎÊ

#### ;
- **!ò
ØMRR: Monthly Recurring Revenue	**
  - î$: MÔ5%w
  - ,š¹Õ: “ºšè‡nÏM
  - Çü¿½ü¹: orders ÆüÖëstatus='DELIVERED'	

- **sGè‡¡MAOV: Average Order Value	**
  - î$: ¥3,500å

  - ,š¹Õ: Ïò
 ÷ è‡p
  - Çü¿½ü¹: orders.total_amount

- **g¢¯¡$LTV: Lifetime Value	**
  - î$: ¥15,000å

  - —: (sGè‡¡M × üe;¦ × g¢™š“) - g¢r—³¹È
  - ô°;¦: !

#### ,šSQL‹
```sql
-- !ò
Ø
SELECT 
    DATE_TRUNC('month', order_date) as month,
    SUM(total_amount) as monthly_revenue,
    COUNT(*) as order_count,
    AVG(total_amount) as average_order_value
FROM orders 
WHERE status = 'DELIVERED'
    AND order_date >= DATE_TRUNC('year', CURRENT_DATE)
GROUP BY DATE_TRUNC('month', order_date)
ORDER BY month;

-- g¢¯¡$
WITH customer_metrics AS (
    SELECT 
        user_id,
        COUNT(*) as order_count,
        SUM(total_amount) as total_spent,
        AVG(total_amount) as avg_order_value,
        MAX(order_date) - MIN(order_date) as customer_lifespan_days
    FROM orders 
    WHERE status = 'DELIVERED'
    GROUP BY user_id
)
SELECT 
    AVG(total_spent) as avg_ltv,
    AVG(avg_order_value) as avg_aov,
    AVG(order_count) as avg_orders_per_customer,
    AVG(customer_lifespan_days) as avg_lifespan_days
FROM customer_metrics;
```

### 2. æü¶ür—ûw

#### ;
- **“¢¯Æ£Öæü¶üMAU: Monthly Active Users	**
  - î$: 10,000æü¶üå

  - š©: …k1Şå
í°¤óW_æü¶üp

- **°æü¶ür—‡**
  - î$: “300æü¶üå

  - ,š¹Õ: “°{2æü¶üp

- **æü¶ü™š‡êÆó·çó‡	**
  - 1åŒ: 70%å

  - 7åŒ: 40%å
  
  - 30åŒ: 20%å


#### ,š¯¨ê
```sql
-- MAU—
SELECT 
    DATE_TRUNC('month', last_login_at) as month,
    COUNT(DISTINCT id) as monthly_active_users
FROM users 
WHERE last_login_at >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '12 months'
GROUP BY DATE_TRUNC('month', last_login_at)
ORDER BY month;

-- ™š‡—³ÛüÈ	
WITH cohorts AS (
    SELECT 
        user_id,
        DATE_TRUNC('month', created_at) as cohort_month
    FROM users
),
user_activities AS (
    SELECT 
        user_id,
        DATE_TRUNC('month', last_login_at) as activity_month
    FROM users
    WHERE last_login_at IS NOT NULL
)
SELECT 
    c.cohort_month,
    COUNT(DISTINCT c.user_id) as cohort_size,
    COUNT(DISTINCT CASE WHEN ua.activity_month = c.cohort_month THEN c.user_id END) as month_0,
    COUNT(DISTINCT CASE WHEN ua.activity_month = c.cohort_month + INTERVAL '1 month' THEN c.user_id END) as month_1,
    COUNT(DISTINCT CASE WHEN ua.activity_month = c.cohort_month + INTERVAL '2 months' THEN c.user_id END) as month_2
FROM cohorts c
LEFT JOIN user_activities ua ON c.user_id = ua.user_id
WHERE c.cohort_month >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '6 months'
GROUP BY c.cohort_month
ORDER BY c.cohort_month;
```

## ×íÀ¯ÈKPI

### 1. _ı)(‡

#### ;
- **øM")(‡**
  - î$: MAUn80%å

  - ,š: “"ŸLæü¶üp / MAU

- **«üÈ)(‡**
  - î$: MAUn40%å

  - ,š: “«üÈ)(æü¶üp / MAU

- **ìÓåü•?‡**
  - î$: üen15%å

  - ,š: ìÓåü•?p / üeŒ†p

#### ,šÀÃ·åÜüÉ
```sql
-- _ı)(‡ÀÃ·åÜüÉ
WITH monthly_users AS (
    SELECT COUNT(DISTINCT id) as mau
    FROM users 
    WHERE last_login_at >= DATE_TRUNC('month', CURRENT_DATE)
),
feature_usage AS (
    SELECT 
        'search' as feature,
        COUNT(DISTINCT user_id) as users
    FROM search_logs 
    WHERE created_at >= DATE_TRUNC('month', CURRENT_DATE)
    
    UNION ALL
    
    SELECT 
        'cart' as feature,
        COUNT(DISTINCT c.user_id) as users
    FROM carts c
    JOIN cart_items ci ON c.user_id = ci.cart_id
    WHERE ci.added_at >= DATE_TRUNC('month', CURRENT_DATE)
    
    UNION ALL
    
    SELECT 
        'review' as feature,
        COUNT(DISTINCT user_id) as users
    FROM reviews 
    WHERE created_at >= DATE_TRUNC('month', CURRENT_DATE)
)
SELECT 
    fu.feature,
    fu.users as feature_users,
    mu.mau,
    ROUND(fu.users * 100.0 / mu.mau, 2) as usage_rate_percent
FROM feature_usage fu
CROSS JOIN monthly_users mu;
```

### 2. ³óĞü¸çó‡

#### ³óĞü¸çóÕ¡Íë
```mermaid
graph TD
    A[*O] -->|{2‡ 5%| B[{2æü¶ü]
    B -->|øM²§‡ 70%| C[øM²§]
    C -->|«üÈı ‡ 25%| D[«üÈı ]
    D -->|üe‡ 60%| E[üeŒ†]
    E -->|êÔüÈüe‡ 40%| F[êÔüÈüe]
```

#### ;
- ***O’{2³óĞü¸çó‡**: 5%å

- **²§’«üÈı ‡**: 25%å

- **«üÈ’üe‡**: 60%å

- **Ş’êÔüÈüe‡**: 40%å


```sql
-- ³óĞü¸çóÕ¡Íë
WITH funnel_data AS (
    SELECT 
        COUNT(DISTINCT v.user_id) as visitors,
        COUNT(DISTINCT u.id) as registered_users,
        COUNT(DISTINCT CASE WHEN bl.user_id IS NOT NULL THEN u.id END) as book_viewers,
        COUNT(DISTINCT CASE WHEN c.user_id IS NOT NULL THEN u.id END) as cart_users,
        COUNT(DISTINCT CASE WHEN o.user_id IS NOT NULL THEN u.id END) as purchasers
    FROM (SELECT DISTINCT user_id FROM page_views WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') v
    LEFT JOIN users u ON v.user_id = u.id
    LEFT JOIN (SELECT DISTINCT user_id FROM book_logs WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') bl ON u.id = bl.user_id
    LEFT JOIN (SELECT DISTINCT user_id FROM carts WHERE updated_at >= CURRENT_DATE - INTERVAL '30 days') c ON u.id = c.user_id
    LEFT JOIN (SELECT DISTINCT user_id FROM orders WHERE order_date >= CURRENT_DATE - INTERVAL '30 days') o ON u.id = o.user_id
)
SELECT 
    visitors,
    registered_users,
    ROUND(registered_users * 100.0 / visitors, 2) as registration_rate,
    book_viewers,
    ROUND(book_viewers * 100.0 / registered_users, 2) as browse_rate,
    cart_users,
    ROUND(cart_users * 100.0 / book_viewers, 2) as cart_rate,
    purchasers,
    ROUND(purchasers * 100.0 / cart_users, 2) as purchase_rate
FROM funnel_data;
```

## €SKPI

### 1. ·¹Æà'ı

#### API'ı
- **ì¹İó¹B“**
  - P50: 200mså
  - P95: 500mså
  - P99: 1000mså

- **¹ëü×ÃÈ**
  - øM"API: 100 req/så

  - è‡API: 50 req/så


- **¨éü‡**
  - 4xx ¨éü: 5%å
  - 5xx ¨éü: 0.1%å

#### ã–-š‹Prometheus	
```yaml
# prometheus-rules.yml
groups:
- name: performance-alerts
  rules:
  - alert: HighResponseTime
    expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.5
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "API response time is high"
      description: "95th percentile response time is {{ $value }}s"
      
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) > 0.001
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value | humanizePercentage }}"
```

### 2. ï('

#### ;
- **¢Ã×¿¤à**: 99.9%å
“	
- **MTTRsG©çB“	**: 15å
- **MTBFsGEœ“”	**: 720B“å


#### ï('—
```sql
-- ï('—!	
WITH downtime_periods AS (
    SELECT 
        DATE_TRUNC('month', start_time) as month,
        SUM(EXTRACT(EPOCH FROM (end_time - start_time))) as total_downtime_seconds
    FROM incidents 
    WHERE status = 'RESOLVED'
        AND start_time >= DATE_TRUNC('year', CURRENT_DATE)
    GROUP BY DATE_TRUNC('month', start_time)
),
monthly_stats AS (
    SELECT 
        month,
        EXTRACT(EPOCH FROM (month + INTERVAL '1 month' - month)) as total_seconds_in_month,
        COALESCE(total_downtime_seconds, 0) as downtime_seconds
    FROM (
        SELECT generate_series(
            DATE_TRUNC('year', CURRENT_DATE),
            DATE_TRUNC('month', CURRENT_DATE),
            INTERVAL '1 month'
        ) as month
    ) months
    LEFT JOIN downtime_periods USING (month)
)
SELECT 
    month,
    ROUND(
        ((total_seconds_in_month - downtime_seconds) / total_seconds_in_month) * 100, 
        3
    ) as availability_percent,
    ROUND(downtime_seconds / 60, 2) as downtime_minutes
FROM monthly_stats
ORDER BY month;
```

### 3. »­åêÆ£

#### ;
- **c¢¯»¹fLp**: “100öå
- **1'úK‰îc~gnB“**
  - Critical: 24B“å…
  - High: 72B“å…
  - Medium: 11“å…

- **»­åêÆ£Æ¹ÈŸ½‡**: 100%êêü¹M	

#### »­åêÆ£ã–¯¨ê
```sql
-- c¢¯»¹fLã–
SELECT 
    DATE_TRUNC('day', created_at) as date,
    COUNT(*) as failed_attempts,
    COUNT(DISTINCT ip_address) as unique_ips,
    COUNT(DISTINCT email) as targeted_accounts
FROM security_logs 
WHERE event_type = 'FAILED_LOGIN'
    AND created_at >= CURRENT_DATE - INTERVAL '30 days'
    AND attempt_count >= 5  -- 5Şå
n#š1W
GROUP BY DATE_TRUNC('day', created_at)
ORDER BY date;

-- p8¢¯»¹Ñ¿üóú
SELECT 
    ip_address,
    COUNT(*) as request_count,
    COUNT(DISTINCT endpoint) as unique_endpoints,
    MAX(created_at) as last_access
FROM access_logs 
WHERE created_at >= CURRENT_DATE - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(*) > 1000  -- 1B“g1000ê¯¨¹Èå

ORDER BY request_count DESC;
```

## K(KPI

### 1. Ç×í¤áóÈ

#### ;
- **Ç×í¤;¦**: 12Şå

- **Ç×í¤Ÿ‡**: 95%å

- **íüëĞÃ¯‡**: 5%å
- **Ç×í¤B“**: 10å

#### CI/CDÑ¤×é¤óã–
```yaml
# GitHub Actions metrics collection
name: Deployment Metrics

on:
  workflow_run:
    workflows: ["Production Deploy"]
    types: [completed]

jobs:
  collect-metrics:
    runs-on: ubuntu-latest
    steps:
    - name: Collect deployment metrics
      run: |
        # Ç×í¤áóÈPœ’áÈê¯¹hWf2
        deployment_status="${{ github.event.workflow_run.conclusion }}"
        deployment_time="${{ github.event.workflow_run.updated_at - github.event.workflow_run.created_at }}"
        
        curl -X POST "https://metrics.readscape.jp/api/deployments" \
          -H "Content-Type: application/json" \
          -d "{
            \"status\": \"$deployment_status\",
            \"duration\": \"$deployment_time\",
            \"commit\": \"${{ github.event.workflow_run.head_sha }}\",
            \"environment\": \"production\",
            \"timestamp\": \"$(date -Iseconds)\"
          }"
```

### 2. Áê

#### ³üÉÁê
- **Æ¹È«ĞìÃ¸**: 80%å

- **³üÉìÓåüŸ½‡**: 100%
- **€S„ µÔ‡**: 5%åSonarQubenMaintainability Rating	

```gradle
// build.gradle - Æ¹È«ĞìÃ¸-š
jacoco {
    toolVersion = "0.8.8"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

// Áê²üÈ-š
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80  // 80%å
n«ĞìÃ¸’B
            }
        }
    }
}
```

### 3. ³¹È¹‡

#### ¤óÕé³¹È
- **“¤óÕé³¹È**: ˆ—…¥100,000å	
- **æü¶üS_Š³¹È**: ¥50å/MAU
- **ê¯¨¹ÈS_Š³¹È**: ¥0.01å

#### ³¹Èã–‹AWS	
```bash
#!/bin/bash
# ³¹Èã–¹¯ê×È

# “³¹ÈÖ—
aws ce get-cost-and-usage \
    --time-period Start=2024-01-01,End=2024-01-31 \
    --granularity MONTHLY \
    --metrics BlendedCost \
    --group-by Type=DIMENSION,Key=SERVICE

# ¢éüÈ-š
aws budgets put-budget \
    --account-id 123456789012 \
    --budget '{
        "BudgetName": "readscape-monthly-budget",
        "BudgetLimit": {
            "Amount": "100000",
            "Unit": "JPY"
        },
        "TimeUnit": "MONTHLY",
        "BudgetType": "COST",
        "CostFilters": {
            "TagKey": ["Project"],
            "TagValue": ["readscape-jp"]
        }
    }'
```

## KPIÀÃ·åÜüÉ-

### 1. ¨°¼¯Æ£ÖÀÃ·åÜüÉ

```yaml
# Grafana dashboard configuration
dashboard:
  title: "Readscape-JP Executive Dashboard"
  panels:
    - title: "Monthly Revenue"
      type: "stat"
      query: "sum(monthly_revenue)"
      thresholds: [0, 1000000, 5000000]
      
    - title: "Active Users"
      type: "graph"
      query: "monthly_active_users"
      timeRange: "12M"
      
    - title: "System Health"
      type: "gauge"
      query: "avg(up)"
      min: 0
      max: 1
      
    - title: "Top Selling Books"
      type: "table"
      query: |
        SELECT 
          title,
          author,
          SUM(quantity) as sales
        FROM order_items oi
        JOIN books b ON oi.book_id = b.id
        WHERE oi.created_at >= DATE_TRUNC('month', CURRENT_DATE)
        GROUP BY title, author
        ORDER BY sales DESC
        LIMIT 10
```

### 2. ‹zÁüàQÀÃ·åÜüÉ

```yaml
dashboard:
  title: "Development Team Dashboard"
  panels:
    - title: "API Response Times"
      type: "heatmap"
      query: "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))"
      
    - title: "Error Rate"
      type: "graph"
      query: "rate(http_requests_total{status=~'5..'}[5m])"
      
    - title: "Test Coverage"
      type: "stat"
      query: "jacoco_coverage_percentage"
      
    - title: "Deployment Frequency"
      type: "bargraph"
      query: "increase(deployments_total[7d])"
```

## KPI9„¢¯·çó×éó

### 1. KPIªBnşÜÕíü

```mermaid
graph TD
    A[KPI¢éüÈz] --> B{¢éüÈìÙë}
    B -->|Critical| C[s§k¤ó·ÇóÈ\]
    B -->|Warning| D[30å…k¿û‹Ë]
    B -->|Info| E[!ŞšpgpL]
    
    C --> F[Ê%şÜÁüàÛÆ]
    D --> G[ÅSkˆ‹]
    E --> H[9„H]
    
    F --> I[9,Ÿà]
    G --> I
    H --> I
    
    I --> J[9„ŸÅ]
    J --> K[¹œ,š]
    K --> L{9„º}
    L -->|Yes| M[Œ†]
    L -->|No| N[ı şVŸ½]
    N --> K
```

### 2. šìÓåü×í»¹

#### 1!KPIìÓåü
- **Â **: ‹zêüÀü×íÀ¯ÈªüÊü
- **@B“**: 30
- **ºî**:
  - ·¹Æà'ı
  - Ç×í¤áóÈ¶Á
  - »­åêÆ£¢éüÈ

```sql
-- 1!ìÓåü(µŞêü¯¨ê
SELECT 
    'Performance' as category,
    AVG(response_time_p95) as avg_value,
    'ms' as unit
FROM performance_metrics 
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'

UNION ALL

SELECT 
    'Availability' as category,
    AVG(uptime_percentage) as avg_value,
    '%' as unit
FROM availability_metrics 
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'

UNION ALL

SELECT 
    'Deployments' as category,
    COUNT(*) as avg_value,
    'count' as unit
FROM deployments 
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days';
```

#### !Ó¸Í¹ìÓåü
- **Â **: h¹Æü¯ÛëÀü
- **@B“**: 2B“
- **ºî**:
  - Ó¸Í¹KPIT¶Á
  - æü¶üLÕ
  - öPœ
  - !nî-š

### 3. KPI9„½V‹

#### ì¹İó¹B“9„
```yaml
½V: ­ãÃ·å&e i
î: P95ì¹İó¹B“’500ms’300msk9„
P: 2ö
wS„¢¯·çó:
  - Redis cachedne
  - Çü¿Ùü¹¯¨ê i
  - CDNe
,š¹Õ: Prometheus metricsã–
```

#### ³óĞü¸çó‡9„
```yaml
½V: üeÕíü i
î: «üÈ’üe‡’60%’70%k9„
P: 1ö
wS„¢¯·çó:
  - Á§Ã¯¢¦ÈÕíü!e
  - /UD¹Õı 
  - «üÈ>Äáüë½V
,š¹Õ: Google Analytics + ìêÈéÃ­ó°
```

## Áêİ<S6

### 1. Áê²üÈ

```mermaid
graph LR
    A[³üÉ\] --> B[êÕÆ¹È]
    B --> C[³üÉìÓåü]
    C --> D[ÁêÁ§Ã¯]
    D --> E{Áê²üÈpass?}
    E -->|Yes| F[Şü¸]
    E -->|No| G[îcB]
    G --> A
```

#### Áêú–
- **Æ¹È«ĞìÃ¸**: 80%å

- **SonarQube Quality Gate**: Pass
- **»­åêÆ£1'**: Highå
jW
- **'ıÆ¹È**: î$¯ê¢

### 2. ™š„9„×í»¹

```yaml
9„µ¤¯ë:
  Plan:
    - KPIî-š
    - 9„½VËH
    - ê½ü¹;
    
  Do:
    - ½VŸÅ
    - Æ¹ÈŸL
    - Ç×í¤áóÈ
    
  Check:
    - KPI,š
    - ¹œ<
    - ²L½ú
    
  Action:
    - fÒ…¹t
    - !;Í 
    - Ù¹È×é¯Æ£¹q	
```

SnKPI¡S6kˆŠReadscape-JP×í¸§¯ÈnÁê
h™š„jw’ŸşW~Y