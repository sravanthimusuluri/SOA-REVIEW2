package com.bibliotech.rental.service;

import com.bibliotech.rental.client.BookClient;
import com.bibliotech.rental.client.FineClient;
import com.bibliotech.rental.dto.*;
import com.bibliotech.rental.entity.Rental;
import com.bibliotech.rental.entity.RentalStatus;
import com.bibliotech.rental.exception.DuplicateBorrowException;
import com.bibliotech.rental.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
