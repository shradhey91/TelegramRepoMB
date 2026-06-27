package com.telegram.stories.repository;

import com.telegram.stories.entities.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface StoryRepo extends JpaRepository<Story, Long> {

    List<Story> findByExpiresAtAfter(OffsetDateTime now);

    List<Story> findByUserIdAndExpiresAtAfter(
            Long userId,
            OffsetDateTime now
    );

    List<Story> findByExpiresAtAfterOrderByCreatedAtDesc(OffsetDateTime now);
}
