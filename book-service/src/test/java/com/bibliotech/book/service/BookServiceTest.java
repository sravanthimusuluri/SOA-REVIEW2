package com.bibliotech.book.service;

import com.bibliotech.book.dto.AvailabilityResponse;
import com.bibliotech.book.dto.BookRequest;
import com.bibliotech.book.dto.BookResponse;
import com.bibliotech.book.entity.Book;
import com.bibliotech.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    @DisplayName("POSITIVE: Get all books without filters")
    void testGetAllBooks_NoFilter_Positive() {
        when(bookRepository.findAll()).thenReturn(List.of(sampleBook));

        List<BookResponse> books = bookService.getAllBooks(null, null, null);

        assertNotNull(books);
        assertEquals(1, books.size());
        assertEquals("Building Microservices", books.get(0).getTitle());
    }

    @Test
    @DisplayName("POSITIVE: Get all books with search keyword filter")
    void testGetAllBooks_WithFilter_Positive() {
        when(bookRepository.searchBooks("Microservices", null, null)).thenReturn(List.of(sampleBook));

        List<BookResponse> books = bookService.getAllBooks("Microservices", null, null);

        assertNotNull(books);
        assertEquals(1, books.size());
        assertEquals("Sam Newman", books.get(0).getAuthor());
    }

    @Test
    @DisplayName("POSITIVE: Get book by ID succeeds")
    void testGetBookById_Success_Positive() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        BookResponse response = bookService.getBookById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Building Microservices", response.getTitle());
    }

    @Test
    @DisplayName("NEGATIVE: Get book by ID throws exception when ID not found")
    void testGetBookById_NotFound_Negative() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookService.getBookById(999L));
        assertTrue(ex.getMessage().contains("Book not found with ID: 999"));
    }

    @Test
    @DisplayName("POSITIVE: Create book succeeds")
    void testCreateBook_Success_Positive() {
        BookRequest request = new BookRequest(
                "Clean Architecture",
                "Robert C. Martin",
                "978-0134494166",
                "Software Engineering",
                5,
                "Main Library"
        );

        when(bookRepository.existsByIsbn("978-0134494166")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book b = invocation.getArgument(0);
            b.setId(2L);
            return b;
        });

        BookResponse response = bookService.createBook(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Clean Architecture", response.getTitle());
        assertEquals(5, response.getAvailableCopies());
    }

    @Test
    @DisplayName("NEGATIVE: Create book throws exception when ISBN already exists")
    void testCreateBook_DuplicateIsbn_Negative() {
        BookRequest request = new BookRequest(
                "Duplicate Book",
                "Author",
                "978-1492034025",
                "Architecture",
                3,
                "Main Library"
        );

        when(bookRepository.existsByIsbn("978-1492034025")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookService.createBook(request));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("POSITIVE: Update book details succeeds")
    void testUpdateBook_Success_Positive() {
        BookRequest request = new BookRequest(
                "Building Microservices 2nd Edition",
                "Sam Newman",
                "978-1492034025",
                "Architecture",
                5,
                "Main Library"
        );

        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.updateBook(1L, request);

        assertNotNull(response);
        assertEquals("Building Microservices 2nd Edition", response.getTitle());
        assertEquals(5, response.getTotalCopies());
        // Original: total 3, available 2 => 1 borrowed. New total 5 => available 5 - 1 = 4
        assertEquals(4, response.getAvailableCopies());
    }

    @Test
    @DisplayName("NEGATIVE: Update book throws exception when total copies is less than borrowed copies")
    void testUpdateBook_TotalLessThanBorrowed_Negative() {
        // total = 3, available = 2 => borrowed = 1
        BookRequest request = new BookRequest(
                "Building Microservices",
                "Sam Newman",
                "978-1492034025",
                "Architecture",
                0, // Requested 0 which is less than 1 borrowed
                "Main Library"
        );

        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBook(1L, request));
        assertTrue(ex.getMessage().contains("Total copies cannot be less than currently borrowed copies"));
    }

    @Test
    @DisplayName("POSITIVE: Delete book with zero active borrows succeeds")
    void testDeleteBook_Success_Positive() {
        sampleBook.setAvailableCopies(3); // totalCopies = 3, available = 3 => borrowed = 0
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        assertDoesNotThrow(() -> bookService.deleteBook(1L));
        verify(bookRepository, times(1)).delete(sampleBook);
    }

    @Test
    @DisplayName("NEGATIVE: Delete book throws exception when copies are currently borrowed")
    void testDeleteBook_HasActiveBorrowedCopies_Negative() {
        // sampleBook: totalCopies = 3, availableCopies = 2 => 1 is borrowed
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookService.deleteBook(1L));
        assertTrue(ex.getMessage().contains("Cannot delete book while 1 copies are currently borrowed"));
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    @DisplayName("POSITIVE: Check availability returns correct stock")
    void testCheckAvailability_Success_Positive() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        AvailabilityResponse response = bookService.checkAvailability(1L);

        assertNotNull(response);
        assertEquals(1L, response.getBookId());
        assertEquals(2, response.getAvailableCopies());
        assertTrue(response.isAvailable());
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("POSITIVE: Decrement copies when stock available")
    void testDecrementCopies_WhenAvailable_DecrementsSuccessfully() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvailabilityResponse response = bookService.decrementCopies(1L);

        assertEquals(1, response.getAvailableCopies());
        assertTrue(response.isAvailable());
        verify(bookRepository, times(1)).save(sampleBook);
    }

    @Test
    @DisplayName("NEGATIVE: Decrement copies throws exception when zero copies available")
    void testDecrementCopies_WhenZeroAvailable_ThrowsException() {
        sampleBook.setAvailableCopies(0);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        assertThrows(IllegalStateException.class, () -> bookService.decrementCopies(1L));
        verify(bookRepository, never()).save(sampleBook);
    }

    @Test
    @DisplayName("POSITIVE: Increment copies on return")
    void testIncrementCopies_WhenBelowTotal_IncrementsSuccessfully() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvailabilityResponse response = bookService.incrementCopies(1L);

        assertEquals(3, response.getAvailableCopies());
        verify(bookRepository, times(1)).save(sampleBook);
    }
}
