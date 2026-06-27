package com.telegram.stories.entities;

import com.telegram.auth.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
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
