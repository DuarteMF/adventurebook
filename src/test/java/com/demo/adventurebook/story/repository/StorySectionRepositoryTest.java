package com.demo.adventurebook.story.repository;

import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.SectionType;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.entity.StorySection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class StorySectionRepositoryTest {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private StorySectionRepository sectionRepository;

    private Story testStory;
    private Story anotherStory;

    @BeforeEach
    void setUp() {
        testStory = new Story();
        testStory.setTitle("Test Story");
        testStory.setAuthor("Test Author");
        testStory.setDifficulty(Difficulty.MEDIUM);
        testStory.setSourceFilename("test.json");
        testStory.setValid(true);
        storyRepository.save(testStory);

        anotherStory = new Story();
        anotherStory.setTitle("Another Story");
        anotherStory.setAuthor("Another Author");
        anotherStory.setDifficulty(Difficulty.EASY);
        anotherStory.setSourceFilename("another.json");
        anotherStory.setValid(true);
        storyRepository.save(anotherStory);
    }

    @Test
    @DisplayName("findByStoryId returns all sections for a story")
    void shouldFindAllSectionsByStoryId() {
        var section1 = new StorySection();
        section1.setStory(testStory);
        section1.setExternalId("1");
        section1.setText("Section 1");
        section1.setType(SectionType.BEGIN);
        sectionRepository.save(section1);

        var section2 = new StorySection();
        section2.setStory(testStory);
        section2.setExternalId("2");
        section2.setText("Section 2");
        section2.setType(SectionType.NODE);
        sectionRepository.save(section2);

        var results = sectionRepository.findByStoryId(testStory.getId());

        assertThat(results)
                .hasSize(2)
                .extracting(StorySection::getExternalId)
                .contains("1", "2");
    }

    @Test
    @DisplayName("findByStoryId returns empty list when story has no sections")
    void shouldReturnEmptyListWhenNoSections() {
        var results = sectionRepository.findByStoryId(testStory.getId());

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByStoryId only returns sections for the specified story")
    void shouldNotReturnSectionsFromOtherStories() {
        var testSection = new StorySection();
        testSection.setStory(testStory);
        testSection.setExternalId("1");
        testSection.setText("Section for test story");
        testSection.setType(SectionType.BEGIN);
        sectionRepository.save(testSection);

        var otherSection = new StorySection();
        otherSection.setStory(anotherStory);
        otherSection.setExternalId("1");
        otherSection.setText("Section for another story");
        otherSection.setType(SectionType.BEGIN);
        sectionRepository.save(otherSection);

        var results = sectionRepository.findByStoryId(testStory.getId());

        assertThat(results)
                .hasSize(1)
                .extracting(StorySection::getText)
                .contains("Section for test story");
    }

    @Test
    @DisplayName("findByStoryIdAndExternalId returns section by story and external id")
    void shouldFindSectionByStoryAndExternalId() {
        var section = new StorySection();
        section.setStory(testStory);
        section.setExternalId("section-1");
        section.setText("Section text");
        section.setType(SectionType.NODE);
        sectionRepository.save(section);

        var result = sectionRepository.findByStoryIdAndExternalId(testStory.getId(), "section-1");

        assertThat(result)
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.getText()).isEqualTo("Section text"));
    }

    @Test
    @DisplayName("findByStoryIdAndExternalId returns empty when section not found")
    void shouldReturnEmptyWhenSectionNotFound() {
        var result = sectionRepository.findByStoryIdAndExternalId(testStory.getId(), "nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByStoryIdAndExternalId requires both story id and external id to match")
    void shouldNotFindSectionWithDifferentStoryId() {
        var section = new StorySection();
        section.setStory(testStory);
        section.setExternalId("section-1");
        section.setText("Section text");
        section.setType(SectionType.NODE);
        sectionRepository.save(section);

        var result = sectionRepository.findByStoryIdAndExternalId(anotherStory.getId(), "section-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findBeginSectionByStoryId returns the beginning section by story")
    void shouldFindBeginningSectionByStoryId() {
        var beginSection = new StorySection();
        beginSection.setStory(testStory);
        beginSection.setExternalId("begin");
        beginSection.setText("Begin section");
        beginSection.setType(SectionType.BEGIN);
        sectionRepository.save(beginSection);

        var result = sectionRepository.findBeginSectionByStoryId(testStory.getId());

        assertThat(result)
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.getText()).isEqualTo("Begin section"));
    }

    @Test
    @DisplayName("findBeginSectionByStoryId returns empty when no beginning section exists")
    void shouldReturnEmptyWhenNoBeginningSectionExists() {
        var result = sectionRepository.findBeginSectionByStoryId(testStory.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findBeginSectionByStoryId throws when a story has multiple beginning sections")
    void shouldThrowWhenMultipleBeginningSectionsExist() {
        for (int i = 1; i <= 2; i++) {
            var section = new StorySection();
            section.setStory(testStory);
            section.setExternalId("begin-" + i);
            section.setText("Beginning section " + i);
            section.setType(SectionType.BEGIN);
            sectionRepository.save(section);
        }

        assertThatThrownBy(() -> sectionRepository.findBeginSectionByStoryId(testStory.getId()))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    @Test
    @DisplayName("findBeginSectionByStoryId doesn't return sections from other stories")
    void shouldNotReturnBeginningSectionFromOtherStory() {
        var testSection = new StorySection();
        testSection.setStory(testStory);
        testSection.setExternalId("begin");
        testSection.setText("Begin section");
        testSection.setType(SectionType.BEGIN);
        sectionRepository.save(testSection);

        var result = sectionRepository.findBeginSectionByStoryId(anotherStory.getId());

        assertThat(result).isEmpty();
    }
}
