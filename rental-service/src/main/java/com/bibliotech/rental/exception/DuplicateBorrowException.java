package com.bibliotech.rental.exception;

public class DuplicateBorrowException extends RuntimeException {
    public DuplicateBorrowException(String message) {
        super(message);
    }
}
