package com.demo.adventurebook.story.controller;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.OptionDto;
import com.demo.adventurebook.story.dto.SectionDto;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.SectionType;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.entity.StorySection;
import com.demo.adventurebook.story.repository.StoryRepository;
import com.demo.adventurebook.story.repository.StorySectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class StoryControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private StorySectionRepository storySectionRepository;

    @BeforeEach
    void setUp() {
        storySectionRepository.deleteAll();
        storyRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /stories returns only valid stories in a paged response")
    void shouldReturnValidStoriesPage() {
        persistStory("The Golden Compass Quest", "Alistair Sterling", Difficulty.MEDIUM, true, 3);
        persistStory("The Crystal Caverns", "Evelyn Stormrider", Difficulty.EASY, false, 0);

        ResponseEntity<String> response = restTemplate.getForEntity("/stories", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("The Golden Compass Quest");
        assertThat(response.getBody()).contains("\"totalElements\":1");
        assertThat(response.getBody()).doesNotContain("The Crystal Caverns");
    }

    @Test
    @DisplayName("GET /stories filters by query and difficulty")
    void shouldFilterStoriesByQueryAndDifficulty() {
        persistStory("Dragon Quest", "Ann", Difficulty.HARD, true, 2);
        persistStory("The Dragon's Tale", "Ann", Difficulty.EASY, true, 2);
        persistStory("A different story", "Bob", Difficulty.HARD, true, 1);

        ResponseEntity<String> response = restTemplate.getForEntity("/stories?q=dragon&difficulty=HARD", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Dragon Quest");
        assertThat(response.getBody()).doesNotContain("The Dragon's Tale");
        assertThat(response.getBody()).doesNotContain("A different story");
    }

    // --- POST endpoint integration test ---

    @Test
    @DisplayName("POST /stories with valid book persists story and returns 200")
    void shouldPersistValidBookAndReturnOk() {
        BookDto validBook = BookDto.builder()
                .title("Posted Adventure")
                .author("Test User")
                .difficulty(Difficulty.MEDIUM)
                .sections(List.of(
                        SectionDto.builder()
                                .id("1")
                                .text("You stand at a crossroads")
                                .type(SectionType.BEGIN)
                                .options(List.of(
                                        OptionDto.builder()
                                                .description("Go left")
                                                .gotoId("2")
                                                .build(),
                                        OptionDto.builder()
                                                .description("Go right")
                                                .gotoId("3")
                                                .build()
                                ))
                                .build(),
                        SectionDto.builder()
                                .id("2")
                                .text("You found treasure!")
                                .type(SectionType.END)
                                .build(),
                        SectionDto.builder()
                                .id("3")
                                .text("You fell into a trap")
                                .type(SectionType.END)
                                .build()
                ))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/stories",
                validBook,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify the story was persisted
        List<Story> allStories = storyRepository.findAll();
        assertThat(allStories).hasSize(1);

        Story createdStory = allStories.getFirst();
        assertThat(createdStory.getTitle()).isEqualTo("Posted Adventure");
        assertThat(createdStory.getAuthor()).isEqualTo("Test User");
        assertThat(createdStory.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(createdStory.isValid()).isTrue();
        assertThat(createdStory.getSourceFilename()).isNull();

        // Verify sections were persisted
        List<StorySection> sections = storySectionRepository.findAll();
        assertThat(sections).hasSize(3);

        // Verify section metadata (without accessing lazy-loaded options)
        StorySection beginSection = sections.stream()
                .filter(s -> s.getExternalId().equals("1"))
                .findFirst()
                .orElseThrow();
        assertThat(beginSection.getType()).isEqualTo(SectionType.BEGIN);
        assertThat(beginSection.getText()).isEqualTo("You stand at a crossroads");

        StorySection endSection1 = sections.stream()
                .filter(s -> s.getExternalId().equals("2"))
                .findFirst()
                .orElseThrow();
        assertThat(endSection1.getType()).isEqualTo(SectionType.END);

        StorySection endSection2 = sections.stream()
                .filter(s -> s.getExternalId().equals("3"))
                .findFirst()
                .orElseThrow();
        assertThat(endSection2.getType()).isEqualTo(SectionType.END);
    }

    private void persistStory(String title, String author, Difficulty difficulty, boolean valid, int sectionCount) {
        Story story = Story.builder()
                .title(title)
                .author(author)
                .difficulty(difficulty)
                .valid(valid)
                .sourceFilename(title.toLowerCase().replace(" ", "-") + ".json")
                .build();

        for (int i = 1; i <= sectionCount; i++) {
            StorySection section = StorySection.builder()
                    .story(story)
                    .externalId(String.valueOf(i))
                    .text("Section " + i)
                    .type(i == 1 ? SectionType.BEGIN : SectionType.NODE)
                    .build();
            story.addSection(section);
        }

        storyRepository.save(story);
    }
}

