package com.demo.adventurebook.story.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.StoryDto;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.Story;
import com.demo.adventurebook.story.entity.StorySection;
import com.demo.adventurebook.story.ingest.service.BookPersistenceService;
import com.demo.adventurebook.story.repository.StoryRepository;
import com.demo.adventurebook.story.repository.StorySectionRepository;
import com.demo.adventurebook.story.repository.StorySpecifications;
import com.demo.adventurebook.story.service.internal.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final StorySectionRepository storySectionRepository;
    private final StoryValidationService storyValidationService;
    private final BookPersistenceService bookPersistenceService;

    public Optional<Story> findStory(Long id) {
        return storyRepository.findById(id);
    }

    public Optional<StorySection> findStorySection(Long storyId, String sectionExternalId) {
        return storySectionRepository.findByStoryIdAndExternalId(storyId, sectionExternalId);
    }

    public Optional<StorySection> findBeginStorySection(Long storyId) {
        return storySectionRepository.findBeginSectionByStoryId(storyId);
    }

    public Page<StoryDto> getStories(String q, Difficulty difficulty, Pageable pageable) {
        Specification<Story> spec = StorySpecifications.isValid();

        if (q != null && !q.isBlank()) {
            spec = spec.and(StorySpecifications.titleOrAuthorContains(q.strip()));
        }
        if (difficulty != null) {
            spec = spec.and(StorySpecifications.hasDifficulty(difficulty));
        }

        return storyRepository.findAll(spec, pageable).map(StoryDto::new);
    }

    public void createStory(BookDto request) {
        ValidationResult result = storyValidationService.validate(request);
        if (result.isValid()) {
            bookPersistenceService.persistValidBook(request, null);
            return;
        }
        log.warn("Book '{}' from HTTP request is invalid: {}", request.getTitle(), result.getErrors());
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("\n", result.getErrors()));
    }
}
