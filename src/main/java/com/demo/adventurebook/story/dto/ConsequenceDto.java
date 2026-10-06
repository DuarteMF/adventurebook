package com.demo.adventurebook.story.dto;

import com.demo.adventurebook.config.JacksonConfig;
import com.demo.adventurebook.story.entity.ConsequenceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsequenceDto {
    @NotNull
    private ConsequenceType type;
    @NotBlank
    @JsonDeserialize(using = JacksonConfig.StringOrNumberDeserializer.class)
    private String value;
    @NotBlank
    private String text;
}
