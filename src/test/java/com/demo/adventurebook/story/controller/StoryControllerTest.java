package com.demo.adventurebook.story.controller;

import com.demo.adventurebook.config.ConversionConfig;
import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.SectionDto;
import com.demo.adventurebook.story.dto.StoryDto;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.SectionType;
import com.demo.adventurebook.story.service.StoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.demo.adventurebook.story.controller.StoryController.class)
@Import(ConversionConfig.class)
class StoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StoryService storyService;

    // --- helpers ---

    private StoryDto story(long id, String title, String author, Difficulty difficulty, int sections) {
        return new StoryDto(id, title, author, difficulty, sections);
    }

    private void stubService(List<StoryDto> items) {
        var page = new PageImpl<>(items, PageRequest.of(0, 10), items.size());
        when(storyService.getStories(any(), any(), any()))
                .thenReturn(page);
    }

    // --- response shape ---

    @Test
    @DisplayName("GET /stories returns slim PagedResponse shape")
    void shouldReturnPagedResponseShape() throws Exception {
        stubService(List.of(story(1L, "Dragon Quest", "Ann", Difficulty.HARD, 5)));

        mockMvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                // fields that must NOT appear (Spring Page bloat)
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.numberOfElements").doesNotExist())
                .andExpect(jsonPath("$.first").doesNotExist())
                .andExpect(jsonPath("$.last").doesNotExist())
                .andExpect(jsonPath("$.empty").doesNotExist());
    }

    @Test
    @DisplayName("GET /stories returns sectionCount in each story item")
    void shouldIncludeSectionCount() throws Exception {
        stubService(List.of(story(1L, "Dragon Quest", "Ann", Difficulty.HARD, 7)));

        mockMvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sectionCount").value(7));
    }

    // --- filters are forwarded ---

    @Test
    @DisplayName("GET /stories?q= forwards q to service")
    void shouldForwardQParam() throws Exception {
        stubService(List.of());

        mockMvc.perform(get("/stories").param("q", "dragon"))
                .andExpect(status().isOk());

        verify(storyService).getStories(eq("dragon"), isNull(), any());
    }

    @Test
    @DisplayName("GET /stories?difficulty= forwards difficulty to service")
    void shouldForwardDifficultyParam() throws Exception {
        stubService(List.of());

        mockMvc.perform(get("/stories").param("difficulty", "HARD"))
                .andExpect(status().isOk());

        verify(storyService).getStories(isNull(), eq(Difficulty.HARD), any());
    }

    @Test
    @DisplayName("GET /stories?difficulty=hard accepts case-insensitive difficulty")
    void shouldAcceptLowercaseDifficulty() throws Exception {
        stubService(List.of());

        mockMvc.perform(get("/stories").param("difficulty", "hard"))
                .andExpect(status().isOk());

        verify(storyService).getStories(isNull(), eq(Difficulty.HARD), any());
    }

    @Test
    @DisplayName("GET /stories?difficulty=INVALID returns 400")
    void shouldReturn400ForBadDifficulty() throws Exception {
        mockMvc.perform(get("/stories").param("difficulty", "LEGENDARY"))
                .andExpect(status().isBadRequest());
    }

    // --- pagination params ---

    @Test
    @DisplayName("GET /stories?page=2&size=5 forwards pagination to service")
    void shouldForwardPaginationParams() throws Exception {
        var page = new PageImpl<>(List.<StoryDto>of(), PageRequest.of(2, 5), 0);
        when(storyService.getStories(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/stories").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @DisplayName("GET /stories uses default page=0, size=10 when not specified")
    void shouldUseDefaultPagination() throws Exception {
        stubService(List.of());

        mockMvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    // --- content ---

    @Test
    @DisplayName("GET /stories returns all story fields")
    void shouldReturnStoryFields() throws Exception {
        stubService(List.of(story(42L, "Crystal Caverns", "J. Smith", Difficulty.EASY, 3)));

        mockMvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].title").value("Crystal Caverns"))
                .andExpect(jsonPath("$.content[0].author").value("J. Smith"))
                .andExpect(jsonPath("$.content[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.content[0].sectionCount").value(3));
    }

    @Test
    @DisplayName("GET /stories with no results returns empty content array")
    void shouldReturnEmptyContentWhenNoResults() throws Exception {
        stubService(List.of());

        mockMvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    // --- POST endpoint tests ---

    @Test
    @DisplayName("POST /stories with valid book returns 200")
    void shouldCreateStoryWithValidBook() throws Exception {
        BookDto bookDto = validBook();

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(bookDto)))
                .andExpect(status().isOk());

        verify(storyService).createStory(any(BookDto.class));
    }

    @Test
    @DisplayName("POST /stories forwards correct book data to service")
    void shouldForwardBookDataToService() throws Exception {
        String json = """
                {
                    "title": "Test Story",
                    "author": "Test Author",
                    "difficulty": "EASY",
                    "sections": [
                        {
                            "id": "1",
                            "text": "Beginning",
                            "type": "BEGIN",
                            "options": [
                                {
                                    "description": "Go",
                                    "gotoId": "2"
                                }
                            ]
                        },
                        {
                            "id": "2",
                            "text": "End",
                            "type": "END"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(storyService).createStory(argThat(book ->
                book.getTitle().equals("Test Story") &&
                        book.getAuthor().equals("Test Author") &&
                        book.getDifficulty() == Difficulty.EASY
        ));
    }

    @Test
    @DisplayName("POST /stories returns 400 when book is invalid")
    void shouldReturn400WhenBookInvalid() throws Exception {
        BookDto invalidBook = invalidBook();
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book is invalid"))
                .when(storyService).createStory(any(BookDto.class));

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(invalidBook)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /stories returns 400 when title is blank")
    void shouldReturn400WhenTitleBlank() throws Exception {
        String json = """
                {
                    "title": "",
                    "author": "Test Author",
                    "difficulty": "EASY",
                    "sections": []
                }
                """;

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /stories returns 400 when author is blank")
    void shouldReturn400WhenAuthorBlank() throws Exception {
        String json = """
                {
                    "title": "Test Story",
                    "author": "",
                    "difficulty": "EASY",
                    "sections": []
                }
                """;

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /stories returns 400 when difficulty is missing")
    void shouldReturn400WhenDifficultyMissing() throws Exception {
        String json = """
                {
                    "title": "Test Story",
                    "author": "Test Author",
                    "sections": []
                }
                """;

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /stories returns 400 when sections are empty")
    void shouldReturn400WhenSectionsEmpty() throws Exception {
        BookDto bookDto = BookDto.builder()
                .title("Test Story")
                .author("Test Author")
                .difficulty(Difficulty.EASY)
                .sections(List.of())
                .build();

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book has no sections"))
                .when(storyService).createStory(any(BookDto.class));

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(bookDto)))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---

    private BookDto validBook() {
        return BookDto.builder()
                .title("Valid Story")
                .author("Valid Author")
                .difficulty(Difficulty.MEDIUM)
                .sections(List.of(
                        SectionDto.builder()
                                .id("1")
                                .text("Start here")
                                .type(SectionType.BEGIN)
                                .options(List.of(
                                        com.demo.adventurebook.story.dto.OptionDto.builder()
                                                .description("Continue")
                                                .gotoId("2")
                                                .build()
                                ))
                                .build(),
                        SectionDto.builder()
                                .id("2")
                                .text("The end")
                                .type(SectionType.END)
                                .build()
                ))
                .build();
    }

    private BookDto invalidBook() {
        return BookDto.builder()
                .title("Invalid Story")
                .author("Invalid Author")
                .difficulty(Difficulty.EASY)
                .sections(List.of()) // Empty sections will cause validation to fail
                .build();
    }

    private String asJson(Object obj) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
    }
}

