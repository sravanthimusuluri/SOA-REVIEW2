package com.bibliotech.book.controller;

import com.bibliotech.book.dto.AvailabilityResponse;
import com.bibliotech.book.dto.BookRequest;
import com.bibliotech.book.dto.BookResponse;
import com.bibliotech.book.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "branch", required = false) String branch) {
        return ResponseEntity.ok(bookService.getAllBooks(search, category, branch));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookService.checkAvailability(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        BookResponse response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> updateBook(@PathVariable("id") Long id, @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable("id") Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/decrement")
    public ResponseEntity<AvailabilityResponse> decrementCopies(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookService.decrementCopies(id));
    }

    @PostMapping("/{id}/increment")
    public ResponseEntity<AvailabilityResponse> incrementCopies(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookService.incrementCopies(id));
    }
}
