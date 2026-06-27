package com.telegram.stories.repository;

import com.telegram.stories.entities.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface StoryViewRepo extends JpaRepository<StoryView, Long> {

    List<StoryView> findByStoryIdOrderByViewedAtDesc(Long storyId);

    boolean existsByStoryIdAndViewerId(Long storyId, Long viewerId);

    long countByStoryId(Long id);

    @Query("SELECT sv.story.id FROM StoryView sv WHERE sv.story.id IN :storyIds AND sv.viewer.id = :viewerId")
    List<Long> findViewedStoryIds(@Param("storyIds") List<Long> storyIds, @Param("viewerId") Long viewerId);

    @Query("SELECT sv.story.id, COUNT(sv) FROM StoryView sv WHERE sv.story.id IN :storyIds GROUP BY sv.story.id")
    List<Object[]> countByStoryIds(@Param("storyIds") List<Long> storyIds);

}
