package com.bibliotech.auth.dto;

public class ValidateTokenResponse {
    private boolean valid;
    private String username;
    private String role;
    private Long userId;
    private String studentId;
    private String message;

    public ValidateTokenResponse() {}

    public ValidateTokenResponse(boolean valid, String username, String role, Long userId, String studentId, String message) {
        this.valid = valid;
        this.username = username;
        this.role = role;
        this.userId = userId;
        this.studentId = studentId;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
