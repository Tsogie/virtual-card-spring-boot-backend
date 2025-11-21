package net.otgon.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key secretKey;

    @PostConstruct
    private void init() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new RuntimeException("JWT secret is not configured");
        }
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public Key getSecretKey() {
        return secretKey;
    }

    public String extractUsername(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return claimsJws.getBody().getSubject();
        } catch (ExpiredJwtException e) {
            // Log at INFO level instead of ERROR - this is expected behavior
            System.out.println("[JWT] Token expired for user: " + e.getClaims().getSubject());
            throw new RuntimeException("Token expired", e);
        } catch (JwtException e) {
            // This is unexpected - log as warning
            System.out.println("[JWT] Invalid token: " + e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }
}
