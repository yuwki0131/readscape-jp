# システムアーキテクチャ設計書

## 概要

Readscape-JPは、書籍販売システムとして、一般消費者向けのConsumer APIと管理者向けのInventory Management APIを提供するマイクロサービスアーキテクチャを採用しています。

## 全体システム構成

```mermaid
graph TB
    subgraph "Frontend Applications"
        WebApp[Web Application]
        MobileApp[Mobile Application]
        AdminPanel[Admin Panel]
    end

    subgraph "API Gateway"
        Gateway[API Gateway<br/>nginx/envoy]
    end

    subgraph "Microservices"
        ConsumerAPI[Consumer API<br/>:8080]
        InventoryAPI[Inventory Management API<br/>:8081]
    end

    subgraph "Data Layer"
        PostgresDB[(PostgreSQL<br/>Database)]
        Redis[(Redis Cache)]
    end

    subgraph "External Services"
        PaymentGW[Payment Gateway]
        EmailService[Email Service]
        S3[File Storage<br/>S3 Compatible]
    end

    WebApp --> Gateway
    MobileApp --> Gateway
    AdminPanel --> Gateway

    Gateway --> ConsumerAPI
    Gateway --> InventoryAPI

    ConsumerAPI --> PostgresDB
    ConsumerAPI --> Redis
    InventoryAPI --> PostgresDB
    InventoryAPI --> Redis

    ConsumerAPI --> PaymentGW
    ConsumerAPI --> EmailService
    ConsumerAPI --> S3
    InventoryAPI --> S3
```

## マイクロサービス分離戦略

### 1. Consumer API（消費者向けAPI）

#### 責務
- 書籍検索・閲覧機能
- ユーザー登録・認証
- ショッピングカート管理
- 注文処理
- レビュー投稿・閲覧

#### 技術スタック
- **フレームワーク**: Spring Boot 3.2.0
- **Java バージョン**: Java 21
- **認証**: JWT + Spring Security
- **データアクセス**: Spring Data JPA
- **API文書**: Springdoc OpenAPI

#### 特徴
- 読み取り中心のワークロード
- 高い同時アクセス要求
- 外部システム連携（決済、メール）

### 2. Inventory Management API（在庫管理API）

#### 責務
- 書籍マスタ管理（CRUD）
- 在庫管理・追跡
- 管理者認証・認可
- 売上分析・レポート
- システム管理機能

#### 技術スタック
- **フレームワーク**: Spring Boot 3.2.0
- **Java バージョン**: Java 21
- **認証**: JWT + Spring Security（ロールベース）
- **データアクセス**: Spring Data JPA
- **API文書**: Springdoc OpenAPI

#### 特徴
- 書き込み中心のワークロード
- 管理者権限での操作
- データ整合性の重視
- バッチ処理機能

## データベース構成

### 共有データベース戦略

両APIは同一のPostgreSQLデータベースを共有しますが、論理的にスキーマを分離しています。

```mermaid
erDiagram
    users ||--o{ orders : places
    users ||--o{ reviews : writes  
    users ||--|| carts : has
    books ||--o{ order_items : contains
    books ||--o{ cart_items : contains
    books ||--o{ reviews : receives
    books ||--o{ inventory_transactions : tracks
    orders ||--o{ order_items : contains
    carts ||--o{ cart_items : contains
    
    users {
        bigserial id PK
        varchar email UK
        varchar password_hash
        varchar name
        timestamp created_at
        timestamp updated_at
    }
    
    books {
        bigserial id PK
        varchar title
        varchar author
        varchar isbn UK
        varchar category
        integer price
        text description
        date publication_date
        varchar publisher
        integer pages
        integer stock_quantity
        varchar status
        timestamp created_at
        timestamp updated_at
        bigint created_by FK
        bigint updated_by FK
    }
    
    orders {
        bigserial id PK
        varchar order_number UK
        bigint user_id FK
        varchar status
        integer total_price
        text shipping_address
        varchar payment_method
        timestamp order_date
        date estimated_delivery
    }
    
    carts {
        bigserial user_id PK
        timestamp updated_at
    }
    
    cart_items {
        bigserial id PK
        bigint cart_id FK
        bigint book_id FK
        integer quantity
        timestamp added_at
    }
    
    reviews {
        bigserial id PK
        bigint user_id FK
        bigint book_id FK
        integer rating
        text comment
        timestamp created_at
    }
```

### データ一貫性の確保

1. **Transaction Management**
   - Spring の `@Transactional` アノテーションによる宣言的トランザクション
   - データベースレベルでの外部キー制約
   - 楽観的ロック（`@Version`）による同時実行制御

2. **Data Validation**
   - Bean Validation（JSR-303）による入力検証
   - データベース制約による整合性チェック
   - ビジネスロジック層での整合性確認

## セキュリティアーキテクチャ

