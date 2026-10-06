package com.demo.adventurebook.adventure.dto;

import com.demo.adventurebook.story.entity.SectionType;
import com.demo.adventurebook.story.entity.StorySection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdventureSectionDto {

    private String id;
    private String text;
    private SectionType type;
    private List<AdventureOptionDto> options;

    public static AdventureSectionDto from(StorySection storySection) {
        List<AdventureOptionDto> options = storySection.getOptions().stream()
                .map(AdventureOptionDto::from)
                .toList();

        return AdventureSectionDto.builder()
                .id(storySection.getExternalId())
                .text(storySection.getText())
                .type(storySection.getType())
                .options(options)
                .build();
    }
}
