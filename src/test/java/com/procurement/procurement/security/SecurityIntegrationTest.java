package com.procurement.procurement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.procurement.dto.auth.LoginRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // ==========================================
    // 1. Thymeleaf Web Login & Access
    // ==========================================

    @Test
    @DisplayName("Thymeleaf /login page should be publicly accessible")
    void testLoginPagePublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Unauthenticated access to /dashboard should redirect to /login")
    void testUnauthenticatedDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Web form login with valid credentials succeeds and redirects to /dashboard")
    void testWebFormLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "adminTestPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Web form login with invalid credentials fails and redirects to /login?error")
    void testWebFormLoginFailure() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "wrongPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    // ==========================================
    // 2. REST API JWT Authentication (/api/auth/login)
    // ==========================================

    @Test
    @DisplayName("REST /api/auth/login with valid credentials returns 200 and JWT token")
    void testRestLoginSuccess() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("manager", "managerTestPassword123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertNotNull(responseJson);
    }

    @Test
    @DisplayName("REST /api/auth/login with bad credentials returns 401 Unauthorized")
    void testRestLoginBadCredentials() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("manager", "wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ==========================================
    // 3. REST API Token Validation & Role Restrictions
    // ==========================================

    @Test
    @DisplayName("Accessing protected API without token returns 401 Unauthorized")
    void testApiMissingTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/procurement/approval/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Accessing protected API with malformed token returns 401 Unauthorized (not 500)")
    void testApiMalformedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/procurement/approval/all")
                        .header("Authorization", "Bearer invalid.malformed.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Employee token accessing Manager/Admin API returns 403 Forbidden")
    void testEmployeeAccessingApprovalApiReturns403() throws Exception {
        String employeeToken = jwtUtil.generateToken("employee");

        mockMvc.perform(get("/api/procurement/approval/all")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager token accessing Approval API returns 200 OK")
    void testManagerAccessingApprovalApiReturns200() throws Exception {
        String managerToken = jwtUtil.generateToken("manager");

        mockMvc.perform(get("/api/procurement/approval/all")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin token accessing Approval API returns 200 OK")
    void testAdminAccessingApprovalApiReturns200() throws Exception {
        String adminToken = jwtUtil.generateToken("admin");

        mockMvc.perform(get("/api/procurement/approval/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 4. Swagger / OpenAPI Endpoint Security
    // ==========================================

    @Test
    @DisplayName("Unauthenticated access to Swagger UI redirects or returns 401/403")
    void testSwaggerEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
