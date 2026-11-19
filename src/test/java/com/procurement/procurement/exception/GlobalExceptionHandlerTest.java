package com.procurement.procurement.exception;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/api/procurement/approval");
        webRequest = new ServletWebRequest(mockRequest);
    }

    @Test
    @DisplayName("Should return HTTP 403 Forbidden when AccessDeniedException is thrown")
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<Object> response = exceptionHandler.handleAccessDenied(ex, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(403, body.get("status"));
        assertEquals("Forbidden", body.get("error"));
    }

    @Test
    @DisplayName("Should return HTTP 401 Unauthorized when BadCredentialsException is thrown")
    void testHandleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Invalid username or password");

        ResponseEntity<Object> response = exceptionHandler.handleAuthenticationException(ex, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
    }

    @Test
    @DisplayName("Should return HTTP 401 Unauthorized when JwtException is thrown")
    void testHandleJwtException() {
        MalformedJwtException ex = new MalformedJwtException("JWT strings must contain exactly 2 period characters");

        ResponseEntity<Object> response = exceptionHandler.handleAuthenticationException(ex, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
    }

    @Test
    @DisplayName("Should return HTTP 404 Not Found when ResourceNotFoundException is thrown")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Vendor not found with id: 42");

        ResponseEntity<Object> response = exceptionHandler.handleResourceNotFound(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(404, body.get("status"));
        assertEquals("Resource Not Found", body.get("error"));
    }
}
