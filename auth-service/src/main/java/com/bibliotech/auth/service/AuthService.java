package com.bibliotech.auth.service;

import com.bibliotech.auth.dto.AuthResponse;
import com.bibliotech.auth.dto.LoginRequest;
import com.bibliotech.auth.dto.RegisterRequest;
import com.bibliotech.auth.dto.ValidateTokenResponse;
import com.bibliotech.auth.entity.Role;
import com.bibliotech.auth.entity.User;
import com.bibliotech.auth.repository.UserRepository;
import com.bibliotech.auth.util.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }
        if (request.getStudentId() != null && !request.getStudentId().isBlank() &&
                userRepository.existsByStudentId(request.getStudentId())) {
            throw new IllegalArgumentException("Student ID is already registered: " + request.getStudentId());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.STUDENT;

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getEmail(),
                request.getStudentId(),
                role
        );

        User saved = userRepository.save(user);
        String token = jwtUtils.generateToken(saved);

        return new AuthResponse(
                token,
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getStudentId(),
                saved.getRole(),
                jwtUtils.getExpirationMs()
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtUtils.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getStudentId(),
                user.getRole(),
                jwtUtils.getExpirationMs()
        );
    }

    public ValidateTokenResponse validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtUtils.validateToken(token)) {
            return new ValidateTokenResponse(false, null, null, null, null, "Token is invalid or expired");
        }

        String username = jwtUtils.getUsernameFromToken(token);
        String role = jwtUtils.getRoleFromToken(token);
        Long userId = jwtUtils.getUserIdFromToken(token);
        String studentId = jwtUtils.getStudentIdFromToken(token);

        return new ValidateTokenResponse(true, username, role, userId, studentId, "Token is valid");
    }

    public AuthResponse refreshToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Bearer token is required");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new IllegalArgumentException("Token is invalid or expired");
        }
        String username = jwtUtils.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return new AuthResponse(jwtUtils.generateToken(user), user.getId(), user.getUsername(),
                user.getEmail(), user.getFullName(), user.getStudentId(), user.getRole(),
                jwtUtils.getExpirationMs());
    }

    public User getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
