package com.demo.adventurebook.story.ingest.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.repository.StoryRepository;
import com.demo.adventurebook.story.service.StoryValidationService;
import com.demo.adventurebook.story.service.internal.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookLoaderServiceUnitTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private BookPersistenceService bookPersistenceService;

    @Mock
    private StoryValidationService storyValidationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookLoaderService loaderService;

    private void injectResolver(ResourcePatternResolver resolver) throws Exception {
        Field f = BookLoaderService.class.getDeclaredField("resourcePatternResolver");
        f.setAccessible(true);
        f.set(loaderService, resolver);
    }

    @Test
    @DisplayName("loadBooks handles empty file by persisting invalid shell")
    void loadBooks_emptyFile() throws Exception {
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        Resource resource = mock(Resource.class);
        when(resource.getFilename()).thenReturn("empty.json");
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(resolver.getResources(any())).thenReturn(new Resource[]{resource});
        when(storyRepository.existsBySourceFilename("empty.json")).thenReturn(false);

        when(bookPersistenceService.persistInvalidShell(eq("empty.json"), isNull())).thenReturn(Story.builder().id(1L).sourceFilename("empty.json").valid(false).build());

        injectResolver(resolver);

        var loaded = loaderService.loadBooks("classpath:books/*.json");

        assertThat(loaded).hasSize(1);
        verify(bookPersistenceService).persistInvalidShell(eq("empty.json"), isNull());
    }

    @Test
    @DisplayName("loadBooks parses valid JSON, validates and persists valid book")
    void loadBooks_validJson() throws Exception {
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        Resource resource = mock(Resource.class);
        byte[] content = "{\"title\":\"T\"}".getBytes(StandardCharsets.UTF_8);
        when(resource.getFilename()).thenReturn("good.json");
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(resolver.getResources(any())).thenReturn(new Resource[]{resource});
        when(storyRepository.existsBySourceFilename("good.json")).thenReturn(false);

        BookDto dto = new BookDto();
        dto.setTitle("T");
        when(objectMapper.readValue(content, BookDto.class)).thenReturn(dto);
        when(storyValidationService.validate(dto)).thenReturn(ValidationResult.valid());
        when(bookPersistenceService.persistValidBook(eq(dto), eq("good.json"))).thenReturn(Story.builder().id(2L).sourceFilename("good.json").valid(true).build());

        injectResolver(resolver);

        var loaded = loaderService.loadBooks("classpath:books/*.json");

        assertThat(loaded).hasSize(1);
        verify(bookPersistenceService).persistValidBook(eq(dto), eq("good.json"));
    }
}

