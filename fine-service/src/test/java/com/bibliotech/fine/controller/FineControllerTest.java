package com.bibliotech.fine.controller;

import com.bibliotech.fine.config.JwtAuthenticationFilter;
import com.bibliotech.fine.dto.CalculateFineRequest;
import com.bibliotech.fine.dto.FineResponse;
import com.bibliotech.fine.entity.FineStatus;
import com.bibliotech.fine.service.FineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FineController.class)
@AutoConfigureMockMvc(addFilters = false)
class FineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FineService fineService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POSITIVE: POST /api/fines/calculate calculates and records fine (201 Created)")
    void testCalculateFine_Success() throws Exception {
        CalculateFineRequest request = new CalculateFineRequest(100L, "2400030661", "sravanthi", 4);
        FineResponse response = new FineResponse(1L, 100L, "2400030661", "sravanthi", 4,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(40.00), FineStatus.UNPAID, LocalDateTime.now(), null);

        when(fineService.calculateAndRecordFine(any(CalculateFineRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/fines/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(40.0))
                .andExpect(jsonPath("$.status").value("UNPAID"));
    }

    @Test
    @DisplayName("NEGATIVE: POST /api/fines/calculate with invalid days returns 400 Bad Request")
    void testCalculateFine_InvalidDays_BadRequest() throws Exception {
        CalculateFineRequest request = new CalculateFineRequest(100L, "2400030661", "sravanthi", -1);

        when(fineService.calculateAndRecordFine(any(CalculateFineRequest.class)))
                .thenThrow(new IllegalArgumentException("Overdue days must be greater than zero"));

        mockMvc.perform(post("/api/fines/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.overdueDays").value("Overdue days must be at least 1"));
    }

    @Test
    @DisplayName("POSITIVE: GET /api/fines/my-fines returns current user's fines")
    void testGetMyFines_Success() throws Exception {
        FineResponse fine = new FineResponse(1L, 100L, "2400030661", "sravanthi", 3,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00), FineStatus.UNPAID, LocalDateTime.now(), null);

        when(fineService.getFinesByStudentUsername("sravanthi")).thenReturn(List.of(fine));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "sravanthi", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        mockMvc.perform(get("/api/fines/my-fines").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentUsername").value("sravanthi"))
                .andExpect(jsonPath("$[0].amount").value(30.0));
    }

    @Test
    @DisplayName("POSITIVE: PUT /api/fines/{id}/pay pays fine successfully")
    void testPayFine_Success() throws Exception {
        FineResponse response = new FineResponse(1L, 100L, "2400030661", "sravanthi", 3,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00), FineStatus.PAID, LocalDateTime.now(), LocalDateTime.now());

        when(fineService.payFine(eq(1L), anyString(), anyBoolean())).thenReturn(response);

        TestingAuthenticationToken auth = new TestingAuthenticationToken("sravanthi", "credentials", "ROLE_STUDENT");

        mockMvc.perform(put("/api/fines/1/pay").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
