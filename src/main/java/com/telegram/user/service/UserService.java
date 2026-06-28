package com.telegram.user.service;

import com.telegram.user.dto.request.UpdateProfileRequest;
import com.telegram.user.dto.response.UserProfileResponse;
import com.telegram.auth.entity.User;

import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.chat.repository.ChatMemberRepo;

import com.telegram.notification.repository.NotificationRepository;
import com.telegram.storage.dto.StorageFolder;
import com.telegram.storage.dto.StorageServiceProvider;
import com.telegram.storage.dto.UploadResult;
import com.telegram.user.repository.BlockedUserRepo;
import com.telegram.user.repository.UserRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final BlockedUserRepo blockedUserRepo;
    private final ChatMemberRepo chatMemberRepo;
    private final NotificationRepository notificationRepository;
    private final StorageServiceProvider storageServiceProvider;

    private static final int SEARCH_MAX_RESULTS = 20;

    public UserService(UserRepo userRepo,
                       BlockedUserRepo blockedUserRepo,
                       ChatMemberRepo chatMemberRepo,
                       NotificationRepository notificationRepository,
                       StorageServiceProvider storageServiceProvider) {
        this.userRepo = userRepo;
        this.blockedUserRepo = blockedUserRepo;
        this.chatMemberRepo = chatMemberRepo;
        this.notificationRepository = notificationRepository;
        this.storageServiceProvider = storageServiceProvider;
    }

    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toProfile(user);
    }

    public List<UserProfileResponse> searchUsers(String query) {

        List<User> users = userRepo.searchUsers(query, PageRequest.of(0, SEARCH_MAX_RESULTS));

        List<UserProfileResponse> responses = new ArrayList<>();

        for (User user : users) {
            responses.add(toProfile(user));
        }

        return responses;
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.displayName() != null) {
            String trimmed = request.displayName().trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Display name cannot be empty");
            }
            user.setDisplayName(trimmed);
        }

        if (request.bio() != null) {
            if (request.bio().length() > 200) {
                throw new IllegalArgumentException("Bio cannot exceed 200 characters");
            }
            user.setBio(request.bio().trim());
        }

        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }

        userRepo.save(user);
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {

        User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image file");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Avatar size cannot exceed 5MB");
        }

        if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("backblazeb2.com")) {
            try {
                storageServiceProvider.delete(user.getAvatarUrl(), null);
            } catch (Exception ignored) {

            }
        }

        UploadResult result = storageServiceProvider.upload(file, StorageFolder.USERS, userId);
        user.setAvatarUrl(result.url());
        userRepo.save(user);

        return toProfile(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        chatMemberRepo.deleteAllByUserId(userId);
        notificationRepository.deleteAllByRecipientId(userId);
        blockedUserRepo.deleteAllByBlockerIdOrBlockedId(userId, userId);
        userRepo.delete(user);

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