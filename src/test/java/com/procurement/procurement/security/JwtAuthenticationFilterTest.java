package com.procurement.procurement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySuperSecretKeyThatIsAtLeast32BytesLong!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);

        // Simple real UserDetailsService without bytecode mocking
        userDetailsService = new CustomUserDetailsService(null) {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                if ("unknown".equals(username)) {
                    throw new UsernameNotFoundException("User not found");
                }
                return new User(username, "encodedPassword",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROCUREMENT_MANAGER")));
            }
        };

        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should skip authentication when Authorization header is missing")
    void testNoAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filterChainExecuted = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> filterChainExecuted.set(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(filterChainExecuted.get());
    }

    @Test
    @DisplayName("Should successfully authenticate when valid Bearer token is provided")
    void testValidBearerToken() throws ServletException, IOException {
        String token = jwtUtil.generateToken("manager");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filterChainExecuted = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> filterChainExecuted.set(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("manager", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(filterChainExecuted.get());
    }

    @Test
    @DisplayName("Should handle expired token gracefully without throwing 500 error")
    void testExpiredToken() throws ServletException, IOException {
        // Generate an expired token
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secret", "mySuperSecretKeyThatIsAtLeast32BytesLong!");
        ReflectionTestUtils.setField(expiredJwtUtil, "expiration", -5000L);
        String expiredToken = expiredJwtUtil.generateToken("admin");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filterChainExecuted = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> filterChainExecuted.set(true);

        assertDoesNotThrow(() -> filter.doFilterInternal(request, response, filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(filterChainExecuted.get());
    }

    @Test
    @DisplayName("Should handle malformed token gracefully without throwing 500 error")
    void testMalformedToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filterChainExecuted = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> filterChainExecuted.set(true);

        assertDoesNotThrow(() -> filter.doFilterInternal(request, response, filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(filterChainExecuted.get());
    }
}
