package com.bibliotech.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CalculateFineRequest {

    private Long rentalId;
    private String studentId;
    private String studentUsername;
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
