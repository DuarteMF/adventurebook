package com.demo.adventurebook.adventure.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdventureChoiceRequestDto {

    @NotNull
    private Long optionId;
}
