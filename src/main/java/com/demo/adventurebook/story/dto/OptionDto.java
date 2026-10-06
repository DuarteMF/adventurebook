package com.demo.adventurebook.story.dto;

import com.demo.adventurebook.config.JacksonConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class OptionDto {
    @NotBlank
    private String description;
    @NotBlank
    @JsonDeserialize(using = JacksonConfig.StringOrNumberDeserializer.class)
    private String gotoId;
    @Valid
    private ConsequenceDto consequence;
}
