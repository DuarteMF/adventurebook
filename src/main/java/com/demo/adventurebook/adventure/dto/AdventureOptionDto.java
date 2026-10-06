package com.demo.adventurebook.adventure.dto;

import com.demo.adventurebook.story.entity.ConsequenceType;
import com.demo.adventurebook.story.entity.StoryOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sanitized option exposed to the player.
 * gotoExternalId is intentionally omitted to prevent spoilers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdventureOptionDto {

    private Long id;
    private String description;
    private ConsequenceType consequenceType;
    private String consequenceText;

    public static AdventureOptionDto from(StoryOption option) {
        return AdventureOptionDto.builder()
                .id(option.getId())
                .description(option.getDescription())
                .consequenceType(option.getConsequenceType())
                .consequenceText(option.getConsequenceText())
                .build();
    }
}
