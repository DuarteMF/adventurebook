package com.demo.adventurebook.story.dto;

import com.demo.adventurebook.story.entity.Difficulty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookDto {
    @NotBlank
    private String title;
    @NotBlank
    private String author;
    @NotNull
    private Difficulty difficulty;
    @Valid
    @NotEmpty
    @Builder.Default
    private List<SectionDto> sections = new ArrayList<>();
}
