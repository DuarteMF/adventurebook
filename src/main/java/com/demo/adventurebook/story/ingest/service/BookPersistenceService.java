package com.demo.adventurebook.story.ingest.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.OptionDto;
import com.demo.adventurebook.story.dto.SectionDto;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.entity.StoryOption;
import com.demo.adventurebook.story.entity.StorySection;
import com.demo.adventurebook.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Writes books to the database, one transaction per public method call.
 * <p>
 * This class is a pure writer: it receives an already parsed
 * {@link BookDto} and stores it either as a fully playable {@link Story} or as an
 * invalid, section-less shell.
 * </p>
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class BookPersistenceService {

    private final StoryRepository storyRepository;

    /**
     * Persists a valid book with all its sections and options.
     */
    public Story persistValidBook(BookDto bookDto, String filename) {
        Story story = Story.builder()
                .title(bookDto.getTitle())
                .author(bookDto.getAuthor())
                .difficulty(bookDto.getDifficulty())
                .valid(true)
                .sourceFilename(filename)
                .storySections(new ArrayList<>())
                .build();

        if (bookDto.getSections() != null) {
            for (SectionDto sectionDto : bookDto.getSections()) {
                StorySection storySection = StorySection.builder()
                        .story(story)
                        .externalId(sectionDto.getId())
                        .text(sectionDto.getText() != null ? sectionDto.getText() : "")
                        .type(sectionDto.getType())
                        .options(new ArrayList<>())
                        .build();

                if (sectionDto.getOptions() != null) {
                    for (OptionDto optionDto : sectionDto.getOptions()) {
                        StoryOption.StoryOptionBuilder optionBuilder = StoryOption.builder()
                                .storySection(storySection)
                                .description(optionDto.getDescription() != null ? optionDto.getDescription() : "")
                                .gotoExternalId(optionDto.getGotoId());

                        if (optionDto.getConsequence() != null) {
                            optionBuilder.consequenceType(optionDto.getConsequence().getType());
                            optionBuilder.consequenceValue(optionDto.getConsequence().getValue());
                            optionBuilder.consequenceText(optionDto.getConsequence().getText());
                        }

                        storySection.getOptions().add(optionBuilder.build());
                    }
                }

                story.getStorySections().add(storySection);
            }
        }

        return storyRepository.save(story);
    }

    /**
     * Persists a Story shell with {@code valid = false} and no sections.
     * Used for empty files, unparseable JSON, and books that fail validation.
     */
    public Story persistInvalidShell(String filename, BookDto bookDto) {
        String title = (bookDto != null && bookDto.getTitle() != null && !bookDto.getTitle().isBlank())
                ? bookDto.getTitle()
                : titleFromFilename(filename);
        String author = (bookDto != null && bookDto.getAuthor() != null && !bookDto.getAuthor().isBlank())
                ? bookDto.getAuthor()
                : "Unknown";
        Difficulty difficulty = (bookDto != null && bookDto.getDifficulty() != null)
                ? bookDto.getDifficulty()
                : Difficulty.EASY;

        Story story = Story.builder()
                .title(title)
                .author(author)
                .difficulty(difficulty)
                .valid(false)
                .sourceFilename(filename)
                .storySections(new ArrayList<>())
                .build();

        return storyRepository.save(story);
    }

    // --- private helpers ---

    private String titleFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Unknown Story";
        }
        String nameWithoutExt = filename.replaceFirst("[.][^.]+$", "");
        String[] parts = nameWithoutExt.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return !sb.isEmpty() ? sb.toString() : nameWithoutExt;
    }
}
