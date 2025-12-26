package com.tietoevry.surestapp.controller;

import com.tietoevry.surestapp.dto.request.LoginRequest;
import com.tietoevry.surestapp.dto.response.LoginResponse;
import com.tietoevry.surestapp.exception.InvalidCredentialsException;
import com.tietoevry.surestapp.service.AuthenticationService;
import com.tietoevry.surestapp.util.TestDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationController Unit Tests")
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private Validator validator;

    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "password123";
    private static final String INVALID_USERNAME = "invaliduser";
    private static final String INVALID_PASSWORD = "wrongpassword";

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // A. Success Scenarios

    @Test
    @DisplayName("Should return login response when credentials are valid")
    void should_returnLoginResponse_when_credentialsAreValid() {
        // Arrange
        LoginRequest request = TestDataBuilder.validLoginRequest();
        LoginResponse expectedResponse = TestDataBuilder.adminLoginResponse();
        when(authenticationService.authenticate(request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<LoginResponse> response = authenticationController.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse.token(), response.getBody().token());
        assertEquals(expectedResponse.username(), response.getBody().username());
        assertEquals(expectedResponse.role(), response.getBody().role());
        assertEquals("Bearer", response.getBody().type());
        verify(authenticationService, times(1)).authenticate(request);
    }

    @Test
    @DisplayName("Should return login response with USER role when regular user logs in")
    void should_returnLoginResponseWithUserRole_when_regularUserLogsIn() {
        // Arrange
        LoginRequest request = TestDataBuilder.loginRequestWithUsername("user");
        LoginResponse expectedResponse = TestDataBuilder.userLoginResponse();
        when(authenticationService.authenticate(request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<LoginResponse> response = authenticationController.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USER", response.getBody().role());
        assertEquals("user", response.getBody().username());
        verify(authenticationService, times(1)).authenticate(request);
    }

    // B. Exception Scenarios

    @Test
    @DisplayName("Should throw InvalidCredentialsException when username is invalid")
    void should_throwInvalidCredentialsException_when_usernameIsInvalid() {
        // Arrange
        LoginRequest request = TestDataBuilder.loginRequestWithUsername(INVALID_USERNAME);
        when(authenticationService.authenticate(request))
            .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authenticationController.login(request)
        );
        assertEquals("Invalid username or password", exception.getMessage());
        verify(authenticationService, times(1)).authenticate(request);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is invalid")
    void should_throwInvalidCredentialsException_when_passwordIsInvalid() {
        // Arrange
        LoginRequest request = TestDataBuilder.loginRequestWithPassword(INVALID_PASSWORD);
        when(authenticationService.authenticate(request))
            .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> authenticationController.login(request)
        );
        assertEquals("Invalid username or password", exception.getMessage());
        verify(authenticationService, times(1)).authenticate(request);
    }

    // C. Validation Scenarios

    @Test
    @DisplayName("Should fail validation when username is blank")
    void should_failValidation_when_usernameIsBlank() {
        // Arrange
        LoginRequest request = new LoginRequest("", VALID_PASSWORD);

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Username is required")));
    }

    @Test
    @DisplayName("Should fail validation when password is blank")
    void should_failValidation_when_passwordIsBlank() {
        // Arrange
        LoginRequest request = new LoginRequest(VALID_USERNAME, "");

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Password is required")));
    }

    @Test
    @DisplayName("Should fail validation when username is null")
    void should_failValidation_when_usernameIsNull() {
        // Arrange
        LoginRequest request = new LoginRequest(null, VALID_PASSWORD);

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Username is required")));
    }

    @Test
    @DisplayName("Should fail validation when password is null")
    void should_failValidation_when_passwordIsNull() {
        // Arrange
        LoginRequest request = new LoginRequest(VALID_USERNAME, null);

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Password is required")));
    }
}
