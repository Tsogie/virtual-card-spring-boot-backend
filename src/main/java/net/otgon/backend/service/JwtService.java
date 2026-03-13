package net.otgon.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.security.Key;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    private Key secretKey;

    @PostConstruct
    void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured — check JWT_SECRET env var");
        }
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public JwtService() {}

    public JwtService(String secret) {
        this.jwtSecret = secret;
        init();
    }

    public String extractUsername(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return claimsJws.getBody().getSubject();
        } catch (ExpiredJwtException e) {
            System.out.println("[JWT] Token expired for user: " + e.getClaims().getSubject());
            throw new RuntimeException("Token expired", e);
        } catch (JwtException e) {
            System.out.println("[JWT] Invalid token: " + e.getMessage());
            throw new RuntimeException("Invalid token", e);
        } catch (Exception e) {
            throw new RuntimeException("[JWT] Unexpected exception", e);
        }
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) 
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateExpiredTokenForTesting(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
