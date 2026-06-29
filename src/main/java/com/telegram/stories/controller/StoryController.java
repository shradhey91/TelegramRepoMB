package com.telegram.stories.controller;

import com.telegram.auth.security.CustomUserDetails;
import com.telegram.stories.dto.request.CreateStoryRequest;
import com.telegram.stories.dto.response.StoryGroupResponse;
import com.telegram.stories.dto.response.StoryResponse;
import com.telegram.stories.dto.response.StoryViewerResponse;
import com.telegram.stories.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Stories")
@RestController
@RequestMapping("api/stories")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping
    @Operation(summary = "Create a story with a media URL")
    public ResponseEntity<StoryResponse> createStory(@RequestBody CreateStoryRequest request, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.createStory(userId, request));
    }

    @GetMapping
    @Operation(summary = "Get the story feed for the current user")
    public ResponseEntity<List<StoryGroupResponse>> getFeed(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.getStoryFeed(userId));
    }

    @PostMapping("/{storyId}/view")
    @Operation(summary = "Mark a story as viewed")
    public ResponseEntity<Void> viewStory(@PathVariable Long storyId, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        storyService.viewStory(storyId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{storyId}/viewers")
    @Operation(summary = "Get viewers of a story (owner only)")
    public ResponseEntity<List<StoryViewerResponse>> getViewers(@PathVariable Long storyId, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.getStoryViewers(storyId, userId));
    }

    @DeleteMapping("/{storyId}")
    @Operation(summary = "Delete a story")
    public ResponseEntity<Void> deleteStory(@PathVariable Long storyId, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        storyService.deleteStory(storyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a story with a media file")
    public ResponseEntity<StoryResponse> uploadStory(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            Authentication authentication) {

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.uploadStory(userId, file, caption));
    }

    @GetMapping("/{storyId}/media")
    @Operation(summary = "Get media bytes for a story")
    public ResponseEntity<byte[]> getStoryMedia(
            @PathVariable Long storyId,
            Authentication authentication) {

        byte[] data = storyService.getStoryMedia(storyId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}