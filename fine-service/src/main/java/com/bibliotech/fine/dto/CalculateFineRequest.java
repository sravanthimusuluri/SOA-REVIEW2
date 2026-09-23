package com.bibliotech.fine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CalculateFineRequest {

    @NotNull(message = "Rental ID is required")
    private Long rentalId;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Student username is required")
    private String studentUsername;

    @NotNull(message = "Overdue days is required")
    @Min(value = 1, message = "Overdue days must be at least 1")
    private Integer overdueDays;

    public CalculateFineRequest() {}

    public CalculateFineRequest(Long rentalId, String studentId, String studentUsername, Integer overdueDays) {
        this.rentalId = rentalId;
        this.studentId = studentId;
        this.studentUsername = studentUsername;
        this.overdueDays = overdueDays;
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
}
