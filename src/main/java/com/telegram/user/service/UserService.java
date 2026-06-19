package com.telegram.user.service;

import com.telegram.user.dto.response.UserProfileResponse;
import com.telegram.auth.entity.User;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.user.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toProfile(user);
    }

    public List<UserProfileResponse> searchUsers(String query) {
        return userRepo.searchUsers(query).stream()
                .map(this::toProfile)
                .toList();
    }

    private UserProfileResponse toProfile(User user) {
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