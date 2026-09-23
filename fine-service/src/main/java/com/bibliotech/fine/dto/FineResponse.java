package com.bibliotech.fine.dto;

import com.bibliotech.fine.entity.Fine;
import com.bibliotech.fine.entity.FineStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FineResponse {

    private Long id;
    private Long rentalId;
    private String studentId;
    private String studentUsername;
    private Integer overdueDays;
    private BigDecimal dailyRate;
    private BigDecimal amount;
    private FineStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public FineResponse() {}

    public static FineResponse fromEntity(Fine fine) {
        FineResponse response = new FineResponse();
        response.setId(fine.getId());
        response.setRentalId(fine.getRentalId());
        response.setStudentId(fine.getStudentId());
        response.setStudentUsername(fine.getStudentUsername());
        response.setOverdueDays(fine.getOverdueDays());
        response.setDailyRate(fine.getDailyRate());
        response.setAmount(fine.getAmount());
        response.setStatus(fine.getStatus());
        response.setCreatedAt(fine.getCreatedAt());
        response.setPaidAt(fine.getPaidAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentUsername() {
        return studentUsername;
    }

    public void setStudentUsername(String studentUsername) {
        this.studentUsername = studentUsername;
    }

    public Integer getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public FineStatus getStatus() {
        return status;
    }

    public void setStatus(FineStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
