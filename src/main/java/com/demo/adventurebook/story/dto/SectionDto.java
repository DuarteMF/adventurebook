package com.demo.adventurebook.story.dto;

import com.demo.adventurebook.config.JacksonConfig;
import com.demo.adventurebook.story.entity.SectionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SectionDto {
    @NotBlank
    @JsonDeserialize(using = JacksonConfig.StringOrNumberDeserializer.class)
    private String id;
    @NotBlank
    private String text;
    @NotNull
    private SectionType type;
    @Valid
    @Builder.Default
    private List<OptionDto> options = new ArrayList<>();
}
