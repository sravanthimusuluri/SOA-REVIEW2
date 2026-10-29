package com.bibliotech.rental.controller;

import com.bibliotech.rental.dto.BorrowRequest;
import com.bibliotech.rental.dto.RentalResponse;
import com.bibliotech.rental.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping("/borrow")
    public ResponseEntity<RentalResponse> borrowBook(
            @Valid @RequestBody BorrowRequest request,
            @AuthenticationPrincipal String username,
            @RequestAttribute(value = "studentId", required = false) String studentId) {
        RentalResponse response = rentalService.borrowBook(request, username, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<RentalResponse> returnBook(@PathVariable("id") Long id, Authentication authentication) {
        RentalResponse response = rentalService.returnBook(id, authentication.getName(), isStaff(authentication));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-rentals")
    public ResponseEntity<List<RentalResponse>> getMyRentals(
            @AuthenticationPrincipal String username,
            Authentication authentication) {
        String effectiveUser = (username != null && !username.isBlank())
                ? username
                : (authentication != null ? authentication.getName() : null);
        if (effectiveUser == null || effectiveUser.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(rentalService.getRentalsByUsername(effectiveUser));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<List<RentalResponse>> getRentalsByStudentId(@PathVariable("studentId") String studentId) {
        return ResponseEntity.ok(rentalService.getRentalsByStudentId(studentId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<List<RentalResponse>> getAllRentals() {
        return ResponseEntity.ok(rentalService.getAllRentals());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<List<RentalResponse>> getOverdueRentals() {
        return ResponseEntity.ok(rentalService.getOverdueRentals());
    }

    @PostMapping("/{id}/simulate-overdue")
    public ResponseEntity<RentalResponse> simulateOverdue(
            @PathVariable("id") Long id,
            @RequestParam(value = "days", defaultValue = "5") int days,
            Authentication authentication) {
        return ResponseEntity.ok(rentalService.simulateOverdue(id, days, authentication.getName(), isStaff(authentication)));
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_LIBRARIAN"));
    }
}
