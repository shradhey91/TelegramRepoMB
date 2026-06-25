package com.telegram.stories.controller;

import com.telegram.auth.security.CustomUserDetails;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.stories.dto.request.CreateStoryRequest;
import com.telegram.stories.dto.response.StoryGroupResponse;
import com.telegram.stories.dto.response.StoryResponse;
import com.telegram.stories.dto.response.StoryViewerResponse;
import com.telegram.stories.entities.Story;
import com.telegram.stories.entities.StoryType;
import com.telegram.stories.repository.StoryRepo;
import com.telegram.stories.service.StoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Stories")
@RequiredArgsConstructor
@RestController
@RequestMapping("api/stories")
public class StoryController {
    private final StoryService storyService;
    private final StoryRepo storyRepo;
    @PostMapping
    public ResponseEntity<StoryResponse> createStory(@RequestBody CreateStoryRequest request, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.createStory(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<StoryGroupResponse>> getFeed(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.getStoryFeed(userId));
    }

    @PostMapping("/{storyId}/view")
    public ResponseEntity<Void> viewStory(@PathVariable Long storyId, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        storyService.viewStory(storyId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{storyId}/viewers")
    public ResponseEntity<List<StoryViewerResponse>> getViewers(@PathVariable Long storyId, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.getStoryViewers(storyId, userId));
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteStory(@PathVariable Long storyId, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        storyService.deleteStory(storyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoryResponse> uploadStory(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            Authentication authentication) {

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(storyService.uploadStory(userId, file, caption));
    }

    @GetMapping("/{storyId}/media")
    public ResponseEntity<byte[]> getStoryMedia(@PathVariable Long storyId) {
        Story story = storyRepo.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        byte[] data = storyService.getStoryMedia(storyId);
        MediaType mediaType = story.getType() == StoryType.VIDEO ? MediaType.valueOf("video/mp4") : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok().contentType(mediaType).body(data);
    }


}