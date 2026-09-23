package com.bibliotech.fine.service;

import com.bibliotech.fine.dto.CalculateFineRequest;
import com.bibliotech.fine.dto.FineResponse;
import com.bibliotech.fine.entity.Fine;
import com.bibliotech.fine.entity.FineStatus;
import com.bibliotech.fine.repository.FineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    void testPayFine_AlreadyPaid_ThrowsException() {
        Fine fine = new Fine(100L, "2400030661", "sravanthi", 3, BigDecimal.valueOf(10.00), BigDecimal.valueOf(30.00));
        fine.setId(5L);
        fine.setStatus(FineStatus.PAID);

        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));

        assertThrows(IllegalStateException.class, () -> fineService.payFine(5L));
        verify(fineRepository, never()).save(fine);
    }
}
