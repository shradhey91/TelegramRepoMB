package com.telegram.stories.repository;

import com.telegram.stories.entities.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepo extends JpaRepository<Story, Long> {

    List<Story> findByExpiresAtAfter(LocalDateTime now);

    List<Story> findByUserIdAndExpiresAtAfter(
            Long userId,
            LocalDateTime now
    );

    List<Story> findByExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now);
}
