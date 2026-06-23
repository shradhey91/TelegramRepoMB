package com.telegram.stories.entities;

import com.telegram.auth.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@Entity
public class StoryView {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Story story;

    @ManyToOne
    private User viewer;

    private LocalDateTime viewedAt;
}
