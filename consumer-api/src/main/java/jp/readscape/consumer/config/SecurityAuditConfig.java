package jp.readscape.consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Configuration
@Slf4j
public class SecurityAuditConfig {

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = getUsername(authentication);
        String clientIp = getClientIp(authentication);
        
        log.info("Authentication success - Username: {}, IP: {}, Timestamp: {}", 
                username, clientIp, System.currentTimeMillis());
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = getUsername(authentication);
        String clientIp = getClientIp(authentication);
        String reason = event.getException().getMessage();
        
        log.warn("Authentication failure - Username: {}, IP: {}, Reason: {}, Timestamp: {}", 
                username, clientIp, reason, System.currentTimeMillis());
    }

    @EventListener 
    public void onAuthorizationDenied(AuthorizationDeniedEvent event) {
        Authentication authentication = event.getAuthentication().get();
        String username = getUsername(authentication);
        String resource = event.getAuthorizationDecision().toString();
        
        log.warn("Authorization denied - Username: {}, Resource: {}, Timestamp: {}", 
                username, resource, System.currentTimeMillis());
    }

    private String getUsername(Authentication authentication) {
        if (authentication == null) {
            return "anonymous";
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        return "unknown";
    }

    private String getClientIp(Authentication authentication) {
        if (authentication == null) {
            return "unknown";
        }
        
        Object details = authentication.getDetails();
        if (details instanceof WebAuthenticationDetails webDetails) {
            return webDetails.getRemoteAddress();
        }
        
        return "unknown";
    }
}