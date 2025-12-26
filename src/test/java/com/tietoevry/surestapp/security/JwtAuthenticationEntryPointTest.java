package com.tietoevry.surestapp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint Unit Tests")
class JwtAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private AuthenticationException authException;

    @BeforeEach
    void setUp() {
        authException = new BadCredentialsException("Bad credentials");
    }

    // A. Success Scenarios

    @Test
    @DisplayName("Should send 401 error when authentication fails")
    void should_send401Error_when_authenticationFails() throws IOException {
        // Arrange
        doNothing().when(response).sendError(anyInt(), anyString());

        // Act
        jwtAuthenticationEntryPoint.commence(request, response, authException);

        // Assert
        verify(response, times(1)).sendError(
            HttpServletResponse.SC_UNAUTHORIZED,
            "Unauthorized: Bad credentials"
        );
    }

    @Test
    @DisplayName("Should include exception message in error response")
    void should_includeExceptionMessage_when_sendingError() throws IOException {
        // Arrange
        AuthenticationException customException = new BadCredentialsException("Invalid JWT token");
        doNothing().when(response).sendError(anyInt(), anyString());

        // Act
        jwtAuthenticationEntryPoint.commence(request, response, customException);

        // Assert
        verify(response, times(1)).sendError(
            HttpServletResponse.SC_UNAUTHORIZED,
            "Unauthorized: Invalid JWT token"
        );
    }

    @Test
    @DisplayName("Should send 401 status code for unauthorized access")
    void should_send401StatusCode_when_unauthorizedAccessOccurs() throws IOException {
        // Arrange
        doNothing().when(response).sendError(anyInt(), anyString());

        // Act
        jwtAuthenticationEntryPoint.commence(request, response, authException);

        // Assert
        verify(response).sendError(eq(401), anyString());
    }

    @Test
    @DisplayName("Should handle null message in authentication exception")
    void should_handleNullMessage_when_authenticationExceptionHasNoMessage() throws IOException {
        // Arrange
        AuthenticationException nullMessageException = new BadCredentialsException(null);
        doNothing().when(response).sendError(anyInt(), anyString());

        // Act
        jwtAuthenticationEntryPoint.commence(request, response, nullMessageException);

        // Assert
        verify(response, times(1)).sendError(
            eq(HttpServletResponse.SC_UNAUTHORIZED),
            eq("Unauthorized: null")
        );
    }

    @Test
    @DisplayName("Should invoke sendError exactly once")
    void should_invokeSendErrorOnce_when_commenceCalled() throws IOException {
        // Arrange
        doNothing().when(response).sendError(anyInt(), anyString());

        // Act
        jwtAuthenticationEntryPoint.commence(request, response, authException);

        // Assert
        verify(response, times(1)).sendError(anyInt(), anyString());
        verifyNoMoreInteractions(response);
    }
}
