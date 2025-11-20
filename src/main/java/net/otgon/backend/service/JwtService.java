package net.otgon.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
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
        } catch (JwtException e) {
            throw new RuntimeException("Invalid token", e);
        }
    }
}
