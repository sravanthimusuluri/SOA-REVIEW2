package com.bibliotech.rental.dto;

import com.bibliotech.rental.entity.Rental;
import com.bibliotech.rental.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RentalResponse {

    private Long id;
    private String studentId;
    private String studentUsername;
    private Long bookId;
    private String bookTitle;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private RentalStatus status;
    private Long fineId;
    private BigDecimal fineAmount;
    private String fineStatus;
    private String note;
    private LocalDateTime createdAt;
    private boolean overdue;

    public RentalResponse() {}

    public static RentalResponse fromEntity(Rental rental) {
        RentalResponse response = new RentalResponse();
        response.setId(rental.getId());
        response.setStudentId(rental.getStudentId());
        response.setStudentUsername(rental.getStudentUsername());
        response.setBookId(rental.getBookId());
        response.setBookTitle(rental.getBookTitle());
        response.setIssueDate(rental.getIssueDate());
        response.setDueDate(rental.getDueDate());
        response.setReturnDate(rental.getReturnDate());
        response.setStatus(rental.getStatus());
        response.setFineId(rental.getFineId());
        response.setNote(rental.getNote());
        response.setCreatedAt(rental.getCreatedAt());

        boolean isPastDue = rental.getStatus() == RentalStatus.ISSUED && LocalDate.now().isAfter(rental.getDueDate());
        response.setOverdue(isPastDue || rental.getStatus() == RentalStatus.OVERDUE);

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setStatus(RentalStatus status) {
        this.status = status;
    }

    public Long getFineId() {
        return fineId;
    }

    public void setFineId(Long fineId) {
        this.fineId = fineId;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getFineStatus() {
        return fineStatus;
    }

    public void setFineStatus(String fineStatus) {
        this.fineStatus = fineStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }
}
