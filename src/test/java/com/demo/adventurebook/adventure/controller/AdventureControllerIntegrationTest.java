package com.demo.adventurebook.adventure.controller;

import com.demo.adventurebook.adventure.dto.AdventureDto;
import com.demo.adventurebook.adventure.entity.Adventure;
import com.demo.adventurebook.adventure.entity.AdventureStatus;
import com.demo.adventurebook.adventure.repository.AdventureRepository;
import com.demo.adventurebook.story.entity.*;
import com.demo.adventurebook.story.repository.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AdventureControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private AdventureRepository adventureRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        adventureRepository.deleteAll();
        storyRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /adventures creates an adventure in the database from a valid story")
    void shouldCreateAdventureFromValidStory() {
        Story story = persistValidStory();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("storyId", story.getId()), headers);

        ResponseEntity<AdventureDto> response = restTemplate.postForEntity("/adventures", request, AdventureDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStoryId()).isEqualTo(story.getId());
        assertThat(response.getBody().getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);
        assertThat(response.getBody().getCurrentSection().getId()).isEqualTo("begin");

        transactionTemplate.executeWithoutResult(_ -> {
            Adventure saved = adventureRepository.findAll().getFirst();
            assertThat(saved.getStory().getId()).isEqualTo(story.getId());
            assertThat(saved.getCurrentStorySection().getExternalId()).isEqualTo("begin");
            assertThat(saved.getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);
            assertThat(saved.getHealth()).isEqualTo(10);
        });
    }

    @Test
    @DisplayName("POST /adventures/{id}/choices advances the adventure and persists the new section")
    void shouldAdvanceAdventureWhenChoiceIsSubmitted() {
        Story story = persistBranchingStory();
        StorySection beginSection = story.getStorySections().stream()
                .filter(section -> "begin".equals(section.getExternalId()))
                .findFirst()
                .orElseThrow();
        StoryOption choice = beginSection.getOptions().getFirst();

        Adventure adventure = Adventure.builder()
                .story(story)
                .currentStorySection(beginSection)
                .health(10)
                .status(AdventureStatus.IN_PROGRESS)
                .build();
        adventureRepository.save(adventure);
        Long adventureId = adventure.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("optionId", choice.getId()), headers);

        ResponseEntity<AdventureDto> response = restTemplate.postForEntity("/adventures/{id}/choices", request, AdventureDto.class, Map.of("id", adventureId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCurrentSection().getId()).isEqualTo("middle");
        assertThat(response.getBody().getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);

        transactionTemplate.executeWithoutResult(_ -> {
            Adventure reloaded = adventureRepository.findById(adventureId).orElseThrow();
            assertThat(reloaded.getCurrentStorySection().getExternalId()).isEqualTo("middle");
            assertThat(reloaded.getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);
        });
    }

    @Test
    @DisplayName("GET /adventures/{id} returns the persisted adventure")
    void shouldReturnAdventureById() {
        Story story = persistValidStory();
        Adventure adventure = Adventure.builder()
                .story(story)
                .currentStorySection(story.getStorySections().stream()
                        .filter(section -> "begin".equals(section.getExternalId()))
                        .findFirst()
                        .orElseThrow())
                .health(7)
                .status(AdventureStatus.IN_PROGRESS)
                .lastConsequenceMessage("You are on your way.")
                .build();
        adventure = adventureRepository.save(adventure);
        Long adventureId = adventure.getId();

        ResponseEntity<AdventureDto> response = restTemplate.getForEntity("/adventures/{id}", AdventureDto.class, Map.of("id", adventureId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(adventure.getId());
        assertThat(response.getBody().getStoryId()).isEqualTo(story.getId());
        assertThat(response.getBody().getLastConsequenceMessage()).isEqualTo("You are on your way.");
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/pause resumes the adventure and persists the new status")
    void shouldPauseAdventure() {
        Story story = persistValidStory();
        Adventure adventure = Adventure.builder()
                .story(story)
                .currentStorySection(story.getStorySections().stream()
                        .filter(section -> "begin".equals(section.getExternalId()))
                        .findFirst()
                        .orElseThrow())
                .health(10)
                .status(AdventureStatus.IN_PROGRESS)
                .build();
        adventure = adventureRepository.save(adventure);
        Long adventureId = adventure.getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/adventures/{id}/pause",
                HttpMethod.PATCH,
                new HttpEntity<>(new HttpHeaders()),
                Void.class,
                Map.of("id", adventureId)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Adventure reloaded = adventureRepository.findById(adventureId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AdventureStatus.PAUSED);
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/resume sets the paused adventure back to in progress")
    void shouldResumeAdventure() {
        Story story = persistValidStory();
        Adventure adventure = Adventure.builder()
                .story(story)
                .currentStorySection(story.getStorySections().stream()
                        .filter(section -> "begin".equals(section.getExternalId()))
                        .findFirst()
                        .orElseThrow())
                .health(10)
                .status(AdventureStatus.PAUSED)
                .build();
        adventure = adventureRepository.save(adventure);
        Long adventureId = adventure.getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/adventures/{id}/resume",
                HttpMethod.PATCH,
                new HttpEntity<>(new HttpHeaders()),
                Void.class,
                Map.of("id", adventureId)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Adventure reloaded = adventureRepository.findById(adventureId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/stop abandons an active adventure")
    void shouldStopAdventure() {
        Story story = persistValidStory();
        Adventure adventure = Adventure.builder()
                .story(story)
                .currentStorySection(story.getStorySections().stream()
                        .filter(section -> "begin".equals(section.getExternalId()))
                        .findFirst()
                        .orElseThrow())
                .health(9)
                .status(AdventureStatus.IN_PROGRESS)
                .build();
        adventure = adventureRepository.save(adventure);
        Long adventureId = adventure.getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/adventures/{id}/stop",
                HttpMethod.PATCH,
                new HttpEntity<>(new HttpHeaders()),
                Void.class,
                Map.of("id", adventureId)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Adventure reloaded = adventureRepository.findById(adventureId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AdventureStatus.ABANDONED);
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/save marks the adventure as saved")
    void shouldSaveAdventure() {
        Story story = persistValidStory();
        Adventure adventure = Adventure.builder()
                .story(story)
                .currentStorySection(story.getStorySections().stream()
                        .filter(section -> "begin".equals(section.getExternalId()))
                        .findFirst()
                        .orElseThrow())
                .health(10)
                .status(AdventureStatus.IN_PROGRESS)
                .build();
        adventure = adventureRepository.save(adventure);
        Long adventureId = adventure.getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/adventures/{id}/save",
                HttpMethod.PATCH,
                new HttpEntity<>(new HttpHeaders()),
                Void.class,
                Map.of("id", adventureId)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Adventure reloaded = adventureRepository.findById(adventureId).orElseThrow();
        assertThat(reloaded.isSaved()).isTrue();
    }

    private Story persistValidStory() {
        Story story = Story.builder()
                .title("Valid Story")
                .author("Author")
                .difficulty(Difficulty.EASY)
                .valid(true)
                .sourceFilename("valid-story.json")
                .build();

        StorySection begin = StorySection.builder()
                .story(story)
                .externalId("begin")
                .text("Begin")
                .type(SectionType.BEGIN)
                .options(new java.util.ArrayList<>())
                .build();

        StoryOption option = StoryOption.builder()
                .storySection(begin)
                .description("Go to end")
                .gotoExternalId("end")
                .build();
        begin.addOption(option);

        StorySection end = StorySection.builder()
                .story(story)
                .externalId("end")
                .text("End")
                .type(SectionType.END)
                .options(new java.util.ArrayList<>())
                .build();

        story.addSection(begin);
        story.addSection(end);
        return storyRepository.save(story);
    }

    private Story persistBranchingStory() {
        Story story = Story.builder()
                .title("Branching Story")
                .author("Author")
                .difficulty(Difficulty.MEDIUM)
                .valid(true)
                .sourceFilename("branching-story.json")
                .build();

        StorySection begin = StorySection.builder()
                .story(story)
                .externalId("begin")
                .text("Start")
                .type(SectionType.BEGIN)
                .options(new java.util.ArrayList<>())
                .build();

        StorySection middle = StorySection.builder()
                .story(story)
                .externalId("middle")
                .text("Middle")
                .type(SectionType.NODE)
                .options(new java.util.ArrayList<>())
                .build();

        StorySection end = StorySection.builder()
                .story(story)
                .externalId("end")
                .text("End")
                .type(SectionType.END)
                .options(new java.util.ArrayList<>())
                .build();

        StoryOption beginOption = StoryOption.builder()
                .storySection(begin)
                .description("Continue")
                .gotoExternalId("middle")
                .consequenceType(ConsequenceType.GAIN_HEALTH)
                .consequenceValue("1")
                .consequenceText("You feel stronger")
                .build();
        begin.addOption(beginOption);

        StoryOption middleOption = StoryOption.builder()
                .storySection(middle)
                .description("Finish")
                .gotoExternalId("end")
                .build();
        middle.addOption(middleOption);

        story.addSection(begin);
        story.addSection(middle);
        story.addSection(end);
        return storyRepository.save(story);
    }
}
