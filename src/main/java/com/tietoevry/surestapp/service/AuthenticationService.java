package com.tietoevry.surestapp.service;

import com.tietoevry.surestapp.domain.User;
import com.tietoevry.surestapp.dto.request.LoginRequest;
import com.tietoevry.surestapp.dto.response.LoginResponse;
import com.tietoevry.surestapp.exception.InvalidCredentialsException;
import com.tietoevry.surestapp.repository.UserRepository;
import com.tietoevry.surestapp.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticate(LoginRequest request) {
        log.info("Authenticating user: {}", request.username());
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> {
                log.error("Authentication failed: User not found with username: {}", request.username());
                return new InvalidCredentialsException("Invalid username or password");
            });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.error("Authentication failed: Invalid password for username: {}", request.username());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().getName());
        log.info("Successfully authenticated user: {} with role: {}", user.getUsername(), user.getRole().getName());

        return new LoginResponse(token, user.getUsername(), user.getRole().getName());
    }
}
