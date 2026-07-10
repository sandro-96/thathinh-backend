package vn.thathinh.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.thathinh.security.CustomUserDetails;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(FrontendCorsProperties.class)
public class AppConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
                return Optional.of(details.getUserId());
            }
            return Optional.of("system");
        };
    }
}
