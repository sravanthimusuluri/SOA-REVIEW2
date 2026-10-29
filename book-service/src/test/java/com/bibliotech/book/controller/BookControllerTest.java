package com.bibliotech.book.controller;

import com.bibliotech.book.config.JwtAuthenticationFilter;
import com.bibliotech.book.dto.AvailabilityResponse;
import com.bibliotech.book.dto.BookRequest;
import com.bibliotech.book.dto.BookResponse;
import com.bibliotech.book.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POSITIVE: GET /api/books returns list of books")
    void testGetAllBooks_Success() throws Exception {
        BookResponse book = new BookResponse(1L, "Building Microservices", "Sam Newman", "978-1492034025", "Cloud Architecture", 5, 4, "Main Library");
        when(bookService.getAllBooks(any(), any(), any())).thenReturn(List.of(book));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Building Microservices"));
    }

    @Test
    @DisplayName("POSITIVE: GET /api/books/{id} returns book details")
    void testGetBookById_Success() throws Exception {
        BookResponse book = new BookResponse(1L, "Building Microservices", "Sam Newman", "978-1492034025", "Cloud Architecture", 5, 4, "Main Library");
        when(bookService.getBookById(1L)).thenReturn(book);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Building Microservices"));
    }

    @Test
    @DisplayName("NEGATIVE: GET /api/books/{id} for non-existent book returns 400 Bad Request")
    void testGetBookById_NotFound() throws Exception {
        when(bookService.getBookById(999L)).thenThrow(new IllegalArgumentException("Book not found with ID: 999"));

        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Book not found with ID: 999"));
    }

    @Test
    @DisplayName("POSITIVE: GET /api/books/{id}/availability returns availability")
    void testCheckAvailability_Success() throws Exception {
        AvailabilityResponse resp = new AvailabilityResponse(1L, "Building Microservices", 4, 5, true);
        when(bookService.checkAvailability(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/books/1/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availableCopies").value(4));
    }

    @Test
    @DisplayName("POSITIVE: POST /api/books creates book and returns 201 Created")
    void testCreateBook_Success() throws Exception {
        BookRequest request = new BookRequest("Clean Code", "Robert C. Martin", "978-0132350884", "Software Engineering", 5, "Main Campus");
        BookResponse response = new BookResponse(2L, "Clean Code", "Robert C. Martin", "978-0132350884", "Software Engineering", 5, 5, "Main Campus");

        when(bookService.createBook(any(BookRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    @DisplayName("POSITIVE: POST /api/books/{id}/decrement decrements copy")
    void testDecrementCopies_Success() throws Exception {
        AvailabilityResponse resp = new AvailabilityResponse(1L, "Building Microservices", 3, 5, true);
        when(bookService.decrementCopies(1L)).thenReturn(resp);

        mockMvc.perform(post("/api/books/1/decrement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(3));
    }

    @Test
    @DisplayName("POSITIVE: POST /api/books/{id}/increment increments copy")
    void testIncrementCopies_Success() throws Exception {
        AvailabilityResponse resp = new AvailabilityResponse(1L, "Building Microservices", 4, 5, true);
        when(bookService.incrementCopies(1L)).thenReturn(resp);

        mockMvc.perform(post("/api/books/1/increment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(4));
    }
}
