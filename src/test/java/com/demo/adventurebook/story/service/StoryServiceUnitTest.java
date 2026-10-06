package com.demo.adventurebook.story.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.SectionDto;
import com.demo.adventurebook.story.dto.StoryDto;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.SectionType;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.entity.StorySection;
import com.demo.adventurebook.story.ingest.service.BookPersistenceService;
import com.demo.adventurebook.story.repository.StoryRepository;
import com.demo.adventurebook.story.repository.StorySectionRepository;
import com.demo.adventurebook.story.service.internal.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryServiceUnitTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StorySectionRepository storySectionRepository;

    @Mock
    private StoryValidationService storyValidationService;

    @Mock
    private BookPersistenceService bookPersistenceService;

    @InjectMocks
    private StoryService consultingService;

    private Story story;
    private StorySection section;

    @BeforeEach
    void setUp() {
        story = Story.builder().id(5L).title("Test").author("A").difficulty(Difficulty.EASY).valid(true).build();
        section = StorySection.builder().id(10L).story(story).externalId("begin").build();
    }

    @Test
    @DisplayName("findStory delegates to repository")
    void findStory_delegatesToRepository() {
        when(storyRepository.findById(5L)).thenReturn(Optional.of(story));

        Optional<Story> result = consultingService.findStory(5L);

        assertThat(result).contains(story);
        verify(storyRepository).findById(5L);
    }

    @Test
    @DisplayName("findStorySection delegates to repository")
    void findStorySection_delegates() {
        when(storySectionRepository.findByStoryIdAndExternalId(5L, "begin")).thenReturn(Optional.of(section));

        Optional<StorySection> result = consultingService.findStorySection(5L, "begin");

        assertThat(result).contains(section);
        verify(storySectionRepository).findByStoryIdAndExternalId(5L, "begin");
    }

    @Test
    @DisplayName("findBeginStorySection delegates to repository")
    void findBeginStorySection_delegates() {
        when(storySectionRepository.findBeginSectionByStoryId(5L)).thenReturn(Optional.of(section));

        Optional<StorySection> result = consultingService.findBeginStorySection(5L);

        assertThat(result).contains(section);
        verify(storySectionRepository).findBeginSectionByStoryId(5L);
    }

    @Test
    @DisplayName("getStories forwards filters to repository and maps to DTO")
    void getStories_forwardsToRepositoryAndMaps() {
        var page = new PageImpl<>(List.of(story), PageRequest.of(0, 10), 1);
        when(storyRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        var result = consultingService.getStories("q", Difficulty.EASY, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        StoryDto dto = result.getContent().getFirst();
        assertThat(dto.getTitle()).isEqualTo(story.getTitle());

        ArgumentCaptor<Specification<Story>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(storyRepository).findAll(specCaptor.capture(), any(PageRequest.class));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    // --- createStory tests ---

    @Test
    @DisplayName("createStory validates the book before persisting")
    void createStory_validatesBook() {
        BookDto validBook = validBook();
        when(storyValidationService.validate(validBook)).thenReturn(ValidationResult.valid());
        when(bookPersistenceService.persistValidBook(eq(validBook), eq(null))).thenReturn(new Story());

        consultingService.createStory(validBook);

        verify(storyValidationService).validate(validBook);
    }

    @Test
    @DisplayName("createStory persists valid book with null filename")
    void createStory_persistsValidBookWithNullFilename() {
        BookDto validBook = validBook();
        when(storyValidationService.validate(validBook)).thenReturn(ValidationResult.valid());
        Story persistedStory = new Story();
        when(bookPersistenceService.persistValidBook(validBook, null)).thenReturn(persistedStory);

        consultingService.createStory(validBook);

        verify(bookPersistenceService).persistValidBook(eq(validBook), eq(null));
    }

    @Test
    @DisplayName("createStory throws ResponseStatusException when book is invalid")
    void createStory_throwsExceptionWhenInvalid() {
        BookDto invalidBook = BookDto.builder()
                .title("Invalid")
                .author("Author")
                .difficulty(Difficulty.EASY)
                .sections(List.of())
                .build();

        when(storyValidationService.validate(invalidBook))
                .thenReturn(ValidationResult.invalid(List.of(
                        "Book has no sections",
                        "Book has no beginning section (type BEGIN)"
                )));

        assertThatThrownBy(() -> consultingService.createStory(invalidBook))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Book has no sections")
                .hasMessageContaining("Book has no beginning section (type BEGIN)");
    }

    @Test
    @DisplayName("createStory includes all validation errors in exception message")
    void createStory_includesAllErrorsInMessage() {
        BookDto invalidBook = BookDto.builder()
                .title("")
                .author("")
                .difficulty(null)
                .sections(List.of())
                .build();

        List<String> errors = List.of(
                "Book title cannot be blank",
                "Book author cannot be blank",
                "Book difficulty cannot be null"
        );

        when(storyValidationService.validate(invalidBook))
                .thenReturn(ValidationResult.invalid(errors));

        assertThatThrownBy(() -> consultingService.createStory(invalidBook))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Book title cannot be blank")
                .hasMessageContaining("Book author cannot be blank")
                .hasMessageContaining("Book difficulty cannot be null");
    }

    // --- helper ---

    private BookDto validBook() {
        return BookDto.builder()
                .title("Test Story")
                .author("Test Author")
                .difficulty(Difficulty.MEDIUM)
                .sections(List.of(
                        SectionDto.builder()
                                .id("1")
                                .text("Start")
                                .type(SectionType.BEGIN)
                                .options(List.of(
                                        com.demo.adventurebook.story.dto.OptionDto.builder()
                                                .description("Go")
                                                .gotoId("2")
                                                .build()
                                ))
                                .build(),
                        SectionDto.builder()
                                .id("2")
                                .text("End")
                                .type(SectionType.END)
                                .build()
                ))
                .build();
    }
}



