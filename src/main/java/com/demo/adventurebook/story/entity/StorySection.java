package com.demo.adventurebook.story.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "story_sections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"story_id", "external_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorySection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Lob
    @Column(nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionType type;

    @OneToMany(mappedBy = "storySection", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoryOption> options = new ArrayList<>();

    public void addOption(StoryOption option) {
        options.add(option);
        option.setStorySection(this);
    }
}
