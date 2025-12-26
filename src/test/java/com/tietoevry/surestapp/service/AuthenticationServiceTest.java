package com.tietoevry.surestapp.service;

import com.tietoevry.surestapp.domain.Role;
import com.tietoevry.surestapp.domain.User;
import com.tietoevry.surestapp.dto.request.LoginRequest;
import com.tietoevry.surestapp.dto.response.LoginResponse;
import com.tietoevry.surestapp.exception.InvalidCredentialsException;
import com.tietoevry.surestapp.repository.UserRepository;
import com.tietoevry.surestapp.util.JwtUtil;
import com.tietoevry.surestapp.util.TestDataBuilder;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User adminUser;
    private User regularUser;
    private Role adminRole;
    private Role userRole;

    private static final String ADMIN_USERNAME = "admin";
    private static final String USER_USERNAME = "user";
    private static final String PASSWORD = "password123";
    private static final String PASSWORD_HASH = "$2a$10$hashedPassword";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.token";

    @BeforeEach
    void setUp() {
        adminRole = new Role("ADMIN");
        userRole = new Role("USER");

        adminUser = new User(ADMIN_USERNAME, PASSWORD_HASH, adminRole);
        regularUser = new User(USER_USERNAME, PASSWORD_HASH, userRole);
    }

    // A. Success Scenarios

    @Test
    @DisplayName("Should return login response when admin credentials are valid")
    void should_returnLoginResponse_when_adminCredentialsAreValid() {
        // Arrange
        LoginRequest request = TestDataBuilder.validLoginRequest();
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtUtil.generateToken(ADMIN_USERNAME, "ADMIN")).thenReturn(JWT_TOKEN);

        // Act
        LoginResponse response = authenticationService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals(JWT_TOKEN, response.token());
        assertEquals(ADMIN_USERNAME, response.username());
        assertEquals("ADMIN", response.role());
        assertEquals("Bearer", response.type());

        verify(userRepository, times(1)).findByUsername(ADMIN_USERNAME);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(jwtUtil, times(1)).generateToken(ADMIN_USERNAME, "ADMIN");
    }

    @Test
    @DisplayName("Should return login response when regular user credentials are valid")
    void should_returnLoginResponse_when_regularUserCredentialsAreValid() {
        // Arrange
        LoginRequest request = TestDataBuilder.loginRequestWithUsername(USER_USERNAME);
        when(userRepository.findByUsername(USER_USERNAME)).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtUtil.generateToken(USER_USERNAME, "USER")).thenReturn(JWT_TOKEN);

        // Act
        LoginResponse response = authenticationService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals(JWT_TOKEN, response.token());
        assertEquals(USER_USERNAME, response.username());
        assertEquals("USER", response.role());
        assertEquals("Bearer", response.type());

        verify(userRepository, times(1)).findByUsername(USER_USERNAME);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(jwtUtil, times(1)).generateToken(USER_USERNAME, "USER");
    }

    // B. Exception Scenarios - Invalid Username

    @Test
    @DisplayName("Should throw InvalidCredentialsException when username does not exist")
    void should_throwInvalidCredentialsException_when_usernameDoesNotExist() {
        // Arrange
        LoginRequest request = TestDataBuilder.loginRequestWithUsername("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authenticationService.authenticate(request)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when username is null in repository")
    void should_throwInvalidCredentialsException_when_usernameIsNullInRepository() {
        // Arrange
        LoginRequest request = TestDataBuilder.validLoginRequest();
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authenticationService.authenticate(request)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(ADMIN_USERNAME);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // C. Exception Scenarios - Invalid Password

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
    void should_throwInvalidCredentialsException_when_passwordIsIncorrect() {
        // Arrange
        LoginRequest request = TestDataBuilder.loginRequestWithPassword("wrongpassword");
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrongpassword", PASSWORD_HASH)).thenReturn(false);

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authenticationService.authenticate(request)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(ADMIN_USERNAME);
        verify(passwordEncoder, times(1)).matches("wrongpassword", PASSWORD_HASH);
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match hash")
    void should_throwInvalidCredentialsException_when_passwordDoesNotMatchHash() {
        // Arrange
        LoginRequest request = TestDataBuilder.validLoginRequest();
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authenticationService.authenticate(request)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(ADMIN_USERNAME);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    // D. JWT Token Generation Scenarios

    @Test
    @DisplayName("Should generate JWT token with correct username and role")
    void should_generateJwtToken_when_authenticationSucceeds() {
        // Arrange
        LoginRequest request = TestDataBuilder.validLoginRequest();
        String customToken = "custom.jwt.token";
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtUtil.generateToken(ADMIN_USERNAME, "ADMIN")).thenReturn(customToken);

        // Act
        LoginResponse response = authenticationService.authenticate(request);

        // Assert
        assertEquals(customToken, response.token());
        verify(jwtUtil, times(1)).generateToken(ADMIN_USERNAME, "ADMIN");
    }

    @Test
    @DisplayName("Should call repository exactly once during successful authentication")
    void should_callRepositoryOnce_when_authenticating() {
        // Arrange
        LoginRequest request = TestDataBuilder.validLoginRequest();
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtUtil.generateToken(ADMIN_USERNAME, "ADMIN")).thenReturn(JWT_TOKEN);

        // Act
        authenticationService.authenticate(request);

        // Assert
        verify(userRepository, times(1)).findByUsername(ADMIN_USERNAME);
        verifyNoMoreInteractions(userRepository);
    }
}
