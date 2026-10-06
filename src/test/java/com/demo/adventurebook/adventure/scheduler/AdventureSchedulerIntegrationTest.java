package com.demo.adventurebook.adventure.scheduler;

import com.demo.adventurebook.adventure.entity.AdventureStatus;
import com.demo.adventurebook.adventure.repository.AdventureRepository;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.SectionType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AdventureSchedulerIntegrationTest {

    @Autowired
    private AdventureScheduler adventureScheduler;

    @Autowired
    private AdventureRepository adventureRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private Clock clock;

    private Long staleId;
    private Long abandonedId;
    private Long recentSavedId;
    private Long recentActiveId;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2024-01-10T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        adventureRepository.deleteAll();

        entityManager.createNativeQuery("INSERT INTO stories (author, difficulty, source_filename, title, valid, id) VALUES (?, ?, ?, ?, ?, default)")
                .setParameter(1, "Ann")
                .setParameter(2, Difficulty.HARD.name())
                .setParameter(3, "scheduler-story.json")
                .setParameter(4, "Scheduler Story")
                .setParameter(5, true)
                .executeUpdate();
        Long storyId = ((Number) entityManager.createNativeQuery("SELECT MAX(id) FROM stories").getSingleResult()).longValue();

        entityManager.createNativeQuery("INSERT INTO story_sections (external_id, story_id, text, type, id) VALUES (?, ?, ?, ?, default)")
                .setParameter(1, "begin")
                .setParameter(2, storyId)
                .setParameter(3, "Start")
                .setParameter(4, SectionType.BEGIN.name())
                .executeUpdate();
        Long sectionId = ((Number) entityManager.createNativeQuery("SELECT MAX(id) FROM story_sections").getSingleResult()).longValue();

        staleId = insertAdventure(storyId, sectionId, AdventureStatus.IN_PROGRESS, false,
                "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", null);
        abandonedId = insertAdventure(storyId, sectionId, AdventureStatus.ABANDONED, false,
                "2024-01-01T00:00:00Z", "2024-01-08T00:00:00Z", null);
        recentSavedId = insertAdventure(storyId, sectionId, AdventureStatus.IN_PROGRESS, true,
                "2024-01-09T12:00:00Z", "2024-01-09T12:00:00Z", "Kept because it was saved");
        recentActiveId = insertAdventure(storyId, sectionId, AdventureStatus.IN_PROGRESS, false,
                "2024-01-09T12:00:00Z", "2024-01-09T12:00:00Z", "Kept because it is new");

        entityManager.clear();
    }

    @Test
    @DisplayName("cleanUpOldUnsavedAdventures deletes stale and abandoned adventures but keeps recent ones")
    void shouldPurgeEligibleAdventures() {
        adventureScheduler.cleanUpOldUnsavedAdventures();

        assertThat(adventureRepository.findById(staleId)).isEmpty();
        assertThat(adventureRepository.findById(abandonedId)).isEmpty();
        assertThat(adventureRepository.findById(recentSavedId)).isPresent();
        assertThat(adventureRepository.findById(recentActiveId)).isPresent();
    }

    private Long insertAdventure(Long storyId, Long sectionId, AdventureStatus status, boolean saved,
                                 String createdAt, String updatedAt, String consequenceMessage) {
        entityManager.createNativeQuery("INSERT INTO adventures (created_at, current_section_id, health, last_consequence_message, saved, status, story_id, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, Instant.parse(createdAt))
                .setParameter(2, sectionId)
                .setParameter(3, 10)
                .setParameter(4, consequenceMessage)
                .setParameter(5, saved)
                .setParameter(6, status.name())
                .setParameter(7, storyId)
                .setParameter(8, Instant.parse(updatedAt))
                .setParameter(9, 0L)
                .executeUpdate();

        return ((Number) entityManager.createNativeQuery("SELECT MAX(id) FROM adventures").getSingleResult()).longValue();
    }
}