### 認証・認可フロー

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant ConsumerAPI
    participant AuthService
    participant Database

    Client->>Gateway: Login Request
    Gateway->>ConsumerAPI: Forward Request
    ConsumerAPI->>AuthService: Validate Credentials
    AuthService->>Database: Check User
    Database-->>AuthService: User Data
    AuthService->>AuthService: Generate JWT
    AuthService-->>ConsumerAPI: JWT Token
    ConsumerAPI-->>Gateway: Login Response
    Gateway-->>Client: JWT Token

    Note over Client: Store JWT Token

    Client->>Gateway: API Request + JWT
    Gateway->>Gateway: Validate JWT
    Gateway->>ConsumerAPI: Forward Request + User Info
    ConsumerAPI->>ConsumerAPI: Check Permissions
    ConsumerAPI-->>Gateway: API Response
    Gateway-->>Client: Response
```

### セキュリティ層

1. **Network Security**
   - HTTPS通信の強制
   - CORS設定による適切なオリジン制御
   - Rate Limiting（レート制限）

2. **Application Security**
   - JWT による認証
   - ロールベースアクセス制御（RBAC）
   - Input Validation & Sanitization
   - SQL Injection対策（JPA使用）

3. **Data Security**
   - パスワードのハッシュ化（bcrypt）
   - 個人情報の暗号化
   - 監査ログの記録

## キャッシュ戦略

### Redis キャッシュ活用

```mermaid
graph LR
    subgraph "Application Layer"
        API[API Server]
    end
    
    subgraph "Cache Layer"  
        Redis[(Redis Cache)]
    end
    
    subgraph "Data Layer"
        DB[(PostgreSQL)]
    end
    
    API --> Redis
    API --> DB
    Redis -.-> DB
```

#### キャッシュ対象データ
- **書籍情報**: 検索結果、カテゴリー一覧
- **ユーザーセッション**: JWT トークンブラックリスト
- **システム設定**: アプリケーション設定値

#### キャッシュ戦略
- **TTL設定**: データ特性に応じた有効期限
- **Cache-Aside Pattern**: アプリケーション制御によるキャッシュ
- **Write-Behind**: 非同期書き込みによる性能向上

## パフォーマンス設計

### 目標性能指標

| メトリクス | 目標値 | 測定方法 |
|-----------|--------|----------|
| API レスポンス時間 | < 200ms | APM ツール |
| データベースクエリ時間 | < 100ms | JPA Statistics |
| アプリケーション起動時間 | < 30秒 | 起動ログ |
| 同時接続数 | 1,000+ | Load Testing |
| スループット | 100 req/sec | Benchmark |

### 性能最適化戦略

1. **データベース最適化**
   - インデックス設計による検索性能向上
   - クエリチューニング
   - Connection Pooling

2. **アプリケーション最適化**
   - レスポンシブキャッシング
   - 非同期処理の活用
   - リソースプールの最適化

3. **インフラ最適化**
   - ロードバランシング
   - オートスケーリング
   - CDN活用（静的リソース）

## 監視・運用設計

### Observability（可観測性）

```mermaid
graph TB
    subgraph "Applications"
        App1[Consumer API]
        App2[Inventory API]
    end

    subgraph "Monitoring Stack"
        Metrics[Metrics<br/>Prometheus]
        Logs[Logs<br/>ELK Stack]
        Traces[Traces<br/>Jaeger]
    end

    subgraph "Alerting"
        AlertManager[Alert Manager]
        Grafana[Grafana Dashboard]
    end

    App1 --> Metrics
    App1 --> Logs
    App1 --> Traces
    App2 --> Metrics
    App2 --> Logs
    App2 --> Traces

    Metrics --> AlertManager
    Metrics --> Grafana
    Logs --> Grafana
    Traces --> Grafana
```

#### 監視項目
- **インフラメトリクス**: CPU、メモリ、ディスク使用率
- **アプリケーションメトリクス**: レスポンス時間、エラー率
- **ビジネスメトリクス**: 注文数、売上額、ユーザー数

## 拡張性・可用性設計

### スケーラビリティ

1. **水平スケーリング**
   - ステートレス設計による複数インスタンス対応
   - ロードバランサーによる負荷分散
   - データベース読み取りレプリカ

2. **垂直スケーリング**
   - JVM チューニング
   - リソース制限の動的調整

### 可用性（High Availability）

1. **冗長化**
   - アプリケーションサーバーの複数配置
   - データベースクラスタリング
   - Redis Sentinel/Cluster

2. **障害対策**
   - Health Check エンドポイント
   - Circuit Breaker パターン
   - Graceful Shutdown

## 今後の拡張計画

### Phase 2: マイクロサービス分離
- User Service の独立
- Order Service の独立
- Search Service の独立

### Phase 3: イベント駆動アーキテクチャ
- メッセージブローカー導入（Apache Kafka/RabbitMQ）
- 非同期イベント処理
- CQRS パターン適用

### Phase 4: クラウドネイティブ化
- Kubernetes デプロイメント
- Service Mesh（Istio）導入
- Serverless Function の活用