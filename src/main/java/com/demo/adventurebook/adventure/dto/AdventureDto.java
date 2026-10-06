package com.demo.adventurebook.adventure.dto;

import com.demo.adventurebook.adventure.entity.Adventure;
import com.demo.adventurebook.adventure.entity.AdventureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdventureDto {

    private Long id;
    private Long storyId;
    private AdventureStatus status;
    private int health;
    private AdventureSectionDto currentSection;
    private String lastConsequenceMessage;

    public static AdventureDto from(Adventure adventure) {
        return AdventureDto.builder()
                .id(adventure.getId())
                .storyId(adventure.getStory().getId())
                .status(adventure.getStatus())
                .health(adventure.getHealth())
                .currentSection(AdventureSectionDto.from(adventure.getCurrentStorySection()))
                .lastConsequenceMessage(adventure.getLastConsequenceMessage())
                .build();
    }
}
