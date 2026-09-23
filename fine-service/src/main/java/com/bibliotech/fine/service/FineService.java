package com.bibliotech.fine.service;

import com.bibliotech.fine.dto.CalculateFineRequest;
import com.bibliotech.fine.dto.FineResponse;
import com.bibliotech.fine.entity.Fine;
import com.bibliotech.fine.entity.FineStatus;
import com.bibliotech.fine.repository.FineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FineService {

    private static final Logger logger = LoggerFactory.getLogger(FineService.class);

    private final FineRepository fineRepository;
    private final BigDecimal dailyRate;

    public FineService(
            FineRepository fineRepository,
            @Value("${bibliotech.fine.daily-rate:10.00}") BigDecimal dailyRate) {
        this.fineRepository = fineRepository;
        this.dailyRate = dailyRate;
    }

    @Transactional
    public FineResponse calculateAndRecordFine(CalculateFineRequest request) {
        if (request.getOverdueDays() <= 0) {
            throw new IllegalArgumentException("Overdue days must be greater than zero to incur a penalty");
        }

        BigDecimal calculatedAmount = dailyRate.multiply(BigDecimal.valueOf(request.getOverdueDays()));

        // Check if fine already recorded for this rental
        Optional<Fine> existingFineOpt = fineRepository.findByRentalId(request.getRentalId());
        if (existingFineOpt.isPresent()) {
            Fine existing = existingFineOpt.get();
            if (existing.getStatus() == FineStatus.UNPAID) {
                existing.setOverdueDays(request.getOverdueDays());
                existing.setDailyRate(dailyRate);
                existing.setAmount(calculatedAmount);
                Fine updated = fineRepository.save(existing);
                logger.info("Updated existing fine #{} for rental #{}: Amount = {}", updated.getId(), request.getRentalId(), calculatedAmount);
                return FineResponse.fromEntity(updated);
            }
            return FineResponse.fromEntity(existing);
        }

        Fine fine = new Fine(
                request.getRentalId(),
                request.getStudentId(),
                request.getStudentUsername(),
                request.getOverdueDays(),
                dailyRate,
                calculatedAmount
        );

        Fine saved = fineRepository.save(fine);
        logger.info("Recorded new penalty fine #{} for student {} (rental #{}): Overdue days = {}, Amount = {}",
                saved.getId(), request.getStudentUsername(), request.getRentalId(), request.getOverdueDays(), calculatedAmount);

        return FineResponse.fromEntity(saved);
    }

    public List<FineResponse> getFinesByStudentId(String studentId) {
        return fineRepository.findByStudentId(studentId).stream()
                .map(FineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<FineResponse> getFinesByStudentUsername(String username) {
        return fineRepository.findByStudentUsername(username).stream()
                .map(FineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<FineResponse> getAllFines() {
        return fineRepository.findAll().stream()
                .map(FineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public FineResponse payFine(Long id) {
        return payFine(id, null, true);
    }

    @Transactional
    public FineResponse payFine(Long id, String currentUsername, boolean staff) {
        Fine fine = fineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fine not found with ID: " + id));

        if (!staff && (currentUsername == null || !currentUsername.equals(fine.getStudentUsername()))) {
            throw new IllegalStateException("You can only pay your own fines");
        }

        if (fine.getStatus() == FineStatus.PAID) {
            throw new IllegalStateException("Fine #" + id + " has already been paid on " + fine.getPaidAt());
        }

        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());

        Fine updated = fineRepository.save(fine);
        logger.info("Fine #{} marked as PAID by student {}", updated.getId(), updated.getStudentUsername());
        return FineResponse.fromEntity(updated);
    }
}
