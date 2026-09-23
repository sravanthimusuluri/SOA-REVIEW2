package com.bibliotech.book.service;

import com.bibliotech.book.dto.AvailabilityResponse;
import com.bibliotech.book.entity.Book;
import com.bibliotech.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new Book(
                "Building Microservices",
                "Sam Newman",
                "978-1492034025",
                "Architecture",
                3,
                2,
                "Main Library"
        );
        sampleBook.setId(1L);
    }

    @Test
    void testCheckAvailability_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        AvailabilityResponse response = bookService.checkAvailability(1L);

        assertNotNull(response);
        assertEquals(1L, response.getBookId());
        assertEquals(2, response.getAvailableCopies());
        assertTrue(response.isAvailable());
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    void testDecrementCopies_WhenAvailable_DecrementsSuccessfully() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvailabilityResponse response = bookService.decrementCopies(1L);

        assertEquals(1, response.getAvailableCopies());
        assertTrue(response.isAvailable());
        verify(bookRepository, times(1)).save(sampleBook);
    }

    @Test
    void testDecrementCopies_WhenZeroAvailable_ThrowsException() {
        sampleBook.setAvailableCopies(0);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        assertThrows(IllegalStateException.class, () -> bookService.decrementCopies(1L));
        verify(bookRepository, never()).save(sampleBook);
    }

    @Test
    void testIncrementCopies_WhenBelowTotal_IncrementsSuccessfully() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvailabilityResponse response = bookService.incrementCopies(1L);

        assertEquals(3, response.getAvailableCopies());
        verify(bookRepository, times(1)).save(sampleBook);
    }
}
