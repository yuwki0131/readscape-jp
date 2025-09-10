# テスト実行ガイド

## 概要

このガイドでは、Readscape-JP Consumer APIのテスト実行方法について説明します。

## テスト構成

### テストカテゴリ

1. **ユニットテスト** - 個別コンポーネントのテスト
2. **統合テスト** - API間結合とワークフローテスト  
3. **リポジトリテスト** - データベース操作テスト
4. **パフォーマンステスト** - 負荷・スケーラビリティテスト

### テストクラス一覧

#### ユニットテスト
```
✅ AuthControllerTest - 認証API
✅ BookServiceTest - 書籍サービス
✅ CartServiceTest - カートサービス  
✅ UserServiceTest - ユーザーサービス
✅ JwtServiceTest - JWT認証
✅ OrderServiceTest - 注文処理
✅ ReviewServiceTest - レビューシステム
✅ LoginAttemptServiceTest - ログイン試行管理
✅ BooksControllerTest - 書籍REST API
✅ CartsControllerTest - カートREST API
```

#### 統合テスト
```
✅ OrderProcessingIntegrationTest - 注文処理フロー
✅ InventoryIntegrationTest - 在庫管理統合
```

#### リポジトリテスト
```
✅ BookRepositoryTest - 書籍データ操作
✅ UserRepositoryTest - ユーザーデータ操作
✅ OrderRepositoryTest - 注文データ操作
✅ CartRepositoryTest - カートデータ操作
```

## テスト実行方法

### 全テスト実行

```bash
# 全テスト実行
./gradlew test

# カバレッジレポート付き実行
./gradlew test jacocoTestReport

# 継続的な実行（変更検知）
./gradlew test --continuous
```

### カテゴリ別テスト実行

```bash
# ユニットテストのみ
./gradlew test --tests "*Test"

# 統合テストのみ  
./gradlew test --tests "*IntegrationTest"

# リポジトリテストのみ
./gradlew test --tests "*RepositoryTest"

# 特定パッケージのテスト
./gradlew test --tests "jp.readscape.consumer.services.*"
```

### 個別テスト実行

```bash
# 特定クラスのテスト
./gradlew test --tests "CartServiceTest"

# 特定メソッドのテスト
./gradlew test --tests "CartServiceTest.addToCartSuccess"

# パターンマッチでの実行
./gradlew test --tests "*Service*Test"
```

## テスト設定

### プロファイル設定

```yaml
# application-test.yml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:tc:postgresql:15:///readscape_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
```

### JaCoCo設定

```gradle
jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
        csv.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80 // 80%カバレッジ要求
            }
        }
    }
}
```

## テストデータ活用

### TestDataFactory使用例

```java
// 基本的なテストユーザー作成
User testUser = TestDataFactory.createUser();

// カスタムユーザー作成
User customUser = TestDataFactory.createUser(
    "custom@example.com", "カスタムユーザー", UserRole.ADMIN
);

// ランダム書籍作成
List<Book> books = TestDataFactory.createBooks(10);

// 在庫レベル指定
Book lowStockBook = TestDataFactory.createBookWithStockLevel(
    TestDataFactory.StockLevel.LOW_STOCK
);
```

### TestScenarios使用例

```java
// 新規ユーザー登録シナリオ
var scenario = new TestScenarios.NewUserRegistrationScenario();
User newUser = scenario.newUser;
String password = scenario.plainPassword;

// ショッピングカートシナリオ
var cartScenario = new TestScenarios.ShoppingCartScenario();
User customer = cartScenario.customer;
List<Book> books = cartScenario.availableBooks;

// 複雑なシナリオ構築
var testData = TestDataFactory.scenario()
    .users(5)
    .books(20) 
    .orders(10)
    .reviews(15)
    .userRole(UserRole.CONSUMER)
    .stockLevel(TestDataFactory.StockLevel.MEDIUM_STOCK)
    .build();
```

### TestUtils使用例

