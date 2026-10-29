package com.bibliotech.rental.service;

import com.bibliotech.rental.client.BookClient;
import com.bibliotech.rental.client.FineClient;
import com.bibliotech.rental.dto.*;
import com.bibliotech.rental.entity.Rental;
import com.bibliotech.rental.entity.RentalStatus;
import com.bibliotech.rental.exception.DuplicateBorrowException;
import com.bibliotech.rental.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private BookClient bookClient;

    @Mock
    private FineClient fineClient;

    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        rentalService = new RentalService(rentalRepository, bookClient, fineClient, 14);
    }

    @Test
    @DisplayName("NEGATIVE: Prevent duplicate borrowing for same book when loan is active")
    void testPreventDuplicateBorrowing_ThrowsDuplicateBorrowException() {
        BorrowRequest request = new BorrowRequest(10L, "2400030661", "sravanthi");

        Rental activeRental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(12));
        activeRental.setId(1L);

        when(rentalRepository.findActiveRentalByUsernameAndBookId(eq("sravanthi"), eq(10L), any()))
                .thenReturn(Optional.of(activeRental));

        assertThrows(DuplicateBorrowException.class, () ->
                rentalService.borrowBook(request, "sravanthi", "2400030661"));

        verify(bookClient, never()).decrementCopies(anyLong());
        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    @DisplayName("POSITIVE: Borrow book successfully when copy is available")
    void testBorrowBook_Success_DecrementsBookAndSavesRental() {
        BorrowRequest request = new BorrowRequest(10L, "2400030661", "sravanthi");

        when(rentalRepository.findActiveRentalByUsernameAndBookId(eq("sravanthi"), eq(10L), any()))
                .thenReturn(Optional.empty());

        AvailabilityResponse availability = new AvailabilityResponse(10L, "Building Microservices", 3, 5, true);
        when(bookClient.checkAvailability(10L)).thenReturn(availability);
        when(bookClient.decrementCopies(10L)).thenReturn(new AvailabilityResponse(10L, "Building Microservices", 2, 5, true));

        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental r = invocation.getArgument(0);
            r.setId(50L);
            return r;
        });

        RentalResponse response = rentalService.borrowBook(request, "sravanthi", "2400030661");

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("sravanthi", response.getStudentUsername());
        assertEquals(10L, response.getBookId());
        assertEquals("Building Microservices", response.getBookTitle());
        assertEquals(RentalStatus.ISSUED, response.getStatus());

        verify(bookClient, times(1)).checkAvailability(10L);
        verify(bookClient, times(1)).decrementCopies(10L);
        verify(rentalRepository, times(1)).save(any(Rental.class));
    }

    @Test
    @DisplayName("NEGATIVE: Borrow book throws exception when book has 0 available copies")
    void testBorrowBook_ZeroAvailableCopies_Negative() {
        BorrowRequest request = new BorrowRequest(10L, "2400030661", "sravanthi");

        when(rentalRepository.findActiveRentalByUsernameAndBookId(eq("sravanthi"), eq(10L), any()))
                .thenReturn(Optional.empty());

        AvailabilityResponse availability = new AvailabilityResponse(10L, "Building Microservices", 0, 5, false);
        when(bookClient.checkAvailability(10L)).thenReturn(availability);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> rentalService.borrowBook(request, "sravanthi", "2400030661"));
        assertTrue(ex.getMessage().contains("0 available copies"));
        verify(bookClient, never()).decrementCopies(anyLong());
        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    @DisplayName("NEGATIVE: Borrow book throws exception when student username is null/empty")
    void testBorrowBook_MissingUsername_Negative() {
        BorrowRequest request = new BorrowRequest(10L, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.borrowBook(request, null, null));
    }

    @Test
    @DisplayName("POSITIVE: Return book on time increments copies without triggering fine")
    void testReturnBook_OnTime_IncrementsCopiesAndNoFine() {
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(9));
        rental.setId(75L);

        when(rentalRepository.findById(75L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RentalResponse response = rentalService.returnBook(75L);

        assertNotNull(response);
        assertEquals(RentalStatus.RETURNED, response.getStatus());
        assertEquals(LocalDate.now(), response.getReturnDate());
        assertFalse(response.isOverdue());

        verify(bookClient, times(1)).incrementCopies(10L);
        verify(fineClient, never()).calculateFine(any());
        verify(rentalRepository, times(1)).save(rental);
    }

    @Test
    @DisplayName("POSITIVE: Return book overdue automatically calculates fine via fine-service")
    void testReturnBook_Overdue_CalculatesFine() {
        // Due 4 days ago
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(18), LocalDate.now().minusDays(4));
        rental.setId(88L);

        when(rentalRepository.findById(88L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FineResponse fineResponse = new FineResponse();
        fineResponse.setId(12L);
        fineResponse.setRentalId(88L);
        fineResponse.setStudentId("2400030661");
        fineResponse.setStudentUsername("sravanthi");
        fineResponse.setOverdueDays(4);
        fineResponse.setDailyRate(BigDecimal.valueOf(10.00));
        fineResponse.setAmount(BigDecimal.valueOf(40.00));
        fineResponse.setStatus("UNPAID");

        when(fineClient.calculateFine(any(CalculateFineRequest.class))).thenReturn(fineResponse);

        RentalResponse response = rentalService.returnBook(88L);

        assertNotNull(response);
        assertEquals(RentalStatus.RETURNED, response.getStatus());
        assertTrue(response.isOverdue());
        assertEquals(12L, response.getFineId());
        assertEquals(BigDecimal.valueOf(40.00), response.getFineAmount());

        verify(bookClient, times(1)).incrementCopies(10L);
        verify(fineClient, times(1)).calculateFine(any(CalculateFineRequest.class));
        verify(rentalRepository, times(1)).save(rental);
    }

    @Test
    @DisplayName("NEGATIVE: Return book throws exception when already returned")
    void testReturnBook_AlreadyReturned_Negative() {
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(4));
        rental.setId(90L);
        rental.setStatus(RentalStatus.RETURNED);
        rental.setReturnDate(LocalDate.now().minusDays(2));

        when(rentalRepository.findById(90L)).thenReturn(Optional.of(rental));

        assertThrows(IllegalStateException.class, () -> rentalService.returnBook(90L));
        verify(bookClient, never()).incrementCopies(anyLong());
    }

    @Test
    @DisplayName("NEGATIVE: Return book throws exception when record does not exist")
    void testReturnBook_NotFound_Negative() {
        when(rentalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> rentalService.returnBook(999L));
    }

    @Test
    @DisplayName("NEGATIVE: Non-staff user attempting to return another student's rental throws exception")
    void testReturnBook_DifferentStudentNotStaff_Negative() {
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(9));
        rental.setId(75L);

        when(rentalRepository.findById(75L)).thenReturn(Optional.of(rental));

        assertThrows(IllegalStateException.class,
                () -> rentalService.returnBook(75L, "otherStudent", false));
    }

    @Test
    @DisplayName("POSITIVE: Staff user can return another student's rental")
    void testReturnBook_StaffCanReturn_Positive() {
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(9));
        rental.setId(75L);

        when(rentalRepository.findById(75L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RentalResponse response = rentalService.returnBook(75L, "librarian", true);

        assertNotNull(response);
        assertEquals(RentalStatus.RETURNED, response.getStatus());
    }

    @Test
    @DisplayName("POSITIVE: Simulate overdue backdates due date")
    void testSimulateOverdue_Success_Positive() {
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(12));
        rental.setId(77L);

        when(rentalRepository.findById(77L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RentalResponse response = rentalService.simulateOverdue(77L, 5, "sravanthi", false);

        assertNotNull(response);
        assertEquals(RentalStatus.OVERDUE, response.getStatus());
        assertEquals(LocalDate.now().minusDays(5), rental.getDueDate());
    }

    @Test
    @DisplayName("POSITIVE: Get rentals by username")
    void testGetRentalsByUsername_Positive() {
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(12));
        rental.setId(1L);

        when(rentalRepository.findByStudentUsernameOrderByCreatedAtDesc("sravanthi"))
                .thenReturn(List.of(rental));

        List<RentalResponse> rentals = rentalService.getRentalsByUsername("sravanthi");

        assertNotNull(rentals);
        assertEquals(1, rentals.size());
        assertEquals("sravanthi", rentals.get(0).getStudentUsername());
    }

    @Test
    @DisplayName("POSITIVE: Get all rentals for staff")
    void testGetAllRentals_Positive() {
        Rental rental = new Rental("2400030661", "sravanthi", 10L, "Building Microservices",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(12));
        rental.setId(1L);

        when(rentalRepository.findAll()).thenReturn(List.of(rental));

        List<RentalResponse> rentals = rentalService.getAllRentals();

        assertNotNull(rentals);
        assertEquals(1, rentals.size());
    }
}
