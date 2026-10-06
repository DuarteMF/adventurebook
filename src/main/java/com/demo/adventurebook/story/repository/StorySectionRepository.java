package com.demo.adventurebook.story.repository;

import com.demo.adventurebook.story.entity.StorySection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorySectionRepository extends JpaRepository<StorySection, Long> {

    List<StorySection> findByStoryId(Long storyId);

    Optional<StorySection> findByStoryIdAndExternalId(Long storyId, String externalId);

    @Query("""
            SELECT s
            FROM StorySection s
            WHERE s.story.id = :storyId
              AND s.type = SectionType.BEGIN
            """)
    Optional<StorySection> findBeginSectionByStoryId(@Param("storyId") Long storyId);
}
