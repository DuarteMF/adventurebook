package com.demo.adventurebook.adventure.repository;

import com.demo.adventurebook.adventure.entity.AdventureStatus;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.SectionType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AdventureRepositoryTest {

    @Autowired
    private AdventureRepository adventureRepository;

    @Autowired
    private EntityManager entityManager;

    private Long staleId;
    private Long abandonedId;
    private Long savedRecentId;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("INSERT INTO stories (author, difficulty, source_filename, title, valid, id) VALUES (?, ?, ?, ?, ?, default)")
                .setParameter(1, "Ann")
                .setParameter(2, Difficulty.HARD.name())
                .setParameter(3, "dragon-quest.json")
                .setParameter(4, "Dragon Quest")
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
                "2024-01-01T00:00:00Z");
        abandonedId = insertAdventure(storyId, sectionId, AdventureStatus.ABANDONED, false,
                "2024-01-01T12:00:00Z");
        savedRecentId = insertAdventure(storyId, sectionId, AdventureStatus.IN_PROGRESS, true,
                "2024-01-01T00:00:00Z");

        entityManager.createNativeQuery("UPDATE adventures SET created_at = :old, updated_at = :old WHERE id = :id")
                .setParameter("old", Instant.parse("2024-01-01T00:00:00Z"))
                .setParameter("id", staleId)
                .executeUpdate();

        entityManager.clear();
    }

    @Test
    @DisplayName("findPurgeCandidateIds returns stale unsaved and abandoned adventures")
    void findPurgeCandidateIds_shouldMatchStaleAndAbandoned() {
        Instant cutoff = Instant.parse("2024-01-02T00:00:00Z");
        Set<Long> ids = adventureRepository.findPurgeCandidateIds(cutoff);

        assertThat(ids).containsExactlyInAnyOrder(staleId, abandonedId);
    }

    @Test
    @DisplayName("deleteIfStale removes only the matching eligible adventure")
    void deleteIfStale_shouldDeleteOnlyEligibleAdventure() {
        Instant cutoff = Instant.parse("2024-01-02T00:00:00Z");
        int deleted = adventureRepository.deleteIfStale(abandonedId, cutoff);

        entityManager.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(adventureRepository.findById(abandonedId)).isEmpty();
    }

    @Test
    @DisplayName("deleteIfStale removes eligible stale adventure by id")
    void deleteIfStale_shouldDeleteEligibleStaleAdventure() {
        Instant cutoff = Instant.parse("2024-01-02T00:00:00Z");

        int deleted = adventureRepository.deleteIfStale(staleId, cutoff);

        entityManager.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(adventureRepository.findById(staleId)).isEmpty();
    }

    @Test
    @DisplayName("deleteIfStale does not remove an ineligible (saved) adventure")
    void deleteIfStale_shouldNotDeleteIneligibleSavedAdventure() {
        Instant cutoff = Instant.parse("2024-01-02T00:00:00Z");

        int deleted = adventureRepository.deleteIfStale(savedRecentId, cutoff);

        entityManager.clear();

        assertThat(deleted).isEqualTo(0);
        // none should be removed
        assertThat(adventureRepository.findById(savedRecentId)).isPresent();
    }

    private Long insertAdventure(Long storyId, Long sectionId, AdventureStatus status, boolean saved,
                                 String updatedAt) {
        entityManager.createNativeQuery("INSERT INTO adventures (created_at, current_section_id, health, last_consequence_message, saved, status, story_id, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, Instant.parse("2023-12-31T00:00:00Z"))
                .setParameter(2, sectionId)
                .setParameter(3, 10)
                .setParameter(4, null)
                .setParameter(5, saved)
                .setParameter(6, status.name())
                .setParameter(7, storyId)
                .setParameter(8, Instant.parse(updatedAt))
                .setParameter(9, 0L)
                .executeUpdate();

        return ((Number) entityManager.createNativeQuery("SELECT MAX(id) FROM adventures").getSingleResult()).longValue();
    }
}
