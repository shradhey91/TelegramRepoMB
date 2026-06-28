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


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24))
                .build();

        storyRepo.save(story);

        return toResponse(story, false, 0);
    }

    @Transactional(readOnly = true)
    public List<StoryGroupResponse> getStoryFeed(Long currentUserId) {

        List<Story> stories = storyRepo.findByExpiresAtAfterOrderByCreatedAtDesc(OffsetDateTime.now(ZoneOffset.UTC));

        if (stories.isEmpty()) {
            return List.of();
        }

        List<Long> allStoryIds = new ArrayList<>();

        for (Story story : stories) {
            allStoryIds.add(story.getId());
        }

        Set<Long> viewedStoryIds = new HashSet<>(storyViewRepo.findViewedStoryIds(allStoryIds, currentUserId));

        List<Object[]> viewCounts = storyViewRepo.countByStoryIds(allStoryIds);

        Map<Long, Long> viewCountMap = new HashMap<>();

        for (Object[] row : viewCounts) {
            Long storyId = (Long) row[0];
            Long viewCount = (Long) row[1];
            viewCountMap.put(storyId, viewCount);
        }

        Map<Long, List<Story>> groupedStories = new HashMap<>();

        for (Story story : stories) {
            Long userId = story.getUser().getId();
            if (!groupedStories.containsKey(userId)) {
                groupedStories.put(userId, new ArrayList<>());
            }
            groupedStories.get(userId).add(story);
        }

        List<StoryGroupResponse> response = new ArrayList<>();

        for (Map.Entry<Long, List<Story>> entry : groupedStories.entrySet()) {

            Long userId = entry.getKey();
            List<Story> userStories = entry.getValue();
            List<StoryResponse> storyResponses = new ArrayList<>();

            for (Story story : userStories) {
                boolean viewed = userId.equals(currentUserId) || viewedStoryIds.contains(story.getId());

                long viewCount = viewCountMap.getOrDefault(story.getId(), 0L);

                storyResponses.add(toResponse(story, viewed, viewCount));
            }

            boolean  hasUnseenStories  = false;

            for (StoryResponse storyResponse : storyResponses) {
                if (!storyResponse.viewed()) {
                    hasUnseenStories = true;
                    break;
                }
            }

            Story firstStory = userStories.get(0);

            StoryGroupResponse storyGroup = new StoryGroupResponse(
                    userId,
                    firstStory.getUser().getUsername(),
                    firstStory.getUser().getAvatarUrl(),
                    hasUnseenStories,
                    storyResponses
            );

            response.add(storyGroup);
        }

        return response;
    }

    @Transactional
    public void viewStory(Long storyId, Long viewerId) {
        Story story = storyRepo.findById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (story.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
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
        StoryView storyView = StoryView.builder().story(story).viewer(viewer).viewedAt(OffsetDateTime.now(ZoneOffset.UTC)).build();
        storyViewRepo.save(storyView);
    }

    @Transactional(readOnly = true)
    public List<StoryViewerResponse> getStoryViewers(Long storyId, Long currentUserId) {

        Story story = storyRepo.findById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (!story.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only story owner can view viewers");
        }

        List<StoryView> storyViews = storyViewRepo.findByStoryIdOrderByViewedAtDesc(storyId);

        List<StoryViewerResponse> responses = new ArrayList<>();

        for (StoryView view : storyViews) {
            StoryViewerResponse response = new StoryViewerResponse(
                    view.getViewer().getId(),
                    view.getViewer().getUsername(),
                    view.getViewer().getDisplayName(),
                    view.getViewer().getAvatarUrl(),
                    view.getViewedAt()
            );
            responses.add(response);
        }
        return responses;
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
        storageServiceProvider.delete(story.getMediaUrl(), story.getMediaFileId());
        storyRepo.delete(story);

    }

    @Transactional(readOnly = true)
    public byte[] getStoryMedia(Long storyId) {

        Story story = storyRepo.findById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        return storageServiceProvider.download(story.getMediaUrl());

    }

    @Transactional
    public StoryResponse uploadStory(Long userId, MultipartFile file, String caption) {

        User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24))
                .build();

        storyRepo.save(story);
        return toResponse(story, true, 0);
    }
}