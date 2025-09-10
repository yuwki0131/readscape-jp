# Readscape-JP KPI�����<���

## ��

Readscape-JP����n��
h���9���hW_KPI́m>U�	n��,��k�Y������gY�z�����������K(���Lqng��,�W����Ջnz��LD~Y

## KPI�������

### 1. KPI^S�

```mermaid
graph TB
    subgraph "Ӹ͹KPI"
        B1[�
���]
        B2[����r�]
        B3[g����]
        B4[4���]
    end
    
    subgraph "�����KPI"
        P1[_�)(�]
        P2[�����������]
        P3[�������]
        P4[�����]
    end
    
    subgraph "�SKPI"
        T1[����'�]
        T2[�(']
        T3[����ƣ]
        T4[�����]
    end
    
    subgraph "K(KPI"
        O1[�������]
        O2[������]
        O3[������]
        O4[��ȹ�]
    end
```

## Ӹ͹KPI

### 1. �
���

#### ;�
- **!�
�MRR: Monthly Recurring Revenue	**
  - �$: M�5%w
  - ,���: ����n�M
  - ������: orders ����status='DELIVERED'	

- **sG臡MAOV: Average Order Value	**
  - �$: �3,500�

  - ,���: ��
 � �p
  - ������: orders.total_amount

- **g���$LTV: Lifetime Value	**
  - �$: �15,000�

  - �: (sG臡M � �e;� � g����) - g�r����
  - ��;�: !

#### ,�SQL�
```sql
-- !�
�
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

-- g���$
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

### 2. ����r��w

#### ;�
- **���ƣ�����MAU: Monthly Active Users	**
  - �$: 10,000�����

  - ��: �k1��
��W_����p

- **������r��**
  - �$: �300�����

  - ,���: ���{2����p

- **������������	**
  - 1�: 70%�

  - 7�: 40%�
  
  - 30�: 20%�


#### ,����
```sql
-- MAU�
SELECT 
    DATE_TRUNC('month', last_login_at) as month,
    COUNT(DISTINCT id) as monthly_active_users
FROM users 
WHERE last_login_at >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '12 months'
GROUP BY DATE_TRUNC('month', last_login_at)
ORDER BY month;

-- ���������	
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

## �����KPI

### 1. _�)(�

#### ;�
- **�M")(�**
  - �$: MAUn80%�

  - ,�: �"�L����p / MAU

- **���)(�**
  - �$: MAUn40%�

  - ,�: ����)(����p / MAU

- **�����?�**
  - �$: �en15%�

  - ,�: �����?p / �e��p

#### ,��÷����
```sql
-- _�)(��÷����
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

### 2. �������

#### �������ա��
```mermaid
graph TD
    A[*O] -->|{2� 5%| B[{2����]
    B -->|�M��� 70%| C[�M��]
    C -->|������ 25%| D[�����]
    D -->|�e� 60%| E[�e��]
    E -->|�����e� 40%| F[�����e]
```

#### ;�
- ***O�{2�������**: 5%�

- **���������**: 25%�

- **��Ȓ�e�**: 60%�

- **ޒ�����e�**: 40%�


```sql
-- �������ա���
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

## �SKPI

### 1. ����'�

#### API'�
- **���B�**
  - P50: 200ms�
  - P95: 500ms�
  - P99: 1000ms�

- **������**
  - �M"API: 100 req/s�

  - �API: 50 req/s�


- **����**
  - 4xx ���: 5%�
  - 5xx ���: 0.1%�

#### �-��Prometheus	
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

### 2. �('

#### ;�
- **��׿��**: 99.9%�
�	
- **MTTRsG��B�	**: 15�
- **MTBFsGE���	**: 720B��


#### �('�
```sql
-- �('�!	
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

### 3. ����ƣ

#### ;�
- **c����fLp**: �100��
- **1'�K��c~gnB�**
  - Critical: 24B��
  - High: 72B��
  - Medium: 11��

- **����ƣƹȟ��**: 100%����M	

#### ����ƣ㖯��
```sql
-- c����fL�
SELECT 
    DATE_TRUNC('day', created_at) as date,
    COUNT(*) as failed_attempts,
    COUNT(DISTINCT ip_address) as unique_ips,
    COUNT(DISTINCT email) as targeted_accounts
FROM security_logs 
WHERE event_type = 'FAILED_LOGIN'
    AND created_at >= CURRENT_DATE - INTERVAL '30 days'
    AND attempt_count >= 5  -- 5��
n#�1W
GROUP BY DATE_TRUNC('day', created_at)
ORDER BY date;

-- p8����ѿ���
SELECT 
    ip_address,
    COUNT(*) as request_count,
    COUNT(DISTINCT endpoint) as unique_endpoints,
    MAX(created_at) as last_access
FROM access_logs 
WHERE created_at >= CURRENT_DATE - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(*) > 1000  -- 1B�g1000ꯨ���

ORDER BY request_count DESC;
```

## K(KPI

### 1. ������

#### ;�
- **���;�**: 12��

- **�����**: 95%�

- **����ï�**: 5%�
- **���B�**: 10�

#### CI/CDѤ����
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
        # ������P����꯹hWf2
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

### 2. ��

#### �����
- **ƹȫ��ø**: 80%�

- **����������**: 100%
- **�S���ԇ**: 5%�SonarQubenMaintainability Rating	

```gradle
// build.gradle - ƹȫ��ø-�
jacoco {
    toolVersion = "0.8.8"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

// ����-�
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80  // 80%�
n���ø��B
            }
        }
    }
}
```

### 3. ��ȹ�

#### ���鳹�
- **����鳹�**: ����100,000�	
- **����S_����**: �50�/MAU
- **ꯨ��S_����**: �0.01�

#### ���㖋AWS	
```bash
#!/bin/bash
# ���㖹����

