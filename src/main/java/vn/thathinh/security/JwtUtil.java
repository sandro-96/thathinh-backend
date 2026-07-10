package vn.thathinh.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import vn.thathinh.model.User;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String DEFAULT_DEV_SECRET = "dev-secret-change-in-production-min-32-chars!!";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final Environment environment;

    private Key key;
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;

    public JwtUtil(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is missing");
        }
        boolean isDev = environment.getActiveProfiles().length == 0
                || Arrays.asList(environment.getActiveProfiles()).contains("dev")
                || Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (!isDev && DEFAULT_DEV_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to a strong value in production (default dev secret detected)");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) { return getClaims(token).getSubject(); }
    public String extractRole(String token) { return getClaims(token).get("role", String.class); }
    public String extractEmail(String token) { return getClaims(token).get("email", String.class); }

    public boolean isTokenValid(String token) {
        try { getClaims(token); return true; }
        catch (JwtException e) { return false; }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
