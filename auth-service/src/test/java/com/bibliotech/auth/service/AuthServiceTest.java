package com.bibliotech.auth.service;

import com.bibliotech.auth.dto.AuthResponse;
import com.bibliotech.auth.dto.LoginRequest;
import com.bibliotech.auth.dto.RegisterRequest;
import com.bibliotech.auth.dto.ValidateTokenResponse;
import com.bibliotech.auth.entity.Role;
import com.bibliotech.auth.entity.User;
import com.bibliotech.auth.repository.UserRepository;
import com.bibliotech.auth.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                "2400030661",
                "encodedPassword123",
                "Musuluri Sravanthi",
                "2400030661@kluniversity.in",
                "2400030661",
                Role.STUDENT
        );
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("POSITIVE: Register student user successfully")
    void testRegister_Success_Positive() {
        RegisterRequest request = new RegisterRequest(
                "2400030661",
                "pass123",
                "Musuluri Sravanthi",
                "2400030661@kluniversity.in",
                "2400030661",
                Role.STUDENT
        );

        when(userRepository.existsByUsername("2400030661")).thenReturn(false);
        when(userRepository.existsByEmail("2400030661@kluniversity.in")).thenReturn(false);
        when(userRepository.existsByStudentId("2400030661")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtils.generateToken(any(User.class))).thenReturn("mock-jwt-token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("2400030661", response.getUsername());
        assertEquals("Musuluri Sravanthi", response.getFullName());
        assertEquals(Role.STUDENT, response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("SECURITY: Public registration cannot create privileged users")
    void testRegister_RequestedAdminRole_RegistersAsStudent() {
        RegisterRequest request = new RegisterRequest(
                "newstudent",
                "pass123",
                "New Student",
                "newstudent@kluniversity.in",
                "STD002",
                Role.ADMIN
        );
        when(userRepository.existsByUsername("newstudent")).thenReturn(false);
        when(userRepository.existsByEmail("newstudent@kluniversity.in")).thenReturn(false);
        when(userRepository.existsByStudentId("STD002")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(jwtUtils.generateToken(any(User.class))).thenReturn("student-token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertEquals(Role.STUDENT, response.getRole());
        verify(userRepository).save(argThat(user -> user.getRole() == Role.STUDENT));
    }

    @Test
    @DisplayName("NEGATIVE: Register fails when username is already taken")
    void testRegister_DuplicateUsername_Negative() {
        RegisterRequest request = new RegisterRequest(
                "2400030661",
                "pass123",
                "Musuluri Sravanthi",
                "2400030661@kluniversity.in",
                "2400030661",
                Role.STUDENT
        );

        when(userRepository.existsByUsername("2400030661")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Username is already taken"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("NEGATIVE: Register fails when email is already registered")
    void testRegister_DuplicateEmail_Negative() {
        RegisterRequest request = new RegisterRequest(
                "newuser",
                "pass123",
                "New User",
                "2400030661@kluniversity.in",
                "STD001",
                Role.STUDENT
        );

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("2400030661@kluniversity.in")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Email is already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("NEGATIVE: Register fails when student ID is already registered")
    void testRegister_DuplicateStudentId_Negative() {
        RegisterRequest request = new RegisterRequest(
                "newuser",
                "pass123",
                "New User",
                "newuser@kluniversity.in",
                "2400030661",
                Role.STUDENT
        );

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@kluniversity.in")).thenReturn(false);
        when(userRepository.existsByStudentId("2400030661")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Student ID is already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POSITIVE: Login succeeds with correct credentials")
    void testLogin_Success_Positive() {
        LoginRequest request = new LoginRequest("2400030661", "pass123");

        when(userRepository.findByUsername("2400030661")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("pass123", "encodedPassword123")).thenReturn(true);
        when(jwtUtils.generateToken(sampleUser)).thenReturn("mock-jwt-token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("2400030661", response.getUsername());
        assertEquals(Role.STUDENT, response.getRole());
    }

    @Test
    @DisplayName("NEGATIVE: Login fails when password does not match")
    void testLogin_WrongPassword_Negative() {
        LoginRequest request = new LoginRequest("2400030661", "wrongPass");

        when(userRepository.findByUsername("2400030661")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPass", "encodedPassword123")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    @DisplayName("NEGATIVE: Login fails when username does not exist")
    void testLogin_UserNotFound_Negative() {
        LoginRequest request = new LoginRequest("unknownUser", "pass123");

        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    @DisplayName("POSITIVE: Validate valid token successfully")
    void testValidateToken_Valid_Positive() {
        String token = "Bearer valid-jwt-token";

        when(jwtUtils.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-jwt-token")).thenReturn("2400030661");
        when(jwtUtils.getRoleFromToken("valid-jwt-token")).thenReturn("STUDENT");
        when(jwtUtils.getUserIdFromToken("valid-jwt-token")).thenReturn(1L);
        when(jwtUtils.getStudentIdFromToken("valid-jwt-token")).thenReturn("2400030661");

        ValidateTokenResponse response = authService.validateToken(token);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals("2400030661", response.getUsername());
        assertEquals("STUDENT", response.getRole());
        assertEquals(1L, response.getUserId());
        assertEquals("2400030661", response.getStudentId());
    }

    @Test
    @DisplayName("NEGATIVE: Validate token fails for expired or malformed token")
    void testValidateToken_Invalid_Negative() {
        String token = "invalid-token";

        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        ValidateTokenResponse response = authService.validateToken(token);

        assertNotNull(response);
        assertFalse(response.isValid());
        assertNull(response.getUsername());
    }

    @Test
    @DisplayName("POSITIVE: Refresh token succeeds with valid existing token")
    void testRefreshToken_Success_Positive() {
        String authHeader = "Bearer valid-token";

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("2400030661");
        when(userRepository.findByUsername("2400030661")).thenReturn(Optional.of(sampleUser));
        when(jwtUtils.generateToken(sampleUser)).thenReturn("new-jwt-token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(authHeader);

        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertEquals("2400030661", response.getUsername());
    }

    @Test
    @DisplayName("NEGATIVE: Refresh token fails when authorization header is missing or invalid")
    void testRefreshToken_InvalidHeader_Negative() {
        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(null));
        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("Basic 1234"));
    }

    @Test
    @DisplayName("POSITIVE: Get user profile succeeds")
    void testGetUserProfile_Success_Positive() {
        when(userRepository.findByUsername("2400030661")).thenReturn(Optional.of(sampleUser));

        User profile = authService.getUserProfile("2400030661");

        assertNotNull(profile);
        assertEquals("2400030661", profile.getUsername());
        assertEquals("Musuluri Sravanthi", profile.getFullName());
    }

    @Test
    @DisplayName("NEGATIVE: Get user profile fails for non-existent user")
    void testGetUserProfile_NotFound_Negative() {
        when(userRepository.findByUsername("nonExistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.getUserProfile("nonExistent"));
    }
}
