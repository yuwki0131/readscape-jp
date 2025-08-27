# タスク02: Flyway マイグレーション設定

## タスク概要
Flyway を使用したデータベースバージョン管理システムの構築と設定を行います。

## 実装内容

### 1. Flyway設定
```yaml
# application.yml
spring:
  flyway:
    enabled: true
    schemas: readscape
    locations: filesystem:../infrastructure/database/migrations
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true
```

### 2. build.gradle設定
```gradle
plugins {
    id 'org.flywaydb.flyway' version '9.22.3'
}

flyway {
    url = 'jdbc:postgresql://localhost:5432/readscape'
    user = 'readscape_user'
    password = 'readscape_pass'
    schemas = ['readscape']
    locations = ['filesystem:../infrastructure/database/migrations']
}
```

### 3. マイグレーションスクリプト作成
- 命名規則: `V{version}__{description}.sql`
- バージョン管理の厳密な運用
- ロールバック対応

## 受け入れ条件
- [ ] Flyway が正しく設定される
- [ ] マイグレーションが自動実行される
- [ ] バージョン管理が機能する
- [ ] 本番環境で安全に実行できる
- [ ] ロールバック可能な設計
- [ ] チーム開発でのコンフリクト回避

## 関連ファイル
- `src/main/resources/application.yml`
- `build.gradle`
- `infrastructure/database/migrations/`