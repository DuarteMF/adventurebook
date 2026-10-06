package com.demo.adventurebook.adventure.service;

import com.demo.adventurebook.adventure.dto.AdventureDto;
import com.demo.adventurebook.adventure.dto.AdventureRequestDto;
import com.demo.adventurebook.adventure.entity.Adventure;
import com.demo.adventurebook.adventure.entity.AdventureStatus;
import com.demo.adventurebook.adventure.repository.AdventureRepository;
import com.demo.adventurebook.story.entity.*;
import com.demo.adventurebook.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdventureService {

    private static final int MAX_HEALTH = 10;

    private final AdventureRepository adventureRepository;
    private final StoryService storyService;

    /**
     * POST /adventures
     * Rejects invalid books (409) or missing stories (404).
     * Initializes adventure at section BEGIN with 10 health.
     */
    @Transactional
    public AdventureDto createAdventure(AdventureRequestDto request) {
        Story story = storyService.findStory(request.getStoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Story not found with id: %d", request.getStoryId())));

        if (!story.isValid()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Story '%s' is not valid and cannot be played.", story.getTitle()));
        }

        StorySection beginStorySection = storyService.findBeginStorySection(story.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Story has no BEGIN section."));

        Adventure adventure = Adventure.builder()
                .story(story)
                .currentStorySection(beginStorySection)
                .health(MAX_HEALTH)
                .status(AdventureStatus.IN_PROGRESS)
                .build();

        adventure = adventureRepository.save(adventure);
        return AdventureDto.from(adventure);
    }

    /**
     * GET /adventures/{id}
     * Returns sanitized DTO hiding gotoId from choices and including last consequence message.
     */
    public AdventureDto getAdventure(Long id) {
        return AdventureDto.from(findAdventureOrThrow(id));
    }

    /**
     * POST /adventures/{id}/choices
     * Validates choice belongs to current section, applies health modifier,
     * evaluates status, and advances section.
     */
    @Transactional
    public AdventureDto makeChoice(Long adventureId, Long optionId) {
        Adventure adventure = findAdventureOrThrow(adventureId);

        if (adventure.getStatus() != AdventureStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Adventure is not in progress (status: %s).", adventure.getStatus()));
        }

        StorySection currentStorySection = adventure.getCurrentStorySection();

        StoryOption chosenOption = getChosenOption(optionId, currentStorySection);

        applyHealthAndConsequences(adventure, chosenOption);

        StorySection nextStorySection = advancingToNextSection(adventure, chosenOption);

        applyNewStatus(adventure, nextStorySection);

        return AdventureDto.from(adventure);
    }

    /**
     * PATCH /adventures/{id}/pause
     * Fetches adventure, verifies if it is in progress, then marks it as paused.
     */
    @Transactional
    public void pauseAdventure(Long id) {
        var adventure = findAdventureOrThrow(id);

        if (adventure.getStatus() != AdventureStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Adventure can't be paused (status: %s).", adventure.getStatus()));
        }
        adventure.setStatus(AdventureStatus.PAUSED);
    }

    /**
     * PATCH /adventures/{id}/resume
     * Fetches adventure, verifies if it is paused, then marks it as resumed.
     */
    @Transactional
    public void resumeAdventure(Long id) {
        var adventure = findAdventureOrThrow(id);

        if (adventure.getStatus() != AdventureStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Adventure can't be resumed (status: %s).", adventure.getStatus()));
        }
        adventure.setStatus(AdventureStatus.IN_PROGRESS);
    }

    /**
     * PATCH /adventures/{id}/stop
     * Fetches adventure, verifies if it is in a valid state, then marks it as abandoned.
     */
    @Transactional
    public void abandonAdventure(Long id) {
        var adventure = findAdventureOrThrow(id);

        if (adventure.getStatus() == AdventureStatus.WON || adventure.getStatus() == AdventureStatus.DIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Adventure already finished (status: %s).", adventure.getStatus()));
        }

        adventure.setStatus(AdventureStatus.ABANDONED);
    }

    /**
     * PATCH /adventures/{id}/save
     * Fetches adventure, then marks it as saved.
     */
    @Transactional
    public void saveAdventure(Long id) {
        var adventure = findAdventureOrThrow(id);

        adventure.setSaved(true);
    }

    // -------------------------------------------------------------------------

    public Set<Long> findPurgeCandidateIds(Instant cutoff) {
        return adventureRepository.findPurgeCandidateIds(cutoff);
    }

    @Transactional
    public boolean purgeAdventure(Long id, Instant cutoff) {
        return adventureRepository.deleteIfStale(id, cutoff) > 0;
    }

    private Adventure findAdventureOrThrow(Long id) {
        return adventureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Adventure not found with id: %d", id)));
    }

    private StoryOption getChosenOption(Long optionId, StorySection currentStorySection) {
        return currentStorySection.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Option %d does not belong to the current section.", optionId)));
    }

    private void applyHealthAndConsequences(Adventure adventure, StoryOption chosenOption) {
        String consequenceMessage = null;
        if (chosenOption.getConsequenceType() != null && chosenOption.getConsequenceValue() != null) {
            int amount = parseHealthAmount(chosenOption.getConsequenceValue(), chosenOption.getId());
            if (chosenOption.getConsequenceType() == ConsequenceType.LOSE_HEALTH) {
                adventure.setHealth(Math.max(0, adventure.getHealth() - amount));
            } else if (chosenOption.getConsequenceType() == ConsequenceType.GAIN_HEALTH) {
                adventure.setHealth(Math.min(MAX_HEALTH, adventure.getHealth() + amount));
            }
            consequenceMessage = chosenOption.getConsequenceText();
        }
        adventure.setLastConsequenceMessage(consequenceMessage);
    }

    private int parseHealthAmount(String value, Long optionId) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Invalid health value '%s' on option %d.", value, optionId));
        }
    }

    private StorySection advancingToNextSection(Adventure adventure, StoryOption chosenOption) {
        StorySection nextStorySection = storyService
                .findStorySection(adventure.getStory().getId(), chosenOption.getGotoExternalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Destination section '%s' not found.", chosenOption.getGotoExternalId())));
        adventure.setCurrentStorySection(nextStorySection);
        return nextStorySection;
    }

    private void applyNewStatus(Adventure adventure, StorySection nextStorySection) {
        AdventureStatus newStatus;
        if (adventure.getHealth() == 0) {
            newStatus = AdventureStatus.DIED;
        } else if (nextStorySection.getType() == SectionType.END) {
            newStatus = AdventureStatus.WON;
        } else {
            newStatus = AdventureStatus.IN_PROGRESS;
        }
        adventure.setStatus(newStatus);
    }
}
