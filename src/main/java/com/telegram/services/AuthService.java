package com.telegram.services;

import com.telegram.dto.request.RegisterRequest;
import com.telegram.dto.response.AuthResponse;
import com.telegram.dto.response.UserProfileResponse;
import com.telegram.entities.User;
import com.telegram.exception.AccessDeniedException;
import com.telegram.exception.ConflictException;
import com.telegram.exception.ResourceNotFoundException;
import com.telegram.repository.UserRepo;
import com.telegram.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepo userRepo,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepo.existsByEmail(request.email().toLowerCase().trim())) {
            throw new ConflictException("Email is already registered");
        }

        if (userRepo.existsByUsername(request.username().trim())) {
            throw new ConflictException("Username is already taken");
        }

        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName() != null
                        ? request.displayName().trim()
                        : request.username().trim())
                .isOnline(false)
                .build();

        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, toProfileResponse(user));
    }

    public AuthResponse login(String email, String password) {
        User user = userRepo.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email.toLowerCase().trim(), password));

        user.setIsOnline(true);
        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, toProfileResponse(user));
    }

    public void logout(Long userId) {
        userRepo.findById(userId).ifPresent(user -> {
            user.setIsOnline(false);
            user.setLastSeenAt(LocalDateTime.now());
            userRepo.save(user);
        });
    }

    public String changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AccessDeniedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        return "Password changed successfully.";
    }

    public UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getIsOnline(),
                user.getLastSeenAt(),
                user.getCreatedAt());
    }
}
