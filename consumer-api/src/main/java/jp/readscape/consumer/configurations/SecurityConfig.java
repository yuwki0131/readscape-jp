package jp.readscape.consumer.configurations;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                          JwtAuthenticationFilter jwtAuthFilter,
                                          DaoAuthenticationProvider authProvider) throws Exception {
        HttpSecurity httpSecurity = http
                .csrf(csrf -> {
                    // JWTベースAPIのためCSRFは基本的に無効化
                    // ただし、本番環境では追加のセキュリティレイヤーとしてSameSite cookieを使用
                    csrf.disable();
                    // 注意: Cookieベースの認証を使用する場合はCSRFを有効にする必要がある
                })
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth
                        // パブリックエンドポイント
                        .requestMatchers("/health", "/health/**", "/actuator/**").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/books/**", "/books/**").permitAll()  // 書籍閲覧は認証不要
                        .requestMatchers(HttpMethod.GET, "/api/books/*/reviews/**").permitAll()  // レビュー閲覧のみ認証不要
                        .requestMatchers(HttpMethod.POST, "/api/books/*/reviews/*/helpful").permitAll()  // 「役立った」マークは認証不要

                        // 認証が必要なレビューエンドポイント
                        .requestMatchers("/api/books/reviews/my-reviews").authenticated()  // ユーザーレビュー一覧は認証必要

                        // 認証関連エンドポイント
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        .requestMatchers("/api/users/check-username", "/api/users/check-email").permitAll()

                        // ロールベースアクセス制御
                        .requestMatchers("/api/cart/**").hasRole("CONSUMER")
                        .requestMatchers("/api/orders/**").hasRole("CONSUMER")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "ANALYST")

                        // 認証が必要なエンドポイント
                        .requestMatchers("/api/users/**").authenticated();

                    // H2 Console - 開発環境でのみ許可
                    if (isDevEnvironment()) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // H2 Console設定 - 開発環境でのみ
        if (isDevEnvironment()) {
            httpSecurity.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
        }

        return httpSecurity.build();
    }
    
    private boolean isDevEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile) || "test".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                           PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}