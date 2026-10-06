package com.demo.adventurebook.story.dto;

import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.Story;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryDto {
    private Long id;
    private String title;
    private String author;
    private Difficulty difficulty;
    private int sectionCount;

    public StoryDto(Story story) {
        this.id = story.getId();
        this.title = story.getTitle();
        this.author = story.getAuthor();
        this.difficulty = story.getDifficulty();
        this.sectionCount = story.getStorySections().size();
    }
}
