package com.demo.adventurebook.story.ingest.service;

import com.demo.adventurebook.story.entity.*;
import com.demo.adventurebook.story.repository.StoryOptionRepository;
import com.demo.adventurebook.story.repository.StoryRepository;
import com.demo.adventurebook.story.repository.StorySectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class BookLoaderServiceIntegrationTest {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private StorySectionRepository storySectionRepository;

    @Autowired
    private StoryOptionRepository storyOptionRepository;

    @Autowired
    private BookLoaderService bookLoaderService;

    @BeforeEach
    public void setUp() {
        storyOptionRepository.deleteAll();
        storySectionRepository.deleteAll();
        storyRepository.deleteAll();
    }

    @Test
    @DisplayName("Loader persists all 7 books from classpath:books/*.json")
    void shouldPersistAllStoriesFromClasspathOnTrigger() {
        bookLoaderService.loadBooks();
        assertThat(storyRepository.findAll()).hasSize(7);

        // 1. Crystal Caverns — invalid (section 666 has no options)
        Optional<Story> cavernsOpt = storyRepository.findBySourceFilename("crystal-caverns.json");
        assertThat(cavernsOpt).isPresent();
        Story caverns = cavernsOpt.get();
        assertThat(caverns.getTitle()).isEqualTo("The Crystal Caverns");
        assertThat(caverns.getAuthor()).isEqualTo("Evelyn Stormrider");
        assertThat(caverns.getDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(caverns.isValid()).isFalse();
        // Invalid books store no sections
        assertThat(storySectionRepository.findByStoryId(caverns.getId())).isEmpty();

        // 2. The Prisoner — invalid (section 666 has no options)
        Optional<Story> prisonerOpt = storyRepository.findBySourceFilename("the-prisoner.json");
        assertThat(prisonerOpt).isPresent();
        Story prisoner = prisonerOpt.get();
        assertThat(prisoner.getTitle()).isEqualTo("The Prisoner");
        assertThat(prisoner.getAuthor()).isEqualTo("Daniel El Fuego");
        assertThat(prisoner.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(prisoner.isValid()).isFalse();
        assertThat(storySectionRepository.findByStoryId(prisoner.getId())).isEmpty();

        // 3. Pirates of the Jade Sea — invalid (invalid gotoId 999 + section 666)
        Optional<Story> piratesOpt = storyRepository.findBySourceFilename("pirates-jade-sea.json");
        assertThat(piratesOpt).isPresent();
        Story pirates = piratesOpt.get();
        assertThat(pirates.getTitle()).isEqualTo("Pirates of the Jade Sea");
        assertThat(pirates.getAuthor()).isEqualTo("Marina Blackwood");
        assertThat(pirates.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(pirates.isValid()).isFalse();
        assertThat(storySectionRepository.findByStoryId(pirates.getId())).isEmpty();

        // 4. Dragon Quest — invalid (empty file)
        Optional<Story> dragonOpt = storyRepository.findBySourceFilename("dragon-quest.json");
        assertThat(dragonOpt).isPresent();
        Story dragon = dragonOpt.get();
        assertThat(dragon.getTitle()).isEqualTo("Dragon Quest");
        assertThat(dragon.isValid()).isFalse();
        assertThat(storySectionRepository.findByStoryId(dragon.getId())).isEmpty();

        // The remaining 3 files are just fixed versions of the non-empty failed books
    }

    @Test
    @DisplayName("Loader is idempotent — repeated calls produce no duplicate stories")
    void shouldBeIdempotentOnRepeatedRuns() {
        bookLoaderService.loadBooks();
        long countAfterFirstRun = storyRepository.count();

        List<Story> loaded = bookLoaderService.loadBooks();

        assertThat(loaded).isEmpty();
        assertThat(storyRepository.count()).isEqualTo(countAfterFirstRun);
    }

    @Test
    @DisplayName("Loads and validates a valid adventure book with valid=true and persists sections")
    void shouldLoadAndPersistValidBook() {
        bookLoaderService.loadBooks("classpath:books-valid/*.json");

        Optional<Story> validStoryOpt = storyRepository.findBySourceFilename("valid-book.json");
        assertThat(validStoryOpt).isPresent();
        Story validStory = validStoryOpt.get();

        assertThat(validStory.getTitle()).isEqualTo("The Golden Compass Quest");
        assertThat(validStory.getAuthor()).isEqualTo("Alistair Sterling");
        assertThat(validStory.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(validStory.isValid()).isTrue();
        assertThat(validStory.getSourceFilename()).isEqualTo("valid-book.json");

        List<StorySection> storySections = storySectionRepository.findByStoryId(validStory.getId());
        assertThat(storySections).hasSize(5);

        // Verify BEGIN section
        Optional<StorySection> beginSectionOpt = storySectionRepository.findByStoryIdAndExternalId(validStory.getId(), "1");
        assertThat(beginSectionOpt).isPresent();
        StorySection beginStorySection = beginSectionOpt.get();
        assertThat(beginStorySection.getType()).isEqualTo(SectionType.BEGIN);

        List<StoryOption> options = storyOptionRepository.findByStorySectionId(beginStorySection.getId());
        assertThat(options).hasSize(2);
        assertThat(options).anyMatch(o -> o.getGotoExternalId().equals("10"));
        assertThat(options).anyMatch(o -> o.getGotoExternalId().equals("20")
                && o.getConsequenceType() != null
                && o.getConsequenceValue().equals("2"));
    }
}

