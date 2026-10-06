package com.demo.adventurebook.story.repository;

import com.demo.adventurebook.story.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StoryOptionRepositoryTest {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private StorySectionRepository sectionRepository;

    @Autowired
    private StoryOptionRepository optionRepository;

    private Story testStory;
    private StorySection testSection;

    @BeforeEach
    void setUp() {
        testStory = new Story();
        testStory.setTitle("Test Story");
        testStory.setAuthor("Test Author");
        testStory.setDifficulty(Difficulty.MEDIUM);
        testStory.setSourceFilename("test.json");
        testStory.setValid(true);
        storyRepository.save(testStory);

        testSection = new StorySection();
        testSection.setStory(testStory);
        testSection.setExternalId("section-1");
        testSection.setText("Section text");
        testSection.setType(SectionType.NODE);
        sectionRepository.save(testSection);
    }

    // --- findByStorySectionId ---

    @Test
    @DisplayName("findByStorySectionId returns all options for a section")
    void shouldFindAllOptionsByStorySectionId() {
        var option1 = new StoryOption();
        option1.setStorySection(testSection);
        option1.setDescription("Option 1");
        option1.setGotoExternalId("section-2");
        optionRepository.save(option1);

        var option2 = new StoryOption();
        option2.setStorySection(testSection);
        option2.setDescription("Option 2");
        option2.setGotoExternalId("section-3");
        optionRepository.save(option2);

        var results = optionRepository.findByStorySectionId(testSection.getId());

        assertThat(results)
                .hasSize(2)
                .extracting(StoryOption::getDescription)
                .contains("Option 1", "Option 2");
    }

    @Test
    @DisplayName("findByStorySectionId returns empty list when no options exist")
    void shouldReturnEmptyListWhenNoOptions() {
        var results = optionRepository.findByStorySectionId(testSection.getId());

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByStorySectionId only returns options for specified section")
    void shouldNotReturnOptionsFromOtherSections() {
        var section2 = new StorySection();
        section2.setStory(testStory);
        section2.setExternalId("section-2");
        section2.setText("Section 2 text");
        section2.setType(SectionType.NODE);
        sectionRepository.save(section2);

        var option1 = new StoryOption();
        option1.setStorySection(testSection);
        option1.setDescription("Option for section 1");
        option1.setGotoExternalId("section-3");
        optionRepository.save(option1);

        var option2 = new StoryOption();
        option2.setStorySection(section2);
        option2.setDescription("Option for section 2");
        option2.setGotoExternalId("section-4");
        optionRepository.save(option2);

        var results = optionRepository.findByStorySectionId(testSection.getId());

        assertThat(results)
                .hasSize(1)
                .extracting(StoryOption::getDescription)
                .contains("Option for section 1");
    }
}
