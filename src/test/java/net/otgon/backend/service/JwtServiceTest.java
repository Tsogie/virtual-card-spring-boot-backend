package net.otgon.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService
 * Service. 1. generates signed token with secret key, 2. extracts username from token
 */
@SpringBootTest 
@TestPropertySource(properties = {
        "jwt.secret=dGVzdFNlY3JldEtleUZvclVuaXRUZXN0aW5nTWluaW11bTMyQ2hhcnNMb25nMTIzNDU=",
        "jwt.expiration=3600000"
})
@DisplayName("JwtService Unit Tests")
public class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    // =====================================================
    // TEST 1: EXTRACT USERNAME - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Test 1: Extract Username Success - Valid token returns correct username")
    void testExtractUsernameSuccess() {

        // ============ ARRANGE ============
        String username = "testuser";
        // Create a valid token using the real service's secret key
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) 
                .signWith(jwtService.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // ============ ACT ============
        String extractedUsername = jwtService.extractUsername(token);

        // ============ ASSERT ============
        assertNotNull(extractedUsername);
        assertEquals(username, extractedUsername);
    }
    // =====================================================
    // TEST 2: EXTRACT USERNAME - FAIL, EXPIRED TOKEN
    // =====================================================
    @Test
    @DisplayName("Test 2: Extract Username Fail - Expired token returns error")
    void testExtractUsernameWithExpiredToken() {
        // ============ ARRANGE ============
        String username = "testuser";
        // Create an expired token using the real service's secret key
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) 
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) 
                .signWith(jwtService.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jwtService.extractUsername(token),
                "Expected RuntimeException(ExpiredJwtException) for invalid signature"
        );

        // ============ ASSERT ============

        // Verify exception message
        assertEquals("Token expired", exception.getMessage());
    }

    // =====================================================
    // TEST 3: EXTRACT USERNAME - FAIL, INVALID TOKEN
    // =====================================================
    @Test
    @DisplayName("Test 3: Extract Username Fail - Invalid(Malformed) token returns error")
    void testExtractUsernameWithInvalidToken() {
        // ============ ARRANGE ============
        String username = "testuser";

        // Invalid token
        String invalidToken = "It.Is.Invalid.token";

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jwtService.extractUsername(invalidToken),
                "Expected RuntimeException(JwtException) for invalid signature"
        );

        // ============ ASSERT ============

        // Verify exception message
        assertEquals("Invalid token", exception.getMessage());
    }

    // =====================================================
    // TEST 4: EXTRACT USERNAME - FAIL, TOKEN SIGNED WITH WRONG KEY
    // =====================================================
    @Test
    @DisplayName("Test 4: Extract Username Fail - Token signed with wrong key")
    void testExtractUsernameWithWrongKey() {
        // ============ ARRANGE ============
        String username = "testuser";

        // Create a DIFFERENT secret key 
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrongSecretKeyThatIsAtLeast32CharactersLongForTesting123".getBytes()
        );

        // Create token signed with WRONG key
        String tokenWithWrongSignature = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        // ============ ACT & ASSERT ============
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jwtService.extractUsername(tokenWithWrongSignature),
                "Expected RuntimeException(JwtException) for invalid signature"
        );

        // ============ ASSERT ============

        // Verify exception message
        assertEquals("Invalid token", exception.getMessage());
    }

    // =====================================================
    // TEST 5: GET SECRET KEY - SUCCESS, KEY INITIALIZED
    // =====================================================
    @Test
    @DisplayName("Test 5: Get Secret Key Success - Key initialized")
    void testGetSecretKeyNotNull() {
        Key key = jwtService.getSecretKey();
        assertNotNull(key, "Secret key should be initialized");
    }

    // =====================================================
    // TEST 6: GENERATE TOKEN - SUCCESS, CREATE VALID JWT
    // =====================================================
    @Test
    @DisplayName("Test 6: Generate Token Success - Creates valid JWT")
    void testGenerateTokenSuccess() {
        // ============ ARRANGE ============
        String username = "alice";

        // ============ ACT ============
        String token = jwtService.generateToken(username);

        // ============ ASSERT ============
        // 1. Token should not be null
        assertNotNull(token);

        // 2. Token should be a long string
        assertTrue(token.length() > 100, "JWT should be a long string");

        // 3. Verify we can extract username back
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(username, extractedUsername);
    }
}
