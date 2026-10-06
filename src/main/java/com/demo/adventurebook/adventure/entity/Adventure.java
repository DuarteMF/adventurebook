package com.demo.adventurebook.adventure.entity;

import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.entity.StorySection;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "adventures",
        indexes = @Index(name = "idx_adventures_unsaved_updated", columnList = "saved, updated_at")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adventure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "current_section_id", nullable = false)
    private StorySection currentStorySection;

    @Builder.Default
    @Column(nullable = false)
    private int health = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdventureStatus status;

    @Builder.Default
    @Column(nullable = false)
    private boolean saved = false;

    @Column(length = 2000)
    private String lastConsequenceMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    @Setter(AccessLevel.NONE)
    private Long version;
}

