package com.bibliotech.rental.dto;

import jakarta.validation.constraints.NotNull;

public class BorrowRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    private String studentId;

    private String studentUsername;

    // Optional duration override for simulation / testing
    private Integer loanDays;

    public BorrowRequest() {}

    public BorrowRequest(Long bookId, String studentId, String studentUsername) {
        this.bookId = bookId;
        this.studentId = studentId;
        this.studentUsername = studentUsername;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
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

    public Integer getLoanDays() {
        return loanDays;
    }

    public void setLoanDays(Integer loanDays) {
        this.loanDays = loanDays;
    }
}
