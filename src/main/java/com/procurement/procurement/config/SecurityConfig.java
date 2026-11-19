package com.procurement.procurement.config;

import com.procurement.procurement.security.CustomUserDetailsService;
import com.procurement.procurement.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF for REST and demo compatibility
                .csrf(csrf -> csrf.disable())

                // Session management (IF_REQUIRED supports both web sessions and stateless REST tokens)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .authorizeHttpRequests(auth -> auth

                        // 1. Static resources and public web login
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**")
                        .permitAll()

                        // 2. REST Authentication API endpoints
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        // 3. Swagger / OpenAPI documentation (Admin & Procurement Manager only)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .hasAnyRole("ADMIN", "PROCUREMENT_MANAGER")

                        // 4. Dashboard (Web)
                        .requestMatchers("/dashboard")
                        .authenticated()

                        // 5. Vendor Management
                        .requestMatchers("/vendor/delete/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/vendor/**", "/api/vendor/**")
                        .hasAnyRole("ADMIN", "PROCUREMENT_MANAGER")

                        // 6. Requisition Management
                        .requestMatchers("/requisition/**", "/api/procurement/requisition/**")
                        .hasAnyRole("ADMIN", "PROCUREMENT_MANAGER", "EMPLOYEE")

                        // 7. Approval Center (Admin & Procurement Manager only)
                        .requestMatchers("/approval/**", "/api/procurement/approval/**")
                        .hasAnyRole("ADMIN", "PROCUREMENT_MANAGER")

                        // 8. Purchase Order Management (Admin & Procurement Manager only)
                        .requestMatchers("/purchase-order/**", "/api/procurement/purchase-order/**")
                        .hasAnyRole("ADMIN", "PROCUREMENT_MANAGER")

                        // 9. Reporting APIs
                        .requestMatchers("/api/reports/**")
                        .hasAnyRole("ADMIN", "PROCUREMENT_MANAGER")

                        // 10. Any other request
                        .anyRequest()
                        .authenticated()
                )

                // Thymeleaf form login
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )

                // Logout configuration
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )

                // Hybrid Exception Handling: JSON 401/403 for /api/**, redirect for Web
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required or token expired\"}");
                                response.getWriter().flush();
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                                response.getWriter().flush();
                            } else {
                                response.sendRedirect("/dashboard");
                            }
                        })
                )

                // Register JWT authentication filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
