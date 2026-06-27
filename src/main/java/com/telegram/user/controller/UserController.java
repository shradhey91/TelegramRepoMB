package com.telegram.user.controller;

import com.telegram.user.dto.request.UpdateProfileRequest;
import com.telegram.user.dto.response.UserProfileResponse;
import com.telegram.auth.security.CustomUserDetails;
import com.telegram.user.service.BlockService;
import com.telegram.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Profile management, search, block/unblock, and account deletion")
public class UserController {

    private final UserService userService;
    private final BlockService blockService;

    public UserController(UserService userService, BlockService blockService) {
        this.userService = userService;
        this.blockService = blockService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getUserProfile(user.getId()));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user's profile by ID")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username or display name")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current user's profile (display name, bio, avatar URL)")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.updateProfile(user.getId(), request));
    }

    // ─── NEW: Upload Avatar ─────────────────────────────────────────────

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new avatar image (max 5MB, image files only)")
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.uploadAvatar(user.getId(), file));
    }

    // ─── NEW: Delete Account ────────────────────────────────────────────

    @DeleteMapping("/me")
    @Operation(summary = "Delete (anonymize) the current user's account — this is irreversible")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails user) {
        userService.deleteAccount(user.getId());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    // ─── NEW: Block / Unblock ───────────────────────────────────────────

    @PostMapping("/{userId}/block")
    @Operation(summary = "Block a user — prevents messages and calls between you")
    public ResponseEntity<Map<String, String>> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user) {
        blockService.blockUser(user.getId(), userId);
        return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
    }

    @DeleteMapping("/{userId}/block")
    @Operation(summary = "Unblock a previously blocked user")
    public ResponseEntity<Map<String, String>> unblockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user) {
        blockService.unblockUser(user.getId(), userId);
        return ResponseEntity.ok(Map.of("message", "User unblocked successfully"));
    }

    @GetMapping("/blocked")
    @Operation(summary = "Get the list of users you have blocked")
    public ResponseEntity<List<UserProfileResponse>> getBlockedUsers(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(blockService.getBlockedUsers(user.getId()));
    }
}