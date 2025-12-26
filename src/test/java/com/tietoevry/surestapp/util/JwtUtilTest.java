package com.tietoevry.surestapp.util;

import com.tietoevry.surestapp.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtUtil jwtUtil;

    private static final String SECRET = "4c40e46f855ed8f3722480ea85fa6f389b0bb5c4c5ea49fc0677657d0adfa33a";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours
    private static final String USERNAME = "testuser";
    private static final String ROLE = "ROLE_USER";

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getExpirationMs()).thenReturn(EXPIRATION_MS);
        jwtUtil = new JwtUtil(jwtProperties);
    }

    // A. Token Generation

    @Test
    @DisplayName("Should generate valid JWT token with username and role")
    void should_generateValidJwtToken_when_usernameAndRoleProvided() {
        // Act
        String token = jwtUtil.generateToken(USERNAME, ROLE);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts separated by dots
    }

    @Test
    @DisplayName("Should generate different tokens for different usernames")
    void should_generateDifferentTokens_when_differentUsernames() {
        // Act
        String token1 = jwtUtil.generateToken("user1", ROLE);
        String token2 = jwtUtil.generateToken("user2", ROLE);

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should generate different tokens for different roles")
    void should_generateDifferentTokens_when_differentRoles() {
        // Act
        String token1 = jwtUtil.generateToken(USERNAME, "ROLE_USER");
        String token2 = jwtUtil.generateToken(USERNAME, "ROLE_ADMIN");

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should generate token with admin role")
    void should_generateToken_when_adminRoleProvided() {
        // Act
        String token = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        // Assert
        assertNotNull(token);
        String role = jwtUtil.getRoleFromToken(token);
        assertEquals("ROLE_ADMIN", role);
    }

    // B. Token Validation

    @Test
    @DisplayName("Should validate token as true when token is valid")
    void should_validateTokenAsTrue_when_tokenIsValid() {
        // Arrange
        String token = jwtUtil.generateToken(USERNAME, ROLE);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should validate token as false when token is invalid")
    void should_validateTokenAsFalse_when_tokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate token as false when token is malformed")
    void should_validateTokenAsFalse_when_tokenIsMalformed() {
        // Arrange
        String malformedToken = "not-a-valid-jwt";

        // Act
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate token as false when token is empty")
    void should_validateTokenAsFalse_when_tokenIsEmpty() {
        // Act
        boolean isValid = jwtUtil.validateToken("");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate token as false when token is null")
    void should_validateTokenAsFalse_when_tokenIsNull() {
        // Act
        boolean isValid = jwtUtil.validateToken(null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate token as false when token has wrong signature")
    void should_validateTokenAsFalse_when_tokenHasWrongSignature() {
        // Arrange
        String token = jwtUtil.generateToken(USERNAME, ROLE);
        String tamperedToken = token.substring(0, token.length() - 10) + "tampered12";

        // Act
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // Assert
        assertFalse(isValid);
    }

    // C. Extract Username from Token

    @Test
    @DisplayName("Should extract username from valid token")
    void should_extractUsername_when_tokenIsValid() {
        // Arrange
        String token = jwtUtil.generateToken(USERNAME, ROLE);

        // Act
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertEquals(USERNAME, extractedUsername);
    }

    @Test
    @DisplayName("Should extract correct username for admin user")
    void should_extractCorrectUsername_when_adminUserToken() {
        // Arrange
        String adminUsername = "admin";
        String token = jwtUtil.generateToken(adminUsername, "ROLE_ADMIN");

        // Act
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertEquals(adminUsername, extractedUsername);
    }

    @Test
    @DisplayName("Should throw exception when extracting username from invalid token")
    void should_throwException_when_extractingUsernameFromInvalidToken() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act & Assert
        assertThrows(JwtException.class, () -> jwtUtil.getUsernameFromToken(invalidToken));
    }

    @Test
    @DisplayName("Should preserve username case when extracting")
    void should_preserveUsernameCase_when_extracting() {
        // Arrange
        String mixedCaseUsername = "TestUser123";
        String token = jwtUtil.generateToken(mixedCaseUsername, ROLE);

        // Act
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertEquals(mixedCaseUsername, extractedUsername);
    }

    // D. Extract Role from Token

    @Test
    @DisplayName("Should extract role from valid token")
    void should_extractRole_when_tokenIsValid() {
        // Arrange
        String token = jwtUtil.generateToken(USERNAME, ROLE);

        // Act
        String extractedRole = jwtUtil.getRoleFromToken(token);

        // Assert
        assertEquals(ROLE, extractedRole);
    }

    @Test
    @DisplayName("Should extract correct role for admin user")
    void should_extractCorrectRole_when_adminUserToken() {
        // Arrange
        String adminRole = "ROLE_ADMIN";
        String token = jwtUtil.generateToken(USERNAME, adminRole);

        // Act
        String extractedRole = jwtUtil.getRoleFromToken(token);

        // Assert
        assertEquals(adminRole, extractedRole);
    }

    @Test
    @DisplayName("Should throw exception when extracting role from invalid token")
    void should_throwException_when_extractingRoleFromInvalidToken() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act & Assert
        assertThrows(JwtException.class, () -> jwtUtil.getRoleFromToken(invalidToken));
    }

    @Test
    @DisplayName("Should extract role with custom format")
    void should_extractRoleWithCustomFormat_when_provided() {
        // Arrange
        String customRole = "CUSTOM_ROLE";
        String token = jwtUtil.generateToken(USERNAME, customRole);

        // Act
        String extractedRole = jwtUtil.getRoleFromToken(token);

        // Assert
        assertEquals(customRole, extractedRole);
    }

    // E. Token Round-Trip Tests

    @Test
    @DisplayName("Should maintain username and role integrity through generation and extraction")
    void should_maintainIntegrity_when_generatingAndExtractingToken() {
        // Arrange
        String testUsername = "integrationUser";
        String testRole = "ROLE_INTEGRATION";

        // Act
        String token = jwtUtil.generateToken(testUsername, testRole);
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        String extractedRole = jwtUtil.getRoleFromToken(token);
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertEquals(testUsername, extractedUsername);
        assertEquals(testRole, extractedRole);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void should_handleSpecialCharacters_when_usernameContainsSpecialChars() {
        // Arrange
        String specialUsername = "user@example.com";
        String token = jwtUtil.generateToken(specialUsername, ROLE);

        // Act
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertEquals(specialUsername, extractedUsername);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle long username")
    void should_handleLongUsername_when_generating() {
        // Arrange
        String longUsername = "a".repeat(100);
        String token = jwtUtil.generateToken(longUsername, ROLE);

        // Act
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertEquals(longUsername, extractedUsername);
        assertTrue(isValid);
    }

    // F. Configuration Tests

    @Test
    @DisplayName("Should use configured secret from properties")
    void should_useConfiguredSecret_when_generating() {
        // Assert
        verify(jwtProperties, atLeastOnce()).getSecret();
    }

    @Test
    @DisplayName("Should use configured expiration from properties")
    void should_useConfiguredExpiration_when_generating() {
        // Act
        jwtUtil.generateToken(USERNAME, ROLE);

        // Assert
        verify(jwtProperties, atLeastOnce()).getExpirationMs();
    }
}
