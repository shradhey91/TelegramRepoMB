package com.telegram.stories.repository;

import com.telegram.stories.entities.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface StoryViewRepo extends JpaRepository<StoryView, Long> {

    List<StoryView> findByStoryIdOrderByViewedAtDesc(Long storyId);

    boolean existsByStoryIdAndViewerId(Long storyId, Long viewerId);

    long countByStoryId(Long id);
}
