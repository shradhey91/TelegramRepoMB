package com.telegram.stories.entities;

import com.telegram.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Story {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private User user;

    private String mediaFileId;

    private String mediaUrl;

    private String caption;

    private StoryType type;

    @OneToMany(mappedBy = "story",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<StoryView> views = new ArrayList<>();

    private OffsetDateTime createdAt;

    private OffsetDateTime expiresAt;
}
