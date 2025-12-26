package com.tietoevry.surestapp.security;

import com.tietoevry.surestapp.domain.Role;
import com.tietoevry.surestapp.domain.User;
import com.tietoevry.surestapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Unit Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User adminUser;
    private User regularUser;
    private Role adminRole;
    private Role userRole;

    private static final String ADMIN_USERNAME = "admin";
    private static final String USER_USERNAME = "user";
    private static final String PASSWORD_HASH = "$2a$10$hashedPassword";
    private static final String NONEXISTENT_USERNAME = "nonexistent";

    @BeforeEach
    void setUp() {
        adminRole = new Role("ROLE_ADMIN");
        userRole = new Role("ROLE_USER");

        adminUser = new User(ADMIN_USERNAME, PASSWORD_HASH, adminRole);
        regularUser = new User(USER_USERNAME, PASSWORD_HASH, userRole);
    }

    // A. Success Scenarios - User Found

    @Test
    @DisplayName("Should return UserDetails when user exists with ADMIN role")
    void should_returnUserDetails_when_userExistsWithAdminRole() {
        // Arrange
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert
        assertNotNull(userDetails);
        assertEquals(ADMIN_USERNAME, userDetails.getUsername());
        assertEquals(PASSWORD_HASH, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertEquals(1, userDetails.getAuthorities().size());

        verify(userRepository, times(1)).findByUsername(ADMIN_USERNAME);
    }

    @Test
    @DisplayName("Should return UserDetails when user exists with USER role")
    void should_returnUserDetails_when_userExistsWithUserRole() {
        // Arrange
        when(userRepository.findByUsername(USER_USERNAME)).thenReturn(Optional.of(regularUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(USER_USERNAME);

        // Assert
        assertNotNull(userDetails);
        assertEquals(USER_USERNAME, userDetails.getUsername());
        assertEquals(PASSWORD_HASH, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

        verify(userRepository, times(1)).findByUsername(USER_USERNAME);
    }

    @Test
    @DisplayName("Should return UserDetails with correct password hash")
    void should_returnUserDetailsWithCorrectPasswordHash_when_userExists() {
        // Arrange
        String customPasswordHash = "$2a$10$customHashedPassword";
        User customUser = new User(ADMIN_USERNAME, customPasswordHash, adminRole);
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(customUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert
        assertEquals(customPasswordHash, userDetails.getPassword());
    }

    @Test
    @DisplayName("Should return UserDetails with single authority from role")
    void should_returnUserDetailsWithSingleAuthority_when_userHasRole() {
        // Arrange
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Should return enabled user account")
    void should_returnEnabledUserAccount_when_userExists() {
        // Arrange
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    // B. Exception Scenarios - User Not Found

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void should_throwUsernameNotFoundException_when_userDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername(NONEXISTENT_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(NONEXISTENT_USERNAME)
        );

        assertEquals("User not found: " + NONEXISTENT_USERNAME, exception.getMessage());
        verify(userRepository, times(1)).findByUsername(NONEXISTENT_USERNAME);
    }

    @Test
    @DisplayName("Should include username in exception message when user not found")
    void should_includeUsernameInExceptionMessage_when_userNotFound() {
        // Arrange
        String customUsername = "customuser";
        when(userRepository.findByUsername(customUsername)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(customUsername)
        );

        assertTrue(exception.getMessage().contains(customUsername));
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when repository returns empty optional")
    void should_throwUsernameNotFoundException_when_repositoryReturnsEmptyOptional() {
        // Arrange
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(ADMIN_USERNAME)
        );
    }

    // C. Repository Interaction

    @Test
    @DisplayName("Should call repository exactly once when loading user")
    void should_callRepositoryOnce_when_loadingUser() {
        // Arrange
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

        // Act
        userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert
        verify(userRepository, times(1)).findByUsername(ADMIN_USERNAME);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Should query repository with exact username provided")
    void should_queryRepositoryWithExactUsername_when_loadingUser() {
        // Arrange
        String exactUsername = "ExactUsername";
        User customUser = new User(exactUsername, PASSWORD_HASH, userRole);
        when(userRepository.findByUsername(exactUsername)).thenReturn(Optional.of(customUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(exactUsername);

        // Assert
        assertEquals(exactUsername, userDetails.getUsername());
        verify(userRepository, times(1)).findByUsername(exactUsername);
    }

    // D. Role Mapping

    @Test
    @DisplayName("Should correctly map role name to authority")
    void should_correctlyMapRoleNameToAuthority_when_loadingUser() {
        // Arrange
        Role customRole = new Role("ROLE_CUSTOM");
        User userWithCustomRole = new User(USER_USERNAME, PASSWORD_HASH, customRole);
        when(userRepository.findByUsername(USER_USERNAME)).thenReturn(Optional.of(userWithCustomRole));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(USER_USERNAME);

        // Assert
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOM")));
    }

    @Test
    @DisplayName("Should handle role without ROLE_ prefix")
    void should_handleRoleWithoutPrefix_when_loadingUser() {
        // Arrange
        Role roleWithoutPrefix = new Role("ADMIN");
        User userWithRole = new User(ADMIN_USERNAME, PASSWORD_HASH, roleWithoutPrefix);
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(userWithRole));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ADMIN")));
    }
}
