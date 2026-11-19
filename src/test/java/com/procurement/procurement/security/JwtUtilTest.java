package com.procurement.procurement.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String validSecret = "mySuperSecretKeyThatIsAtLeast32BytesLong!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", validSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract username")
    void testGenerateAndExtractUsername() {
        UserDetails userDetails = new User("admin", "password", Collections.emptyList());

        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("admin", extractedUsername);
    }

    @Test
    @DisplayName("Should validate token successfully against matching user details")
    void testValidateTokenSuccess() {
        UserDetails userDetails = new User("manager", "password", Collections.emptyList());
        String token = jwtUtil.generateToken(userDetails);

        boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertTrue(isValid);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Should fail validation if username does not match")
    void testValidateTokenMismatchedUser() {
        UserDetails userDetails1 = new User("manager", "password", Collections.emptyList());
        UserDetails userDetails2 = new User("employee", "password", Collections.emptyList());

        String token = jwtUtil.generateToken(userDetails1);

        assertFalse(jwtUtil.validateToken(token, userDetails2));
    }

    @Test
    @DisplayName("Should detect expired token and reject validation")
    void testExpiredToken() {
        // Set expiration to negative value (already expired)
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        UserDetails userDetails = new User("admin", "password", Collections.emptyList());

        String expiredToken = jwtUtil.generateToken(userDetails);

        assertTrue(jwtUtil.isTokenExpired(expiredToken));
        assertFalse(jwtUtil.validateToken(expiredToken, userDetails));
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject malformed token gracefully without throwing uncaught exception")
    void testMalformedToken() {
        String malformedToken = "invalid.bearer.token.structure";

        assertFalse(jwtUtil.validateToken(malformedToken));
        assertThrows(JwtException.class, () -> jwtUtil.extractUsername(malformedToken));
    }

    @Test
    @DisplayName("Should reject token signed with a different key")
    void testInvalidSignature() {
        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secret", "differentSecretKeyThatIsAtLeast32Bytes!");
        ReflectionTestUtils.setField(otherJwtUtil, "expiration", 3600000L);

        String foreignToken = otherJwtUtil.generateToken("admin");

        assertFalse(jwtUtil.validateToken(foreignToken));
    }

    @Test
    @DisplayName("Should fail when secret is null or less than 32 characters")
    void testMissingOrShortSecretThrowsException() {
        JwtUtil unconfiguredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(unconfiguredJwtUtil, "secret", "");

        assertThrows(IllegalStateException.class, () -> unconfiguredJwtUtil.generateToken("admin"));

        ReflectionTestUtils.setField(unconfiguredJwtUtil, "secret", "too-short");
        assertThrows(IllegalStateException.class, () -> unconfiguredJwtUtil.generateToken("admin"));
    }
}
