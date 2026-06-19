package com.telegram.user.controller;

import com.telegram.user.dto.response.UserProfileResponse;
import com.telegram.auth.security.CustomUserDetails;
import com.telegram.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Search users and view profiles")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(userService.getUserProfile(userId));
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
}