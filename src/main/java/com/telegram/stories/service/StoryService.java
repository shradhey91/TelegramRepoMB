package com.telegram.stories.service;

import com.telegram.auth.entity.User;
import com.telegram.common.exception.AccessDeniedException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.storage.dto.StorageFolder;
import com.telegram.storage.dto.StorageServiceProvider;
import com.telegram.storage.dto.UploadResult;
import com.telegram.stories.dto.request.CreateStoryRequest;
import com.telegram.stories.dto.response.StoryGroupResponse;
import com.telegram.stories.dto.response.StoryResponse;
import com.telegram.stories.dto.response.StoryViewerResponse;
import com.telegram.stories.entities.Story;
import com.telegram.stories.entities.StoryType;
import com.telegram.stories.entities.StoryView;
import com.telegram.stories.repository.StoryRepo;
import com.telegram.stories.repository.StoryViewRepo;
import com.telegram.user.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // FIX: Use Spring's @Transactional, not Jakarta's
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoryService {
    private final UserRepo userRepo;
    private final StoryRepo storyRepo;
    private final StoryViewRepo storyViewRepo;
    private final StorageServiceProvider storageServiceProvider;

    public StoryService(UserRepo userRepo, StoryRepo storyRepo,
                        StoryViewRepo storyViewRepo,
                        StorageServiceProvider storageServiceProvider) {
        this.userRepo = userRepo;
        this.storyRepo = storyRepo;
        this.storyViewRepo = storyViewRepo;
        this.storageServiceProvider = storageServiceProvider;
    }

    @Transactional
    public StoryResponse createStory(Long userId, CreateStoryRequest request) {
        User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Story story = Story.builder()
                .user(user)
                .mediaUrl(request.mediaUrl())
                .caption(request.caption())
                .type(request.type())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        storyRepo.save(story);

        return toResponse(story, false, 0);
    }

    @Transactional(readOnly = true)
    public List<StoryGroupResponse> getStoryFeed(Long currentUserId) {
        List<Story> stories = storyRepo.findByExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime.now());

        if (stories.isEmpty()) {
            return List.of();
        }

        List<Long> allStoryIds = stories.stream().map(Story::getId).toList();

        Set<Long> viewedStoryIds = new HashSet<>(
                storyViewRepo.findViewedStoryIds(allStoryIds, currentUserId));

        Map<Long, Long> viewCountMap = storyViewRepo.countByStoryIds(allStoryIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, List<Story>> grouped = stories.stream()
                .collect(Collectors.groupingBy(story -> story.getUser().getId()));

        return grouped.entrySet().stream().map(entry -> {
            Long userId = entry.getKey();
            List<StoryResponse> storyResponses = entry.getValue().stream()
                    .map(story -> {
                        boolean viewed = userId.equals(currentUserId)
                                || viewedStoryIds.contains(story.getId());
                        long viewCount = viewCountMap.getOrDefault(story.getId(), 0L);
                        return toResponse(story, viewed, viewCount);
                    })
                    .toList();

            Story firstStory = entry.getValue().get(0);
            boolean hasUnseenStories = storyResponses.stream().anyMatch(s -> !s.viewed());

            return new StoryGroupResponse(
                    userId,
                    firstStory.getUser().getUsername(),
                    firstStory.getUser().getAvatarUrl(),
                    hasUnseenStories,
                    storyResponses
            );
        }).toList();
    }

    @Transactional
    public void viewStory(Long storyId, Long viewerId) {
        Story story = storyRepo.findById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (story.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Story expired");
        }
        if (story.getUser().getId().equals(viewerId)) {
            return;
        }
        boolean alreadyViewed = storyViewRepo.existsByStoryIdAndViewerId(storyId, viewerId);
        if (alreadyViewed) {
            return;
        }

        User viewer = userRepo.findById(viewerId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        StoryView storyView = StoryView.builder().story(story).viewer(viewer).viewedAt(LocalDateTime.now()).build();
        storyViewRepo.save(storyView);
    }

    @Transactional(readOnly = true)
    public List<StoryViewerResponse> getStoryViewers(Long storyId, Long currentUserId) {
        Story story = storyRepo.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (!story.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only story owner can view viewers");
        }

        return storyViewRepo
                .findByStoryIdOrderByViewedAtDesc(storyId)
                .stream()
                .map(view -> new StoryViewerResponse(
                        view.getViewer().getId(),
                        view.getViewer().getUsername(),
                        view.getViewer().getDisplayName(),
                        view.getViewer().getAvatarUrl(),
                        view.getViewedAt()
                ))
                .toList();
    }

    private StoryResponse toResponse(Story story, boolean viewed, long viewerCount) {
        return new StoryResponse(
                story.getId(),
                story.getUser().getId(),
                story.getUser().getUsername(),
                story.getUser().getAvatarUrl(),
                story.getMediaUrl(),
                story.getCaption(),
                story.getType(),
                story.getCreatedAt(),
                story.getExpiresAt(),
                viewerCount,
                viewed
        );
    }

    @Transactional
    public void deleteStory(Long storyId, Long currentUserId) {
        Story story = storyRepo.findById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only delete your own story");
        }
        storageServiceProvider.delete(
                story.getMediaUrl(),
                story.getMediaFileId()
        );
        storyRepo.delete(story);
    }

    @Transactional(readOnly = true)
    public byte[] getStoryMedia(Long storyId) {
        Story story = storyRepo.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        return storageServiceProvider.download(
                story.getMediaUrl()
        );
    }

    @Transactional
    public StoryResponse uploadStory(Long userId, MultipartFile file, String caption) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UploadResult upload = storageServiceProvider.upload(file, StorageFolder.STORIES, userId);
        StoryType type;

        if (file.getContentType() != null && file.getContentType().startsWith("video/")) {
            type = StoryType.VIDEO;
        } else {
            type = StoryType.IMAGE;
        }

        Story story = Story.builder()
                .user(user)
                .mediaFileId(upload.fileId())
                .mediaUrl(upload.fileName())
                .caption(caption)
                .type(type)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        storyRepo.save(story);
        return toResponse(story, true, 0);
    }
}