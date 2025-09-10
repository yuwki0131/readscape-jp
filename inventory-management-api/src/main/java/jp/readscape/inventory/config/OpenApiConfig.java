package jp.readscape.inventory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Readscape-JP Inventory Management API")
                        .version(appVersion)
                        .description("日本語対応書籍販売システムの在庫管理API（管理者・マネージャー向け）")
                        .contact(new Contact()
                                .name("Readscape-JP API Team")
                                .email("api@readscape.jp")
                                .url("https://readscape.jp/api/docs"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + "/api")
                                .description("開発環境"),
                        new Server()
                                .url("https://inventory-api-dev.readscape.jp/api")
                                .description("テスト環境"),
                        new Server()
                                .url("https://inventory-api.readscape.jp/api")
                                .description("本番環境")
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("管理者用JWT トークンによる認証")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("inventory-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminBooksApi() {
        return GroupedOpenApi.builder()
                .group("admin-books")
                .displayName("管理者書籍API")
                .pathsToMatch("/api/admin/books/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminInventoryApi() {
        return GroupedOpenApi.builder()
                .group("admin-inventory")
                .displayName("管理者在庫API")
                .pathsToMatch("/api/admin/inventory/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .displayName("認証API")
                .pathsToMatch("/api/auth/**")
                .build();
    }
}