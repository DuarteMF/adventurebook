package com.demo.adventurebook.adventure.service;

import com.demo.adventurebook.adventure.dto.AdventureDto;
import com.demo.adventurebook.adventure.dto.AdventureRequestDto;
import com.demo.adventurebook.adventure.entity.Adventure;
import com.demo.adventurebook.adventure.entity.AdventureStatus;
import com.demo.adventurebook.adventure.repository.AdventureRepository;
import com.demo.adventurebook.story.entity.*;
import com.demo.adventurebook.story.service.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdventureServiceTest {

    @Mock
    private AdventureRepository adventureRepository;

    @Mock
    private StoryService storyService;

    @InjectMocks
    private AdventureService adventureService;

    private Story validStory;
    private StorySection beginSection;
    private StorySection middleSection;
    private StorySection endSection;
    private StoryOption beginToMiddleOption;
    private StoryOption middleToEndOption;
    private Adventure adventure;

    @BeforeEach
    void setUp() {
        validStory = Story.builder()
                .id(10L)
                .title("Dragon Quest")
                .author("Ann")
                .difficulty(Difficulty.HARD)
                .valid(true)
                .storySections(new ArrayList<>())
                .build();

        beginSection = StorySection.builder()
                .id(1L)
                .story(validStory)
                .externalId("begin")
                .text("Start")
                .type(SectionType.BEGIN)
                .options(new ArrayList<>())
                .build();

        middleSection = StorySection.builder()
                .id(2L)
                .story(validStory)
                .externalId("middle")
                .text("Middle")
                .type(SectionType.NODE)
                .options(new ArrayList<>())
                .build();

        endSection = StorySection.builder()
                .id(3L)
                .story(validStory)
                .externalId("end")
                .text("End")
                .type(SectionType.END)
                .options(new ArrayList<>())
                .build();

        beginToMiddleOption = StoryOption.builder()
                .id(101L)
                .storySection(beginSection)
                .description("Enter the cave")
                .gotoExternalId("middle")
                .consequenceType(ConsequenceType.GAIN_HEALTH)
                .consequenceValue("1")
                .consequenceText("You feel stronger.")
                .build();

        middleToEndOption = StoryOption.builder()
                .id(102L)
                .storySection(middleSection)
                .description("Fight on")
                .gotoExternalId("end")
                .consequenceType(ConsequenceType.LOSE_HEALTH)
                .consequenceValue("2")
                .consequenceText("You take damage.")
                .build();

        beginSection.getOptions().add(beginToMiddleOption);
        middleSection.getOptions().add(middleToEndOption);
        validStory.getStorySections().addAll(List.of(beginSection, middleSection, endSection));

        adventure = Adventure.builder()
                .id(99L)
                .story(validStory)
                .currentStorySection(beginSection)
                .health(10)
                .status(AdventureStatus.IN_PROGRESS)
                .saved(false)
                .lastConsequenceMessage(null)
                .build();
    }

    @Test
    @DisplayName("createAdventure returns 404 when story not found")
    void createAdventure_shouldThrow404WhenStoryMissing() {
        AdventureRequestDto request = new AdventureRequestDto(123L);
        when(storyService.findStory(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adventureService.createAdventure(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException r = (ResponseStatusException) ex;
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("createAdventure rejects invalid stories with 409")
    void createAdventure_shouldRejectInvalidStory() {
        AdventureRequestDto request = new AdventureRequestDto(1L);
        validStory.setValid(false);
        when(storyService.findStory(1L)).thenReturn(Optional.of(validStory));

        assertThatThrownBy(() -> adventureService.createAdventure(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException r = (ResponseStatusException) ex;
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("createAdventure creates a new adventure at BEGIN with max health")
    void createAdventure_shouldCreateAdventureAtBegin() {
        AdventureRequestDto request = new AdventureRequestDto(10L);
        when(storyService.findStory(10L)).thenReturn(Optional.of(validStory));
        when(storyService.findBeginStorySection(10L)).thenReturn(Optional.of(beginSection));

        ArgumentCaptor<Adventure> captor = ArgumentCaptor.forClass(Adventure.class);
        when(adventureRepository.save(any(Adventure.class))).thenAnswer(invocation -> {
            Adventure a = invocation.getArgument(0);
            a.setId(999L);
            return a;
        });

        AdventureDto result = adventureService.createAdventure(request);

        assertThat(result.getStoryId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);
        assertThat(result.getHealth()).isEqualTo(10);
        assertThat(result.getCurrentSection().getId()).isEqualTo("begin");
        verify(adventureRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStorySection()).isSameAs(beginSection);
    }

    @Test
    @DisplayName("makeChoice applies a gain-health consequence and advances to the next section")
    void makeChoice_shouldApplyGainHealthAndAdvance() {
        adventure.setHealth(9);
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));
        when(storyService.findStorySection(10L, "middle")).thenReturn(Optional.of(middleSection));

        AdventureDto result = adventureService.makeChoice(99L, 101L);

        assertThat(result.getHealth()).isEqualTo(10);
        assertThat(result.getCurrentSection().getId()).isEqualTo("middle");
        assertThat(result.getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);
        assertThat(result.getLastConsequenceMessage()).isEqualTo("You feel stronger.");
    }

    @Test
    @DisplayName("makeChoice reduces health to zero and marks DIED")
    void makeChoice_shouldMarkDiedWhenHealthReachesZero() {
        adventure.setHealth(1);
        beginToMiddleOption.setConsequenceType(ConsequenceType.LOSE_HEALTH);
        beginToMiddleOption.setConsequenceValue("2");
        beginToMiddleOption.setConsequenceText("You collapse.");
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));
        when(storyService.findStorySection(10L, "middle")).thenReturn(Optional.of(middleSection));

        AdventureDto result = adventureService.makeChoice(99L, 101L);

        assertThat(result.getHealth()).isZero();
        assertThat(result.getStatus()).isEqualTo(AdventureStatus.DIED);
    }

    @Test
    @DisplayName("makeChoice transitions to WON when destination is END")
    void makeChoice_shouldMarkWonWhenMovingToEnd() {
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));
        when(storyService.findStorySection(10L, "end")).thenReturn(Optional.of(endSection));

        Adventure adventureWithMiddle = Adventure.builder()
                .id(99L)
                .story(validStory)
                .currentStorySection(middleSection)
                .health(10)
                .status(AdventureStatus.IN_PROGRESS)
                .saved(false)
                .build();
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventureWithMiddle));

        middleSection.getOptions().clear();
        middleSection.getOptions().add(middleToEndOption);

        AdventureDto result = adventureService.makeChoice(99L, 102L);

        assertThat(result.getStatus()).isEqualTo(AdventureStatus.WON);
        assertThat(result.getCurrentSection().getId()).isEqualTo("end");
    }

    @Test
    @DisplayName("makeChoice rejects option not belonging to current section")
    void makeChoice_shouldRejectWrongSectionOption() {
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> adventureService.makeChoice(99L, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException r = (ResponseStatusException) ex;
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("pauseAdventure pauses a currently active adventure")
    void pauseAdventure_shouldPauseInProgress() {
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        adventureService.pauseAdventure(99L);

        assertThat(adventure.getStatus()).isEqualTo(AdventureStatus.PAUSED);
    }

    @ParameterizedTest
    @EnumSource(value = AdventureStatus.class, names = {"PAUSED", "WON", "DIED", "ABANDONED"})
    @DisplayName("pauseAdventure rejects all non-in-progress states")
    void pauseAdventure_shouldRejectNonInProgressStates(AdventureStatus status) {
        adventure.setStatus(status);
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> adventureService.pauseAdventure(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException r = (ResponseStatusException) ex;
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("resumeAdventure resumes a paused adventure")
    void resumeAdventure_shouldResumePaused() {
        adventure.setStatus(AdventureStatus.PAUSED);
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        adventureService.resumeAdventure(99L);

        assertThat(adventure.getStatus()).isEqualTo(AdventureStatus.IN_PROGRESS);
    }

    @ParameterizedTest
    @EnumSource(value = AdventureStatus.class, names = {"IN_PROGRESS", "WON", "DIED", "ABANDONED"})
    @DisplayName("resumeAdventure rejects all non-paused states")
    void resumeAdventure_shouldRejectNonPausedStates(AdventureStatus status) {
        adventure.setStatus(status);
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> adventureService.resumeAdventure(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException r = (ResponseStatusException) ex;
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @ParameterizedTest
    @EnumSource(value = AdventureStatus.class, names = {"IN_PROGRESS", "PAUSED", "ABANDONED"})
    @DisplayName("abandonAdventure accepts active or resumable states")
    void abandonAdventure_shouldAllowNonFinishedStates(AdventureStatus status) {
        adventure.setStatus(status);
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        adventureService.abandonAdventure(99L);

        assertThat(adventure.getStatus()).isEqualTo(AdventureStatus.ABANDONED);
    }

    @ParameterizedTest
    @EnumSource(value = AdventureStatus.class, names = {"WON", "DIED"})
    @DisplayName("abandonAdventure rejects finished adventures")
    void abandonAdventure_shouldRejectFinishedAdventures(AdventureStatus status) {
        adventure.setStatus(status);
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> adventureService.abandonAdventure(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException r = (ResponseStatusException) ex;
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("saveAdventure marks the adventure saved")
    void saveAdventure_shouldSetSavedTrue() {
        when(adventureRepository.findById(99L)).thenReturn(Optional.of(adventure));

        adventureService.saveAdventure(99L);

        assertThat(adventure.isSaved()).isTrue();
    }

    @Test
    @DisplayName("findPurgeCandidateIds delegates to repository")
    void findPurgeCandidateIds_shouldDelegate() {
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(adventureRepository.findPurgeCandidateIds(cutoff)).thenReturn(Set.of(1L, 2L));

        assertThat(adventureService.findPurgeCandidateIds(cutoff)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("purgeAdventure delegates to repository")
    void purgeAdventure_shouldDelegate() {
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(adventureRepository.deleteIfStale(5L, cutoff)).thenReturn(1);

        assertThat(adventureService.purgeAdventure(5L, cutoff)).isTrue();
    }
}


