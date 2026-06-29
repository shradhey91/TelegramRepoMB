package com.telegram.user.service;

import com.telegram.auth.entity.User;
import com.telegram.common.exception.ConflictException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.user.dto.response.UserProfileResponse;
import com.telegram.user.entity.BlockedUser;
import com.telegram.user.repository.BlockedUserRepo;
import com.telegram.user.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BlockService {

    private final BlockedUserRepo blockedUserRepo;
    private final UserRepo userRepo;

    public BlockService(BlockedUserRepo blockedUserRepo, UserRepo userRepo) {
        this.blockedUserRepo = blockedUserRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("You cannot block yourself");
        }

        User blocker = userRepo.findById(blockerId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User blocked = userRepo.findById(blockedId).orElseThrow(() -> new ResourceNotFoundException("User to block not found"));

        if (blockedUserRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new ConflictException("User is already blocked");
        }

        BlockedUser block = BlockedUser.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();

        blockedUserRepo.save(block);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        BlockedUser block = blockedUserRepo.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not blocked"));

        blockedUserRepo.delete(block);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getBlockedUsers(Long userId) {

        List<Long> blockedIds = blockedUserRepo.findBlockedUserIds(userId);

        List<User> blockedUsers = userRepo.findAllById(blockedIds);

        List<UserProfileResponse> response = new ArrayList<>();

        for (User user : blockedUsers) {
            UserProfileResponse profile = toProfile(user);
            response.add(profile);
        }

        return response;
    }


    @Transactional(readOnly = true)
    public boolean isBlocked(Long userId1, Long userId2) {
        return blockedUserRepo.isEitherBlocked(userId1, userId2);
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