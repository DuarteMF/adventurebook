package com.demo.adventurebook.story.ingest.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.repository.StoryRepository;
import com.demo.adventurebook.story.service.StoryValidationService;
import com.demo.adventurebook.story.service.internal.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Scans classpath book resources on startup and delegates persistence of each file
 * to {@link BookPersistenceService}, which runs each book in its own transaction.
 * <p>
 * Keeping the scanning loop in a non-transactional service ensures that a failure in
 * one file does not roll back the successfully loaded books that preceded it.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookLoaderService {

    private final StoryRepository storyRepository;
    private final BookPersistenceService bookPersistenceService;
    private final StoryValidationService storyValidationService;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    /**
     * Scans and loads books matching the default classpath pattern.
     *
     * @return list of stories that were newly persisted during this call
     */
    public List<Story> loadBooks() {
        return loadBooks("classpath:books/*.json");
    }

    /**
     * Scans and loads books matching the specified resource pattern.
     * Idempotent: files already recorded by {@code sourceFilename} are skipped.
     *
     * @param pattern a Spring resource location pattern
     * @return list of stories that were newly persisted during this call
     */
    public List<Story> loadBooks(String pattern) {
        List<Story> loaded = new ArrayList<>();
        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources(pattern);
        } catch (IOException e) {
            log.error("Failed to resolve resources for pattern '{}': {}", pattern, e.getMessage(), e);
            return loaded;
        }

        Arrays.sort(resources, Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            if (storyRepository.existsBySourceFilename(filename)) {
                log.info("Story from file '{}' already exists in database. Skipping.", filename);
                continue;
            }

            try {
                Story story = readValidateAndStore(resource, filename);
                loaded.add(story);
            } catch (IOException e) {
                log.error("Failed to open book file '{}': {}", filename, e.getMessage(), e);
            } catch (RuntimeException e) {
                log.error("Failed to store book file '{}': {}", filename, e.getMessage(), e);
            }
        }

        return loaded;
    }

    /**
     * Reads, validates, and persists a single book resource.
     * <p>
     * Invalid books (unparseable, empty, or failing validation rules) are stored as a
     * Story shell with {@code valid = false} and <em>no sections</em>. Since the book
     * cannot be played, storing its sections would only add unreachable, misleading data.
     * </p>
     *
     * @param resource the resource holding the contents of book's JSON file
     * @param filename the filename, used for idempotency and fallback title derivation
     * @return the persisted Story entity
     */
    private Story readValidateAndStore(Resource resource, String filename) throws IOException {
        byte[] content;
        try (InputStream is = resource.getInputStream()) {
            content = is.readAllBytes();
        }

        if (content.length == 0 || new String(content, StandardCharsets.UTF_8).isBlank()) {
            log.warn("Book file '{}' is empty. Marking as invalid story.", filename);
            return bookPersistenceService.persistInvalidShell(filename, null);
        }

        BookDto bookDto;
        try {
            bookDto = objectMapper.readValue(content, BookDto.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON for file '{}': {}. Marking as invalid story.", filename, e.getMessage());
            return bookPersistenceService.persistInvalidShell(filename, null);
        }

        ValidationResult result = storyValidationService.validate(bookDto);
        if (result.isValid()) {
            return bookPersistenceService.persistValidBook(bookDto, filename);
        }
        String title = bookDto != null ? bookDto.getTitle() : null;
        log.warn("Book '{}' from file '{}' is invalid: {}", title, filename, result.getErrors());
        return bookPersistenceService.persistInvalidShell(filename, bookDto);
    }
}
