package com.bibliotech.fine.controller;

import com.bibliotech.fine.dto.CalculateFineRequest;
import com.bibliotech.fine.dto.FineResponse;
import com.bibliotech.fine.service.FineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<FineResponse> calculateFine(@Valid @RequestBody CalculateFineRequest request) {
        FineResponse response = fineService.calculateAndRecordFine(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<List<FineResponse>> getFinesByStudentId(@PathVariable("studentId") String studentId) {
        return ResponseEntity.ok(fineService.getFinesByStudentId(studentId));
    }

    @GetMapping("/my-fines")
    public ResponseEntity<List<FineResponse>> getMyFines(@AuthenticationPrincipal String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(fineService.getFinesByStudentUsername(username));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<List<FineResponse>> getAllFines() {
        return ResponseEntity.ok(fineService.getAllFines());
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<FineResponse> payFine(@PathVariable("id") Long id, Authentication authentication) {
        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_LIBRARIAN"));
        return ResponseEntity.ok(fineService.payFine(id, authentication.getName(), staff));
    }
}
