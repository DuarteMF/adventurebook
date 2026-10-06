package com.demo.adventurebook.adventure.controller;

import com.demo.adventurebook.adventure.dto.AdventureChoiceRequestDto;
import com.demo.adventurebook.adventure.dto.AdventureDto;
import com.demo.adventurebook.adventure.dto.AdventureRequestDto;
import com.demo.adventurebook.adventure.service.AdventureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/adventures")
@RequiredArgsConstructor
@Validated
public class AdventureController {

    private final AdventureService adventureService;

    /**
     * POST /adventures
     * Start a new adventure. Returns 201 Created with the adventure DTO.
     * Returns 404 if the story is not found, 409 if the story is invalid.
     */
    @PostMapping
    public ResponseEntity<AdventureDto> createAdventure(@RequestBody @Valid AdventureRequestDto request) {
        AdventureDto adventure = adventureService.createAdventure(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(adventure);
    }

    /**
     * GET /adventures/{id}
     * Fetch an adventure with sanitized options (no gotoId) and last consequence message.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdventureDto> getAdventure(@PathVariable Long id) {
        return ResponseEntity.ok(adventureService.getAdventure(id));
    }

    /**
     * POST /adventures/{id}/choices
     * Submit a choice for the current section. Applies health effects, advances section,
     * and resolves the new game status.
     */
    @PostMapping("/{id}/choices")
    public ResponseEntity<AdventureDto> makeChoice(
            @PathVariable Long id,
            @RequestBody @Valid AdventureChoiceRequestDto request
    ) {
        return ResponseEntity.ok(adventureService.makeChoice(id, request.getOptionId()));
    }

    /**
     * PATCH /adventures/{id}/pause
     * Pauses an adventure.
     * Returns 404 if adventure can't be found.
     * Returns 409 if adventure is in a state that can't be paused.
     */
    @PatchMapping("/{id}/pause")
    public ResponseEntity<Void> pauseAdventure(@PathVariable Long id) {
        adventureService.pauseAdventure(id);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /adventures/{id}/resume
     * Resumes an adventure.
     * Returns 404 if adventure can't be found.
     * Returns 409 if adventure is in a state that can't be resumed.
     */
    @PatchMapping("/{id}/resume")
    public ResponseEntity<Void> resumeAdventure(@PathVariable Long id) {
        adventureService.resumeAdventure(id);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /adventures/{id}/stop
     * Abandons an adventure.
     * Returns 404 if adventure can't be found.
     * Returns 409 if adventure is in a finished state.
     */
    @PatchMapping("/{id}/stop")
    public ResponseEntity<Void> abandonAdventure(@PathVariable Long id) {
        adventureService.abandonAdventure(id);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /adventures/{id}/save
     * Saves an adventure to be continued later.
     * Made idempotent (saving an already saved adventure returns the same status, but in reality does nothing)
     * Returns 404 if adventure can't be found.
     */
    @PatchMapping("/{id}/save")
    public ResponseEntity<Void> saveAdventure(@PathVariable Long id) {
        adventureService.saveAdventure(id);
        return ResponseEntity.ok().build();
    }
}
