# タスク01: Spring Boot設定ファイル統合管理

## タスク概要
Spring Boot アプリケーションの各種設定ファイルと環境別設定管理の統一・最適化を行います。

## 実装内容

### 1. 環境別設定ファイルの統一

#### 共通設定ファイル (`application.yml`)
```yaml
spring:
  application:
    name: ${APP_NAME:readscape-api}
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          time_zone: Asia/Tokyo
  
  flyway:
    enabled: true
    schemas: readscape
    locations: filesystem:../infrastructure/database/migrations
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api
  error:
    include-stacktrace: never
    include-message: always

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized

logging:
  level:
    root: INFO
    jp.readscape: INFO
    org.springframework.security: WARN
```

#### 開発環境設定 (`application-dev.yml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  h2:
    console:
      enabled: true
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    defer-datasource-initialization: true
  
  flyway:
    enabled: false  # 開発環境ではH2使用のため無効化
  
  security:
    jwt:
      secret: dev-jwt-secret-key-for-development-only-256-bits-long
      expiration: 3600000
      refresh-expiration: 2592000000

logging:
  level:
    jp.readscape: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

server:
  error:
    include-stacktrace: always
```

#### Docker環境設定 (`application-docker.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:readscape}
    username: ${DB_USER:readscape_user}
    password: ${DB_PASSWORD:readscape_pass}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:20}
      minimum-idle: ${DB_POOL_MIN_IDLE:5}
      connection-timeout: 30000
      validation-timeout: 5000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  security:
    jwt:
      secret: ${JWT_SECRET:docker-development-jwt-secret-key-256-bits-long-for-secure-authentication}
      expiration: ${JWT_EXPIRATION:3600000}
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:2592000000}

logging:
  level:
    org.flywaydb: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

#### 本番環境設定 (`application-prod.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:50}
      minimum-idle: ${DB_POOL_MIN_IDLE:10}
      connection-timeout: 20000
      validation-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
  
  jpa:
    show-sql: false
  
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: ${JWT_EXPIRATION:3600000}
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:2592000000}

server:
  error:
    include-stacktrace: never

logging:
  level:
    root: WARN
    jp.readscape: INFO
  file:
    name: /var/log/readscape/${APP_NAME}.log
    max-size: 100MB
    max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2. Logback設定ファイル (`logback-spring.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name"/>
    
    <!-- 開発・Docker環境用設定 -->
    <springProfile name="dev,docker">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
    </springProfile>

    <!-- 本番環境用設定 -->
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/readscape/${APP_NAME}.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>/var/log/readscape/${APP_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <!-- エラー専用ログファイル -->
        <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/readscape/${APP_NAME}-error.log</file>
            <filter class="ch.qos.logback.classic.filter.LevelFilter">
                <level>ERROR</level>
                <onMatch>ACCEPT</onMatch>
                <onMismatch>DENY</onMismatch>
            </filter>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>/var/log/readscape/${APP_NAME}-error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
                <maxFileSize>50MB</maxFileSize>
                <maxHistory>60</maxHistory>
                <totalSizeCap>1GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="FILE" />
            <appender-ref ref="ERROR_FILE" />
        </root>
    </springProfile>
</configuration>
```

### 3. バリデーションメッセージファイル

#### `messages/validation_ja.properties`
```properties
# 共通バリデーションメッセージ
javax.validation.constraints.NotNull.message=必須項目です
javax.validation.constraints.NotEmpty.message=空の値は許可されていません
javax.validation.constraints.NotBlank.message=空白は許可されていません
javax.validation.constraints.Size.message=文字数は{min}文字以上{max}文字以下で入力してください
javax.validation.constraints.Email.message=正しいメールアドレスを入力してください
javax.validation.constraints.Min.message={value}以上の値を入力してください
javax.validation.constraints.Max.message={value}以下の値を入力してください
javax.validation.constraints.Pattern.message=入力形式が正しくありません
javax.validation.constraints.DecimalMin.message={value}以上の値を入力してください
javax.validation.constraints.DecimalMax.message={value}以下の値を入力してください

# アプリケーション固有メッセージ
book.isbn.invalid=ISBNの形式が正しくありません
book.price.negative=価格は0以上である必要があります
user.username.duplicate=このユーザー名は既に使用されています
user.email.duplicate=このメールアドレスは既に登録されています
order.quantity.invalid=注文数量は1以上である必要があります
cart.item.notfound=カートアイテムが見つかりません
```

### 4. 環境変数テンプレート (`.env.example`)

```bash
# Application Configuration
APP_NAME=readscape-consumer-api
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8080

# Database Configuration
DB_HOST=postgres
DB_PORT=5432
DB_NAME=readscape
DB_USER=readscape_user
DB_PASSWORD=readscape_pass
DB_POOL_SIZE=20
DB_POOL_MIN_IDLE=5

# JWT Configuration
JWT_SECRET=your-very-secure-jwt-secret-key-256-bits-long-change-in-production
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=2592000000

# SSL Configuration (Production only)
SSL_KEY_STORE_PATH=/etc/ssl/certs/readscape.p12
SSL_KEY_STORE_PASSWORD=your-ssl-keystore-password

# Monitoring Configuration
MONITORING_ENABLED=true
LOG_LEVEL=INFO
```

## 受け入れ条件

- [x] **Consumer API設定統一**: 全環境設定ファイルが整備される
- [ ] **Inventory API設定統一**: Consumer APIと同じ構造に統一される
- [ ] **本番環境設定**: セキュリティ強化された本番設定が追加される
- [ ] **Logback設定**: 環境別ログ設定が実装される
- [ ] **バリデーション**: 日本語メッセージファイルが作成される
- [ ] **環境変数管理**: 設定テンプレートが提供される
- [ ] **設定の一貫性**: 両APIで設定構造が統一される
- [ ] **セキュリティ**: 本番環境でのセキュリティ設定が強化される

## 対象ファイル

### Consumer API
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-docker.yml`
- `src/main/resources/application-prod.yml` ←新規作成
- `src/main/resources/logback-spring.xml` ←新規作成
- `src/main/resources/messages/validation_ja.properties` ←新規作成

### Inventory Management API
- `src/main/resources/application.yml` ←統一化
- `src/main/resources/application-dev.yml` ←新規作成
- `src/main/resources/application-docker.yml` ←統一化
- `src/main/resources/application-prod.yml` ←新規作成
- `src/main/resources/logback-spring.xml` ←新規作成
- `src/main/resources/messages/validation_ja.properties` ←新規作成

### プロジェクトルート
- `.env.example` ←新規作成

## 技術仕様
- Spring Boot Configuration Properties
- Profile-based Configuration Management
- Logback + SLF4J
- HikariCP Connection Pool
- Bean Validation with Custom Messages
- Environment Variables Management