# ����֗
aws ce get-cost-and-usage \
    --time-period Start=2024-01-01,End=2024-01-31 \
    --granularity MONTHLY \
    --metrics BlendedCost \
    --group-by Type=DIMENSION,Key=SERVICE

# ����-�
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

## KPI�÷����-

### 1. ����ƣ��÷����

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

### 2. �z���Q�÷����

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

## KPI9���������

### 1. KPI�Bn�����

```mermaid
graph TD
    A[KPI����z] --> B{�������}
    B -->|Critical| C[s�k�����\]
    B -->|Warning| D[30�k����]
    B -->|Info| E[!ޚpgpL]
    
    C --> F[�%�������]
    D --> G[�Sk���]
    E --> H[9�H]
    
    F --> I[9,���]
    G --> I
    H --> I
    
    I --> J[9���]
    J --> K[��,�]
    K --> L{9���}
    L -->|Yes| M[��]
    L -->|No| N[���V��]
    N --> K
```

### 2. �������

#### 1!KPI����
- ** **: �z��������Ȫ���
- **@�B�**: 30
- **���**:
  - ����'�
  - �����ȶ�
  - ����ƣ����

```sql
-- 1!����(�������
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

#### !Ӹ͹����
- ** **: h��������
- **@�B�**: 2B�
- **���**:
  - Ӹ͹KPIT��
  - ����L��
  - ��P�
  - !n�-�

### 3. KPI9��V�

#### ���B�9�
```yaml
�V: ��÷�&e i
�: P95���B��500ms�300msk9�
P: 2�
wS������:
  - Redis cachedne
  - ��������� i
  - CDNe
,���: Prometheus metrics�
```

#### �������9�
```yaml
�V: �e��� i
�: ��Ȓ�e��60%�70%k9�
P: 1�
wS������:
  - ��ï������!e
  - /UD����
  - ���>����V
,���: Google Analytics + ����í�
```

## ���<S6

### 1. ����

```mermaid
graph LR
    A[���\] --> B[��ƹ�]
    B --> C[�������]
    C --> D[����ï]
    D --> E{����pass?}
    E -->|Yes| F[����]
    E -->|No| G[�c�B]
    G --> A
```

#### ����
- **ƹȫ��ø**: 80%�

- **SonarQube Quality Gate**: Pass
- **����ƣ1'**: High�
jW
- **'�ƹ�**: �$��

### 2. ���9���

```yaml
9�����:
  Plan:
    - KPI�-�
    - 9��V�H
    - ���;
    
  Do:
    - �V��
    - ƹȟL
    - ������
    
  Check:
    - KPI,�
    - ��<
    - �L��
    
  Action:
    - f҅�t
    - !;� 
    - ٹ���ƣ�q	
```

SnKPI�S6k��Readscape-JP����n��
h���jw���W~Y