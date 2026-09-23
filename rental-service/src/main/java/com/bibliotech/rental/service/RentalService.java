package com.bibliotech.rental.service;

import com.bibliotech.rental.client.BookClient;
import com.bibliotech.rental.client.FineClient;
import com.bibliotech.rental.dto.*;
import com.bibliotech.rental.entity.Rental;
import com.bibliotech.rental.entity.RentalStatus;
import com.bibliotech.rental.exception.DuplicateBorrowException;
import com.bibliotech.rental.repository.RentalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RentalService {

    private static final Logger logger = LoggerFactory.getLogger(RentalService.class);

    private final RentalRepository rentalRepository;
    private final BookClient bookClient;
    private final FineClient fineClient;
    private final int defaultLoanDays;

    public RentalService(
            RentalRepository rentalRepository,
            BookClient bookClient,
            FineClient fineClient,
            @Value("${bibliotech.rental.default-loan-days:14}") int defaultLoanDays) {
        this.rentalRepository = rentalRepository;
        this.bookClient = bookClient;
        this.fineClient = fineClient;
        this.defaultLoanDays = defaultLoanDays;
    }

    @Transactional
    public RentalResponse borrowBook(BorrowRequest request, String currentUsername, String currentStudentId) {
        String effectiveUsername = (currentUsername != null && !currentUsername.isBlank())
                ? currentUsername : request.getStudentUsername();
        String effectiveStudentId = (currentStudentId != null && !currentStudentId.isBlank())
                ? currentStudentId : request.getStudentId();

        if (effectiveUsername == null || effectiveUsername.isBlank()) {
            throw new IllegalArgumentException("Student username is required to borrow a book");
        }

        if (effectiveStudentId == null || effectiveStudentId.isBlank()) {
            effectiveStudentId = effectiveUsername;
        }

        // 1. BUSINESS RULE: PREVENT DUPLICATE BORROWING
        // Check if student already has an active loan (ISSUED or OVERDUE) for the same book
        Set<RentalStatus> activeStatuses = Set.of(RentalStatus.ISSUED, RentalStatus.OVERDUE);
        Optional<Rental> existingActive = rentalRepository.findActiveRentalByUsernameAndBookId(
                effectiveUsername, request.getBookId(), activeStatuses);

        if (existingActive.isPresent()) {
            Rental active = existingActive.get();
            throw new DuplicateBorrowException(
                    "Duplicate borrowing prohibited: Student '" + effectiveUsername +
                    "' already has an active loan (#" + active.getId() + ") for book ID " + request.getBookId() +
                    " (Status: " + active.getStatus() + ", Due Date: " + active.getDueDate() + ")");
        }

        // 2. CHECK AVAILABILITY via BookClient
        AvailabilityResponse availability;
        try {
            availability = bookClient.checkAvailability(request.getBookId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check book availability from book-service: " + e.getMessage(), e);
        }

        if (availability == null || !availability.isAvailable() || availability.getAvailableCopies() <= 0) {
            String title = (availability != null && availability.getTitle() != null) ? availability.getTitle() : "ID " + request.getBookId();
            throw new IllegalStateException("Book '" + title + "' currently has 0 available copies in inventory.");
        }

        // 3. DECREMENT BOOK COPIES via BookClient
        try {
            bookClient.decrementCopies(request.getBookId());
            logger.info("Successfully decremented inventory for book ID: {}", request.getBookId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reserve book copy from book-service: " + e.getMessage(), e);
        }

        // 4. CREATE RENTAL RECORD
        int loanDays = (request.getLoanDays() != null && request.getLoanDays() > 0)
                ? request.getLoanDays() : defaultLoanDays;

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(loanDays);

        Rental rental = new Rental(
                effectiveStudentId,
                effectiveUsername,
                request.getBookId(),
                availability.getTitle(),
                issueDate,
                dueDate
        );

        Rental saved = rentalRepository.save(rental);
        logger.info("Created rental loan #{} for student {} (book: '{}', due: {})",
                saved.getId(), effectiveUsername, saved.getBookTitle(), saved.getDueDate());

        return RentalResponse.fromEntity(saved);
    }

    @Transactional
    public RentalResponse returnBook(Long rentalId) {
        return returnBook(rentalId, null, true);
    }

    @Transactional
    public RentalResponse returnBook(Long rentalId, String currentUsername, boolean staff) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental record not found with ID: " + rentalId));

        if (!staff && (currentUsername == null || !currentUsername.equals(rental.getStudentUsername()))) {
            throw new IllegalStateException("You can only return your own rental records");
        }

        if (rental.getStatus() == RentalStatus.RETURNED) {
            throw new IllegalStateException("Rental #" + rentalId + " has already been returned on " + rental.getReturnDate());
        }

        LocalDate returnDate = LocalDate.now();
        rental.setReturnDate(returnDate);
        rental.setStatus(RentalStatus.RETURNED);

        // 1. INCREMENT BOOK COPIES via BookClient
        try {
            bookClient.incrementCopies(rental.getBookId());
            logger.info("Incremented inventory for book ID: {} following return", rental.getBookId());
        } catch (Exception e) {
            logger.error("Failed to increment book copies on return: {}", e.getMessage());
            // Proceed to not block student return record, but log warning
        }

        // 2. CHECK FOR OVERDUE & CALCULATE FINE via FineClient
        RentalResponse response = RentalResponse.fromEntity(rental);

        if (returnDate.isAfter(rental.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(rental.getDueDate(), returnDate);
            logger.warn("Rental #{} is returned {} days overdue! Triggering fine calculation...", rentalId, overdueDays);

            try {
                CalculateFineRequest fineReq = new CalculateFineRequest(
                        rental.getId(),
                        rental.getStudentId(),
                        rental.getStudentUsername(),
                        (int) overdueDays
                );
                FineResponse fineResp = fineClient.calculateFine(fineReq);

                rental.setFineId(fineResp.getId());
                rental.setNote("Returned " + overdueDays + " days late. Fine incurred: ₹" + fineResp.getAmount());

                response.setFineId(fineResp.getId());
                response.setFineAmount(fineResp.getAmount());
                response.setFineStatus(fineResp.getStatus());
                response.setNote(rental.getNote());
                response.setOverdue(true);

                logger.info("Automated fine #{} recorded: ₹{}", fineResp.getId(), fineResp.getAmount());
            } catch (Exception e) {
                logger.error("Failed to record fine via fine-service: {}", e.getMessage());
                rental.setNote("Returned " + overdueDays + " days late. Note: Fine recording service encountered an issue.");
            }
        } else {
            rental.setNote("Returned on time. No overdue penalty.");
            response.setNote(rental.getNote());
            response.setOverdue(false);
        }

        Rental saved = rentalRepository.save(rental);
        response.setId(saved.getId());
        response.setReturnDate(saved.getReturnDate());
        response.setStatus(saved.getStatus());

        return response;
    }

    public List<RentalResponse> getRentalsByStudentId(String studentId) {
        return rentalRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::enrichWithFineDetails)
                .collect(Collectors.toList());
    }

    public List<RentalResponse> getRentalsByUsername(String username) {
        return rentalRepository.findByStudentUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::enrichWithFineDetails)
                .collect(Collectors.toList());
    }

    public List<RentalResponse> getAllRentals() {
        return rentalRepository.findAll().stream()
                .map(this::enrichWithFineDetails)
                .collect(Collectors.toList());
    }

    public List<RentalResponse> getOverdueRentals() {
        LocalDate today = LocalDate.now();
        List<Rental> overdueList = rentalRepository.findOverdueRentals(today);
        return overdueList.stream()
                .map(r -> {
                    RentalResponse resp = RentalResponse.fromEntity(r);
                    resp.setOverdue(true);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public RentalResponse simulateOverdue(Long rentalId, int daysPastDue) {
        return simulateOverdue(rentalId, daysPastDue, null, true);
    }

    @Transactional
    public RentalResponse simulateOverdue(Long rentalId, int daysPastDue, String currentUsername, boolean staff) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental record not found with ID: " + rentalId));

        if (!staff && (currentUsername == null || !currentUsername.equals(rental.getStudentUsername()))) {
            throw new IllegalStateException("You can only simulate overdue status for your own rental records");
        }

        if (rental.getStatus() == RentalStatus.RETURNED) {
            throw new IllegalStateException("Cannot simulate overdue on an already returned rental");
        }

        // Set dueDate to `daysPastDue` days in the past
        rental.setDueDate(LocalDate.now().minusDays(daysPastDue));
        rental.setStatus(RentalStatus.OVERDUE);
        Rental updated = rentalRepository.save(rental);

        RentalResponse response = RentalResponse.fromEntity(updated);
        response.setOverdue(true);
        response.setNote("Simulated " + daysPastDue + " days overdue for demonstration");
        return response;
    }

    private RentalResponse enrichWithFineDetails(Rental rental) {
        RentalResponse response = RentalResponse.fromEntity(rental);
        // Check dynamic overdue state
        if (rental.getStatus() == RentalStatus.ISSUED && LocalDate.now().isAfter(rental.getDueDate())) {
            response.setOverdue(true);
        }
        return response;
    }
}
