package com.bibliotech.fine.service;

import com.bibliotech.fine.dto.CalculateFineRequest;
import com.bibliotech.fine.dto.FineResponse;
import com.bibliotech.fine.entity.Fine;
import com.bibliotech.fine.entity.FineStatus;
import com.bibliotech.fine.repository.FineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock
    private FineRepository fineRepository;

    private FineService fineService;

    @BeforeEach
    void setUp() {
        fineService = new FineService(fineRepository, BigDecimal.valueOf(10.00));
    }

    @Test
    @DisplayName("POSITIVE: Calculate and record new fine for overdue return")
    void testCalculateAndRecordFine_NewFine_Success() {
        CalculateFineRequest request = new CalculateFineRequest(100L, "2400030661", "sravanthi", 4);

        when(fineRepository.findByRentalId(100L)).thenReturn(Optional.empty());
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> {
            Fine fine = invocation.getArgument(0);
            fine.setId(1L);
            return fine;
        });

        FineResponse response = fineService.calculateAndRecordFine(request);

        assertNotNull(response);
        assertEquals(100L, response.getRentalId());
        assertEquals("2400030661", response.getStudentId());
        assertEquals(4, response.getOverdueDays());
        // 4 days * 10.00 = 40.00
        assertEquals(new BigDecimal("40.0"), response.getAmount());
        assertEquals(FineStatus.UNPAID, response.getStatus());

        verify(fineRepository, times(1)).save(any(Fine.class));
    }

    @Test
    @DisplayName("POSITIVE: Calculate fine for existing unpaid fine updates amount")
    void testCalculateAndRecordFine_ExistingUnpaidFine_Updates() {
        CalculateFineRequest request = new CalculateFineRequest(100L, "2400030661", "sravanthi", 6);

        Fine existing = new Fine(100L, "2400030661", "sravanthi", 4, BigDecimal.valueOf(10.00), BigDecimal.valueOf(40.00));
        existing.setId(1L);
        existing.setStatus(FineStatus.UNPAID);

        when(fineRepository.findByRentalId(100L)).thenReturn(Optional.of(existing));
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FineResponse response = fineService.calculateAndRecordFine(request);

        assertNotNull(response);
        assertEquals(6, response.getOverdueDays());
        assertEquals(new BigDecimal("60.0"), response.getAmount());
        verify(fineRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("NEGATIVE: Calculate fine throws exception when overdue days is zero or negative")
    void testCalculateAndRecordFine_ZeroOrNegativeDays_ThrowsException() {
        CalculateFineRequest request = new CalculateFineRequest(100L, "2400030661", "sravanthi", 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fineService.calculateAndRecordFine(request));
        assertTrue(ex.getMessage().contains("must be greater than zero"));
        verify(fineRepository, never()).save(any(Fine.class));
    }

    @Test
    @DisplayName("POSITIVE: Pay fine succeeds and transitions status to PAID")
    void testPayFine_Success() {
        Fine fine = new Fine(100L, "2400030661", "sravanthi", 3, BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00));
        fine.setId(5L);
        fine.setStatus(FineStatus.UNPAID);

        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FineResponse response = fineService.payFine(5L);

        assertEquals(FineStatus.PAID, response.getStatus());
        assertNotNull(response.getPaidAt());
        verify(fineRepository, times(1)).save(fine);
    }

    @Test
    @DisplayName("NEGATIVE: Pay fine throws exception when fine is already paid")
    void testPayFine_AlreadyPaid_ThrowsException() {
        Fine fine = new Fine(100L, "2400030661", "sravanthi", 3, BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00));
        fine.setId(5L);
        fine.setStatus(FineStatus.PAID);

        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));

        assertThrows(IllegalStateException.class, () -> fineService.payFine(5L));
        verify(fineRepository, never()).save(fine);
    }

    @Test
    @DisplayName("NEGATIVE: Pay fine throws exception when fine ID is not found")
    void testPayFine_NotFound_ThrowsException() {
        when(fineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> fineService.payFine(999L));
    }

    @Test
    @DisplayName("NEGATIVE: Student attempting to pay another student's fine throws exception")
    void testPayFine_UnauthorizedStudent_ThrowsException() {
        Fine fine = new Fine(100L, "2400030661", "sravanthi", 3, BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00));
        fine.setId(5L);

        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));

        assertThrows(IllegalStateException.class,
                () -> fineService.payFine(5L, "otherStudent", false));
    }

    @Test
    @DisplayName("POSITIVE: Get fines by student ID")
    void testGetFinesByStudentId_Positive() {
        Fine fine = new Fine(100L, "2400030661", "sravanthi", 3, BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00));
        when(fineRepository.findByStudentId("2400030661")).thenReturn(List.of(fine));

        List<FineResponse> list = fineService.getFinesByStudentId("2400030661");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("2400030661", list.get(0).getStudentId());
    }

    @Test
    @DisplayName("POSITIVE: Get fines by username")
    void testGetFinesByUsername_Positive() {
        Fine fine = new Fine(100L, "2400030661", "sravanthi", 3, BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00));
        when(fineRepository.findByStudentUsername("sravanthi")).thenReturn(List.of(fine));

        List<FineResponse> list = fineService.getFinesByStudentUsername("sravanthi");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("sravanthi", list.get(0).getStudentUsername());
    }

    @Test
    @DisplayName("POSITIVE: Get all fines for administrator/librarian oversight")
    void testGetAllFines_Positive() {
        Fine fine = new Fine(100L, "2400030661", "sravanthi", 3, BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00));
        when(fineRepository.findAll()).thenReturn(List.of(fine));

        List<FineResponse> list = fineService.getAllFines();

        assertNotNull(list);
        assertEquals(1, list.size());
    }
}