```java
// JSON変換
String json = TestUtils.toJson(request);
ResponseDto response = TestUtils.fromJson(responseJson, ResponseDto.class);

// MockMvcアサーション
mockMvc.perform(get("/api/books"))
    .andExpect(TestUtils.jsonPathListSize("$.books", 5))
    .andExpect(TestUtils.jsonPathExists("$.pagination"))
    .andExpectAll(TestUtils.successResponseStructure());

// 時間関連
LocalDateTime fiveMinutesAgo = TestUtils.minutesAgo(5);
boolean isRecent = TestUtils.isWithinRange(actual, expected, 2);

// 並行処理テスト
var helper = new TestUtils.ConcurrentTestHelper(10);
var futures = helper.runConcurrently(() -> service.processOrder());
var results = TestUtils.ConcurrentTestHelper.waitForAll(futures);
```

## パフォーマンステスト実行

### 大量データ生成

```java
// 小規模テスト用データ
var scenario = TestScenarios.PerformanceTestScenario.small(); // 100ユーザー、500書籍

// 中規模テスト用データ  
var scenario = TestScenarios.PerformanceTestScenario.medium(); // 1000ユーザー、5000書籍

// 大規模テスト用データ
var scenario = TestScenarios.PerformanceTestScenario.large(); // 10000ユーザー、50000書籍
```

### パフォーマンス測定

```java
// 実行時間測定
var result = TestUtils.measureTime(() -> {
    return bookService.searchBooks("keyword", pageable);
});

System.out.println("実行時間: " + result.getExecutionTimeMs() + "ms");
assertTrue(result.getExecutionTimeMs() < 1000, "検索は1秒以内に完了する必要があります");
```

## カバレッジレポート確認

### レポート場所

- **HTML**: `build/reports/jacoco/test/html/index.html`
- **XML**: `build/reports/jacoco/test/jacocoTestReport.xml`
- **CSV**: `build/reports/jacoco/test/jacocoTestReport.csv`

### カバレッジ目標

- **総合カバレッジ**: 80%以上
- **クラス別カバレッジ**: 70%以上
- **除外対象**: Application、DTO、Config、Exception クラス

## CI/CD での実行

### GitHub Actions設定例

```yaml
name: Test
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run tests
        run: ./gradlew test jacocoTestReport
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          file: ./build/reports/jacoco/test/jacocoTestReport.xml
```

## トラブルシューティング

### よくある問題と解決方法

#### 1. Testcontainers起動失敗
```bash
# Dockerが起動していることを確認
docker --version
docker ps

# メモリ不足の場合
export DOCKER_HOST_CONFIG="--memory=4g"
```

#### 2. テストデータベース接続エラー
```bash
# ポート競合確認
netstat -an | grep 5432

# テストプロファイル確認
cat src/test/resources/application-test.yml
```

#### 3. JaCoCoカバレッジエラー
```bash
# 除外設定確認
./gradlew test --info | grep -i jacoco

# レポート生成強制実行  
./gradlew clean test jacocoTestReport --no-build-cache
```

#### 4. 並行テスト実行でのデータ競合
```java
// @DirtiesContextでテスト分離
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)

// Transactional でロールバック
@Transactional
```

## ベストプラクティス

### テスト作成のガイドライン

1. **Given-When-Then構造**を使用
2. **テスト名は日本語**で具体的に記述
3. **@DisplayName**でテストの意図を明確化
4. **setUp・tearDown**で適切なデータ管理
5. **アサーションは具体的**に記述

### パフォーマンス考慮事項

1. **@TestMethodOrder**で実行順序制御
2. **@TestInstance(PER_CLASS)**で初期化コスト削減
3. **テストデータの再利用**でDB負荷軽減
4. **並行実行**での競合状態回避

### メンテナンス性向上

1. **TestDataFactory**でデータ生成統一
2. **TestScenarios**で共通パターン定義  
3. **TestUtils**でアサーション共通化
4. **BaseIntegrationTest**で共通設定継承

---

## 連絡先・サポート

テスト実行で問題が発生した場合：

- **GitHub Issues**: プロジェクトリポジトリで報告
- **ドキュメント**: `docs/testing/` 配下の詳細資料を参照
- **コードレビュー**: テストコード改善提案はプルリクエストで