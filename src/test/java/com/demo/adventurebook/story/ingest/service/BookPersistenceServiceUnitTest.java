package com.demo.adventurebook.story.ingest.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.OptionDto;
import com.demo.adventurebook.story.dto.SectionDto;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.repository.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookPersistenceServiceUnitTest {

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private BookPersistenceService persistenceService;

    private BookDto bookDto;

    @BeforeEach
    void setUp() {
        SectionDto s1 = new SectionDto();
        s1.setId("b");
        s1.setType(com.demo.adventurebook.story.entity.SectionType.BEGIN);
        OptionDto o = new OptionDto();
        o.setGotoId("e");
        s1.setOptions(List.of(o));

        SectionDto s2 = new SectionDto();
        s2.setId("e");
        s2.setType(com.demo.adventurebook.story.entity.SectionType.END);

        bookDto = new BookDto();
        bookDto.setTitle("The Book");
        bookDto.setAuthor("Author");
        bookDto.setDifficulty(Difficulty.MEDIUM);
        bookDto.setSections(List.of(s1, s2));
    }

    @Test
    @DisplayName("persistValidBook builds story and saves via repository")
    void persistValidBook_saves() {
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Story saved = persistenceService.persistValidBook(bookDto, "file.json");

        assertThat(saved.getTitle()).isEqualTo("The Book");
        assertThat(saved.getAuthor()).isEqualTo("Author");
        assertThat(saved.isValid()).isTrue();
        assertThat(saved.getStorySections()).hasSize(2);
        verify(storyRepository).save(any(Story.class));
    }

    @Test
    @DisplayName("persistInvalidShell derives title and defaults when book missing")
    void persistInvalidShell_defaults() {
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Story s = persistenceService.persistInvalidShell("my-story.json", null);

        assertThat(s.isValid()).isFalse();
        assertThat(s.getTitle()).isEqualTo("My Story");
        assertThat(s.getAuthor()).isEqualTo("Unknown");
        assertThat(s.getDifficulty()).isEqualTo(Difficulty.EASY);
    }
}

