package jp.readscape.consumer.config;

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

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI consumerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Readscape-JP Consumer API")
                        .version(appVersion)
                        .description("日本語対応書籍販売システムの消費者向けREST API")
                        .contact(new Contact()
                                .name("Readscape-JP API Team")
                                .email("api@readscape.jp")
                                .url("https://readscape.jp/api/docs"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("開発環境"),
                        new Server()
                                .url("https://consumer-api-dev.readscape.jp")
                                .description("テスト環境"),
                        new Server()
                                .url("https://consumer-api.readscape.jp")
                                .description("本番環境")
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT トークンによる認証")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("consumer-api")
                .pathsToMatch("/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    @Bean
    public GroupedOpenApi booksApi() {
        return GroupedOpenApi.builder()
                .group("books-api")
                .displayName("書籍API")
                .pathsToMatch("/books/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth-api")
                .displayName("認証API")
                .pathsToMatch("/auth/**", "/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi shoppingApi() {
        return GroupedOpenApi.builder()
                .group("shopping-api")
                .displayName("ショッピングAPI")
                .pathsToMatch("/cart/**", "/orders/**")
                .build();
    }
}