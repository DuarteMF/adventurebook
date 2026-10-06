package com.demo.adventurebook.story.repository;

import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StoryRepositoryTest {

    @Autowired
    private StoryRepository storyRepository;

    @BeforeEach
    void setUp() {
        Story testStory = new Story();
        testStory.setTitle("Test Story");
        testStory.setAuthor("Test Author");
        testStory.setDifficulty(Difficulty.MEDIUM);
        testStory.setSourceFilename("test-story.json");
        testStory.setValid(true);
        storyRepository.save(testStory);
    }

    // --- findBySourceFilename ---

    @Test
    @DisplayName("findBySourceFilename returns story when filename exists")
    void shouldFindBySourceFilename() {
        var result = storyRepository.findBySourceFilename("test-story.json");

        assertThat(result)
                .isPresent()
                .hasValueSatisfying(story -> assertThat(story.getTitle()).isEqualTo("Test Story"));
    }

    @Test
    @DisplayName("findBySourceFilename returns empty when filename doesn't exist")
    void shouldReturnEmptyWhenSourceFilenameNotFound() {
        var result = storyRepository.findBySourceFilename("nonexistent.json");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findBySourceFilename is case-sensitive")
    void shouldBeCaseSensitiveForFilename() {
        var result = storyRepository.findBySourceFilename("TEST-STORY.JSON");

        assertThat(result).isEmpty();
    }

    // --- existsBySourceFilename ---

    @Test
    @DisplayName("existsBySourceFilename returns true when filename exists")
    void shouldExistBySourceFilename() {
        boolean exists = storyRepository.existsBySourceFilename("test-story.json");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsBySourceFilename returns false when filename doesn't exist")
    void shouldReturnFalseWhenSourceFilenameNotFound() {
        boolean exists = storyRepository.existsBySourceFilename("nonexistent.json");

        assertThat(exists).isFalse();
    }

    // --- JpaSpecificationExecutor ---

    @Test
    @DisplayName("findAll with titleOrAuthorContains specification filters by title")
    void shouldFilterByTitleContains() {
        var story2 = new Story();
        story2.setTitle("Dragon Quest");
        story2.setAuthor("Different Author");
        story2.setDifficulty(Difficulty.HARD);
        story2.setSourceFilename("dragon.json");
        story2.setValid(true);
        storyRepository.save(story2);

        var spec = StorySpecifications.titleOrAuthorContains("Dragon");
        var results = storyRepository.findAll(spec);

        assertThat(results)
                .hasSize(1)
                .extracting(Story::getTitle)
                .contains("Dragon Quest");
    }

    @Test
    @DisplayName("findAll with titleOrAuthorContains specification filters by author")
    void shouldFilterByAuthorContains() {
        var story2 = new Story();
        story2.setTitle("Different Title");
        story2.setAuthor("Smith");
        story2.setDifficulty(Difficulty.EASY);
        story2.setSourceFilename("smith.json");
        story2.setValid(true);
        storyRepository.save(story2);

        var spec = StorySpecifications.titleOrAuthorContains("Smith");
        var results = storyRepository.findAll(spec);

        assertThat(results)
                .hasSize(1)
                .extracting(Story::getAuthor)
                .contains("Smith");
    }

    @Test
    @DisplayName("findAll with titleOrAuthorContains is case-insensitive")
    void shouldBeCaseInsensitiveForSearchTerm() {
        var spec = StorySpecifications.titleOrAuthorContains("test");
        var results = storyRepository.findAll(spec);

        assertThat(results)
                .hasSize(1)
                .extracting(Story::getTitle)
                .contains("Test Story");
    }

    @Test
    @DisplayName("findAll with hasDifficulty specification filters by difficulty")
    void shouldFilterByDifficulty() {
        var easyStory = new Story();
        easyStory.setTitle("Easy Adventure");
        easyStory.setAuthor("Easy Author");
        easyStory.setDifficulty(Difficulty.EASY);
        easyStory.setSourceFilename("easy.json");
        easyStory.setValid(true);
        storyRepository.save(easyStory);

        var spec = StorySpecifications.hasDifficulty(Difficulty.MEDIUM);
        var results = storyRepository.findAll(spec);

        assertThat(results)
                .hasSize(1)
                .extracting(Story::getDifficulty)
                .contains(Difficulty.MEDIUM);
    }

    @Test
    @DisplayName("findAll with isValid specification filters only valid stories")
    void shouldFilterOnlyValidStories() {
        var invalidStory = new Story();
        invalidStory.setTitle("Invalid Story");
        invalidStory.setAuthor("Author");
        invalidStory.setDifficulty(Difficulty.HARD);
        invalidStory.setSourceFilename("invalid.json");
        invalidStory.setValid(false);
        storyRepository.save(invalidStory);

        var spec = StorySpecifications.isValid();
        var results = storyRepository.findAll(spec);

        assertThat(results)
                .hasSize(1)
                .extracting(Story::getTitle)
                .contains("Test Story");
    }

    @Test
    @DisplayName("findAll with combined specifications filters correctly")
    void shouldApplyCombinedSpecifications() {
        var story2 = new Story();
        story2.setTitle("Dragon Quest");
        story2.setAuthor("Author");
        story2.setDifficulty(Difficulty.HARD);
        story2.setSourceFilename("dragon.json");
        story2.setValid(true);
        storyRepository.save(story2);

        var invalidStory = new Story();
        invalidStory.setTitle("Another Dragon");
        invalidStory.setAuthor("Author");
        invalidStory.setDifficulty(Difficulty.HARD);
        invalidStory.setSourceFilename("another.json");
        invalidStory.setValid(false);
        storyRepository.save(invalidStory);

        var spec = Specification.where(StorySpecifications.titleOrAuthorContains("Dragon"))
                .and(StorySpecifications.isValid());
        var results = storyRepository.findAll(spec);

        assertThat(results)
                .hasSize(1)
                .extracting(Story::getTitle)
                .contains("Dragon Quest");
    }

    @Test
    @DisplayName("findAll with pagination works correctly")
    void shouldApplyPagination() {
        for (int i = 0; i < 5; i++) {
            var story = new Story();
            story.setTitle("Story " + i);
            story.setAuthor("Author " + i);
            story.setDifficulty(Difficulty.EASY);
            story.setSourceFilename("story-" + i + ".json");
            story.setValid(true);
            storyRepository.save(story);
        }

        var pageable = PageRequest.of(0, 3);
        var page = storyRepository.findAll(pageable);

        assertThat(page)
                .hasSize(3)
                .extracting(Story::getTitle)
                .contains("Test Story", "Story 0", "Story 1");
        assertThat(page.getTotalElements()).isEqualTo(6);
    }
}
