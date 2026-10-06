package com.demo.adventurebook.story.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "story_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private StorySection storySection;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String gotoExternalId;

    @Enumerated(EnumType.STRING)
    private ConsequenceType consequenceType;

    private String consequenceValue;

    @Column(length = 2000)
    private String consequenceText;
}
