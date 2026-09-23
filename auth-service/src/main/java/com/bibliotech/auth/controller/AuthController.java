package com.bibliotech.auth.controller;

import com.bibliotech.auth.dto.AuthResponse;
import com.bibliotech.auth.dto.LoginRequest;
import com.bibliotech.auth.dto.RegisterRequest;
import com.bibliotech.auth.dto.ValidateTokenResponse;
import com.bibliotech.auth.entity.User;
import com.bibliotech.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        
        String token = tokenParam;
        if (token == null && authHeader != null) {
            token = authHeader;
        }

        ValidateTokenResponse response = authService.validateToken(token);
        if (response.isValid()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.refreshToken(authHeader));
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@RequestParam("username") String username) {
        return ResponseEntity.ok(authService.getUserProfile(username));
    }
}
