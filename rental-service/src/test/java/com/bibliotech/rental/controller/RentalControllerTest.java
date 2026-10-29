package com.bibliotech.rental.controller;

import com.bibliotech.rental.config.JwtAuthenticationFilter;
import com.bibliotech.rental.dto.BorrowRequest;
import com.bibliotech.rental.dto.RentalResponse;
import com.bibliotech.rental.entity.RentalStatus;
import com.bibliotech.rental.exception.DuplicateBorrowException;
import com.bibliotech.rental.service.RentalService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RentalController.class)
@AutoConfigureMockMvc(addFilters = false)
class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RentalService rentalService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POSITIVE: POST /api/rentals/borrow creates rental and returns 201 Created")
    void testBorrowBook_Success() throws Exception {
        BorrowRequest request = new BorrowRequest(10L, "2400030661", "sravanthi");
        RentalResponse response = new RentalResponse(1L, "2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.ISSUED, false, null, null, null, null);

        when(rentalService.borrowBook(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/rentals/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.bookTitle").value("Building Microservices"))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    @DisplayName("NEGATIVE: POST /api/rentals/borrow duplicate borrow returns 409 Conflict")
    void testBorrowBook_Duplicate_Conflict() throws Exception {
        BorrowRequest request = new BorrowRequest(10L, "2400030661", "sravanthi");

        when(rentalService.borrowBook(any(), any(), any()))
                .thenThrow(new DuplicateBorrowException("Duplicate borrowing prohibited"));

        mockMvc.perform(post("/api/rentals/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate Borrow Prohibited"));
    }

    @Test
    @DisplayName("POSITIVE: POST /api/rentals/{id}/return returns 200 OK")
    void testReturnBook_Success() throws Exception {
        RentalResponse response = new RentalResponse(1L, "2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(9), LocalDate.now(), RentalStatus.RETURNED, false, null, null, null, "Returned on time");

        when(rentalService.returnBook(any(), any(), anyBoolean())).thenReturn(response);

        TestingAuthenticationToken auth = new TestingAuthenticationToken("sravanthi", "credentials", "ROLE_STUDENT");

        mockMvc.perform(post("/api/rentals/1/return").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    @DisplayName("POSITIVE: GET /api/rentals/my-rentals returns user rentals")
    void testGetMyRentals_Success() throws Exception {
        RentalResponse response = new RentalResponse(1L, "2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.ISSUED, false, null, null, null, null);

        when(rentalService.getRentalsByUsername("sravanthi")).thenReturn(List.of(response));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "sravanthi", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        mockMvc.perform(get("/api/rentals/my-rentals").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentUsername").value("sravanthi"));
    }
}
