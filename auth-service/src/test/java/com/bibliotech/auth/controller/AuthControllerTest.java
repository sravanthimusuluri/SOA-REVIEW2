package com.bibliotech.auth.controller;

import com.bibliotech.auth.config.JwtAuthenticationFilter;
import com.bibliotech.auth.dto.AuthResponse;
import com.bibliotech.auth.dto.LoginRequest;
import com.bibliotech.auth.dto.RegisterRequest;
import com.bibliotech.auth.dto.ValidateTokenResponse;
import com.bibliotech.auth.entity.Role;
import com.bibliotech.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POSITIVE: POST /api/auth/register returns 201 Created")
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("2400030661", "pass123", "Musuluri Sravanthi", "sravs@kluniversity.in", "2400030661", Role.STUDENT);
        AuthResponse response = new AuthResponse("mock-token", 1L, "2400030661", "sravs@kluniversity.in", "Musuluri Sravanthi", "2400030661", Role.STUDENT, 86400000L);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-token"))
                .andExpect(jsonPath("$.username").value("2400030661"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POSITIVE: POST /api/auth/login returns 200 OK")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest("2400030661", "pass123");
        AuthResponse response = new AuthResponse("mock-token", 1L, "2400030661", "sravs@kluniversity.in", "Musuluri Sravanthi", "2400030661", Role.STUDENT, 86400000L);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"))
                .andExpect(jsonPath("$.username").value("2400030661"));
    }

    @Test
    @DisplayName("NEGATIVE: POST /api/auth/login with invalid credentials returns 400 Bad Request")
    void testLogin_Failure() throws Exception {
        LoginRequest request = new LoginRequest("invalidUser", "wrongPass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POSITIVE: GET /api/auth/validate with valid token returns 200 OK")
    void testValidateToken_Success() throws Exception {
        ValidateTokenResponse response = new ValidateTokenResponse(true, "2400030661", "STUDENT", 1L, "2400030661", "Token is valid");

        when(authService.validateToken(any())).thenReturn(response);

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("2400030661"));
    }

    @Test
    @DisplayName("NEGATIVE: GET /api/auth/validate with invalid token returns 401 Unauthorized")
    void testValidateToken_Failure() throws Exception {
        ValidateTokenResponse response = new ValidateTokenResponse(false, null, null, null, null, "Token is invalid or expired");

        when(authService.validateToken(any())).thenReturn(response);

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
