package com.bibliotech.book.service;

import com.bibliotech.book.dto.AvailabilityResponse;
import com.bibliotech.book.dto.BookRequest;
import com.bibliotech.book.dto.BookResponse;
import com.bibliotech.book.entity.Book;
import com.bibliotech.book.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookResponse> getAllBooks(String keyword, String category, String branch) {
        List<Book> books;
        if ((keyword != null && !keyword.isBlank()) ||
            (category != null && !category.isBlank()) ||
            (branch != null && !branch.isBlank())) {
            books = bookRepository.searchBooks(
                    (keyword != null && !keyword.isBlank()) ? keyword.trim() : null,
                    (category != null && !category.isBlank()) ? category.trim() : null,
                    (branch != null && !branch.isBlank()) ? branch.trim() : null
            );
        } else {
            books = bookRepository.findAll();
        }

        return books.stream().map(BookResponse::fromEntity).collect(Collectors.toList());
    }

    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
        return BookResponse.fromEntity(book);
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        Book book = new Book(
                request.getTitle(),
                request.getAuthor(),
                request.getIsbn(),
                request.getCategory(),
                request.getTotalCopies(),
                request.getTotalCopies(), // initially all available
                request.getBranch()
        );

        Book saved = bookRepository.save(book);
        return BookResponse.fromEntity(saved);
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));

        // If ISBN changed, check uniqueness
        if (!book.getIsbn().equals(request.getIsbn()) && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        int borrowed = book.getTotalCopies() - book.getAvailableCopies();
        if (request.getTotalCopies() < borrowed) {
            throw new IllegalArgumentException("Total copies cannot be less than currently borrowed copies (" + borrowed + ")");
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setCategory(request.getCategory());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies() - borrowed);
        book.setBranch(request.getBranch());

        Book saved = bookRepository.save(book);
        return BookResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
        
        int borrowed = book.getTotalCopies() - book.getAvailableCopies();
        if (borrowed > 0) {
            throw new IllegalStateException("Cannot delete book while " + borrowed + " copies are currently borrowed");
        }

        bookRepository.delete(book);
    }

    public AvailabilityResponse checkAvailability(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
        return new AvailabilityResponse(
                book.getId(),
                book.getTitle(),
                book.getAvailableCopies(),
                book.getTotalCopies(),
                book.getAvailableCopies() > 0
        );
    }

    @Transactional
    public AvailabilityResponse decrementCopies(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));

        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("Book '" + book.getTitle() + "' is currently out of stock (0 available copies)");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        Book saved = bookRepository.save(book);

        return new AvailabilityResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getAvailableCopies(),
                saved.getTotalCopies(),
                saved.getAvailableCopies() > 0
        );
    }

    @Transactional
    public AvailabilityResponse incrementCopies(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));

        if (book.getAvailableCopies() < book.getTotalCopies()) {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            Book saved = bookRepository.save(book);
            return new AvailabilityResponse(
                    saved.getId(),
                    saved.getTitle(),
                    saved.getAvailableCopies(),
                    saved.getTotalCopies(),
                    saved.getAvailableCopies() > 0
            );
        }

        return new AvailabilityResponse(
                book.getId(),
                book.getTitle(),
                book.getAvailableCopies(),
                book.getTotalCopies(),
                true
        );
    }
}
