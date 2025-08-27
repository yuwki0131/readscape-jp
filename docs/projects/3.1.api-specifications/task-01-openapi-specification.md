# タスク01: OpenAPI仕様書作成

## タスク概要
Consumer APIとInventory Management APIの包括的なOpenAPI 3.0.3仕様書を作成し、Springdocを利用した自動生成機能を実装します。

## 実装内容

### 1. Consumer API OpenAPI仕様書
#### エンドポイント定義
- **書籍API**: 書籍一覧、詳細、検索
- **ユーザーAPI**: ログイン、登録、プロフィール管理
- **カートAPI**: カート管理、商品追加・削除
- **注文API**: 注文作成、履歴、詳細
- **レビューAPI**: レビュー作成、取得、更新

#### セキュリティ設定
- JWT Bearer認証
- 認証不要エンドポイントの定義
- セキュリティスキーマ設定

### 2. Inventory Management API OpenAPI仕様書
#### エンドポイント定義
- **書籍管理API**: 書籍CRUD操作
- **在庫管理API**: 在庫追跡、更新
- **注文管理API**: 注文処理、状態管理
- **分析API**: 売上分析、レポート

#### セキュリティ設定
- 管理者認証（AdminAuth）
- ロールベースアクセス制御

### 3. Springdoc OpenAPI設定
#### 設定クラス作成
```java
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Readscape-JP API")
                        .version("1.0.0")
                        .description("日本語対応書籍販売システムのREST API"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

#### 依存関係追加
- springdoc-openapi-starter-webmvc-ui
- springdoc-openapi-starter-webmvc-api

### 4. APIアノテーション適用
#### コントローラー層
- @Tag, @Operation, @ApiResponse アノテーション
- @Parameter でパラメータ説明
- @RequestBody, @Schema でリクエスト/レスポンス定義

#### DTOクラス
- @Schema でモデル定義
- @JsonProperty でプロパティ設定
- バリデーションアノテーション連携

### 5. Swagger UI/ReDoc設定
#### アクセスURL設定
- Swagger UI: `/api/swagger-ui.html`
- ReDoc: `/api/redoc`
- OpenAPI JSON: `/api/v3/api-docs`

#### カスタマイズ
- 日本語UI対応
- カスタムCSS適用
- 認証機能統合

## 受け入れ条件
- [ ] Consumer APIの完全なOpenAPI仕様書が作成されている
- [ ] Inventory Management APIの完全なOpenAPI仕様書が作成されている
- [ ] Springdoc設定が両APIで動作している
- [ ] Swagger UIで全エンドポイントが表示される
- [ ] ReDocで読みやすいドキュメントが生成される
- [ ] 認証機能がSwagger UIで動作する
- [ ] 全APIエンドポイントにOpenAPIアノテーションが適用されている
- [ ] レスポンススキーマが正しく定義されている
- [ ] エラーレスポンスが適切に文書化されている

## 関連ファイル
### Consumer API
- `consumer-api/src/main/resources/openapi/consumer-api.yml`
- `consumer-api/src/main/java/jp/readscape/consumer/config/OpenApiConfig.java`
- `consumer-api/src/main/java/jp/readscape/consumer/controllers/**/*.java`

### Inventory Management API  
- `inventory-management-api/src/main/resources/openapi/inventory-api.yml`
- `inventory-management-api/src/main/java/jp/readscape/inventory/config/OpenApiConfig.java`
- `inventory-management-api/src/main/java/jp/readscape/inventory/controllers/**/*.java`

### 共通
- `docs/api/user-guide.md`
- `docs/api/authentication-guide.md`

## 技術仕様
- OpenAPI: 3.0.3
- Springdoc: 2.x
- Swagger UI: 5.x
- ReDoc: 最新版
- 文字エンコーディング: UTF-8
- 認証方式: JWT Bearer Token