package jp.readscape.consumer.configurations.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

@Configuration
public class RoleHierarchyConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        String hierarchy = """
            ROLE_ADMIN > ROLE_MANAGER
            ROLE_ADMIN > ROLE_ANALYST
            ROLE_MANAGER > ROLE_CONSUMER
            ROLE_ANALYST > ROLE_CONSUMER
            """;
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }
}