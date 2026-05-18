package com.connecthub.websocket_handler.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${auth.jwt.secret:connecthub-dev-secret-key-change-me-please-32bytes}")
    private String secret;

    /** Extract userId (subject) from a JWT token */
    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    /** Validate token — returns false if expired or tampered */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}


