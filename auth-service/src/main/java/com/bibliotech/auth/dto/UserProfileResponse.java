package com.bibliotech.auth.dto;

import com.bibliotech.auth.entity.Role;
import com.bibliotech.auth.entity.User;

import java.time.LocalDateTime;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String studentId;
    private Role role;
    private LocalDateTime createdAt;

    public static UserProfileResponse fromEntity(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.fullName = user.getFullName();
        response.studentId = user.getStudentId();
        response.role = user.getRole();
        response.createdAt = user.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
