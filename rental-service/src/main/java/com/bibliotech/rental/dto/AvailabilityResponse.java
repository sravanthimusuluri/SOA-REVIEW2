package com.bibliotech.rental.dto;

public class AvailabilityResponse {

    private Long bookId;
    private String title;
    private Integer availableCopies;
    private Integer totalCopies;
    private boolean available;

    public AvailabilityResponse() {}

    public AvailabilityResponse(Long bookId, String title, Integer availableCopies, Integer totalCopies, boolean available) {
        this.bookId = bookId;
        this.title = title;
        this.availableCopies = availableCopies;
        this.totalCopies = totalCopies;
        this.available = available;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }

    public Integer getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(Integer totalCopies) {
        this.totalCopies = totalCopies;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
