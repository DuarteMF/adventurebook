package com.demo.adventurebook.story.controller;

import com.demo.adventurebook.common.dto.PagedResponse;
import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.StoryDto;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/stories")
@RequiredArgsConstructor
public class StoryController {

    private static final Sort DEFAULT_STORY_SORT = Sort.by(
            Sort.Order.asc("author").ignoreCase(),
            Sort.Order.asc("title").ignoreCase(),
            Sort.Order.asc("id")
    );
    private final StoryService storyService;

    @GetMapping
    public ResponseEntity<PagedResponse<StoryDto>> getStories(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Difficulty difficulty,
            @PageableDefault Pageable pageable
    ) {
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_STORY_SORT);

        return ResponseEntity.ok(PagedResponse.of(storyService.getStories(q, difficulty, effective)));
    }

    @PostMapping
    public ResponseEntity<Void> createStory(@RequestBody @Valid BookDto request) {
        storyService.createStory(request);
        return ResponseEntity.ok().build();
    }
}
