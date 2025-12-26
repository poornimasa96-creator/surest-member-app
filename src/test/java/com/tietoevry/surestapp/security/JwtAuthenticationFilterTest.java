package com.tietoevry.surestapp.security;

import com.tietoevry.surestapp.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String USERNAME = "testuser";
    private static final String ROLE = "ROLE_USER";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // A. Success Scenarios - Valid JWT Token

    @Test
    @DisplayName("Should set authentication when valid JWT token is provided")
    void should_setAuthentication_when_validJwtTokenProvided() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);
        when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ROLE);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(USERNAME, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority(ROLE)));

        verify(jwtUtil, times(1)).validateToken(VALID_TOKEN);
        verify(jwtUtil, times(1)).getUsernameFromToken(VALID_TOKEN);
        verify(jwtUtil, times(1)).getRoleFromToken(VALID_TOKEN);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract username and role from token correctly")
    void should_extractUsernameAndRole_when_tokenIsValid() throws ServletException, IOException {
        // Arrange
        String adminRole = "ROLE_ADMIN";
        String adminUsername = "admin";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(VALID_TOKEN)).thenReturn(adminUsername);
        when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(adminRole);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(adminUsername, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority(adminRole)));
    }

    // B. No Token Scenarios

    @Test
    @DisplayName("Should not set authentication when no authorization header is present")
    void should_notSetAuthentication_when_noAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when authorization header does not start with Bearer")
    void should_notSetAuthentication_when_authorizationHeaderNotBearer() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when authorization header is empty")
    void should_notSetAuthentication_when_authorizationHeaderIsEmpty() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // C. Invalid Token Scenarios

    @Test
    @DisplayName("Should not set authentication when JWT token is invalid")
    void should_notSetAuthentication_when_jwtTokenIsInvalid() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
        when(jwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(jwtUtil, times(1)).validateToken(INVALID_TOKEN);
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(jwtUtil, never()).getRoleFromToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when token validation throws exception")
    void should_notSetAuthentication_when_tokenValidationThrowsException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
        when(jwtUtil.validateToken(INVALID_TOKEN)).thenThrow(new RuntimeException("Token parsing error"));
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(jwtUtil, times(1)).validateToken(INVALID_TOKEN);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // D. Filter Chain Continuation

    @Test
    @DisplayName("Should always call filter chain doFilter")
    void should_callFilterChainDoFilter_when_filterExecutes() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should call filter chain even when authentication succeeds")
    void should_callFilterChain_when_authenticationSucceeds() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);
        when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ROLE);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should call filter chain even when authentication fails")
    void should_callFilterChain_when_authenticationFails() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
        when(jwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // E. Token Extraction

    @Test
    @DisplayName("Should extract token correctly from Bearer authorization header")
    void should_extractTokenCorrectly_when_bearerHeaderProvided() throws ServletException, IOException {
        // Arrange
        String tokenWithSpaces = "token.with.parts";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenWithSpaces);
        when(jwtUtil.validateToken(tokenWithSpaces)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(tokenWithSpaces)).thenReturn(USERNAME);
        when(jwtUtil.getRoleFromToken(tokenWithSpaces)).thenReturn(ROLE);
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtUtil, times(1)).validateToken(tokenWithSpaces);
    }

    @Test
    @DisplayName("Should handle Bearer token with only Bearer prefix")
    void should_handleBearerToken_when_onlyPrefixProvided() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        verify(jwtUtil, times(1)).validateToken("");
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
