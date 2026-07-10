package vn.thathinh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record FrontendCorsProperties(List<String> allowedOriginPatterns) {
    public FrontendCorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            allowedOriginPatterns = List.of("http://localhost:5173");
        }
    }
}
