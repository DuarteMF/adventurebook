package com.demo.adventurebook.adventure.controller;

import com.demo.adventurebook.adventure.dto.AdventureDto;
import com.demo.adventurebook.adventure.dto.AdventureOptionDto;
import com.demo.adventurebook.adventure.dto.AdventureRequestDto;
import com.demo.adventurebook.adventure.dto.AdventureSectionDto;
import com.demo.adventurebook.adventure.entity.AdventureStatus;
import com.demo.adventurebook.adventure.service.AdventureService;
import com.demo.adventurebook.story.entity.ConsequenceType;
import com.demo.adventurebook.story.entity.SectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.demo.adventurebook.adventure.controller.AdventureController.class)
class AdventureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdventureService adventureService;

    @Test
    @DisplayName("POST /adventures returns 201 and created adventure")
    void createAdventure_shouldReturn201() throws Exception {
        AdventureDto dto = AdventureDto.builder()
                .id(1L)
                .storyId(10L)
                .status(AdventureStatus.IN_PROGRESS)
                .health(10)
                .currentSection(AdventureSectionDto.builder()
                        .id("begin")
                        .text("Start")
                        .type(SectionType.BEGIN)
                        .options(List.of(
                                AdventureOptionDto.builder()
                                        .id(55L)
                                        .description("Go forward")
                                        .consequenceType(ConsequenceType.GAIN_HEALTH)
                                        .consequenceText("You feel better")
                                        .build()
                        ))
                        .build())
                .build();

        when(adventureService.createAdventure(any(AdventureRequestDto.class))).thenReturn(dto);

        mockMvc.perform(post("/adventures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storyId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /adventures/{id} returns DTO")
    void getAdventure_shouldReturnAdventure() throws Exception {
        AdventureDto dto = AdventureDto.builder()
                .id(1L)
                .storyId(10L)
                .status(AdventureStatus.IN_PROGRESS)
                .health(8)
                .lastConsequenceMessage("You feel better")
                .currentSection(AdventureSectionDto.builder()
                        .id("begin")
                        .text("Start")
                        .type(SectionType.BEGIN)
                        .options(List.of())
                        .build())
                .build();

        when(adventureService.getAdventure(1L)).thenReturn(dto);

        mockMvc.perform(get("/adventures/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lastConsequenceMessage").value("You feel better"));
    }

    @Test
    @DisplayName("POST /adventures/{id}/choices returns updated DTO")
    void makeChoice_shouldReturnUpdatedAdventure() throws Exception {
        AdventureDto dto = AdventureDto.builder()
                .id(1L)
                .storyId(10L)
                .status(AdventureStatus.WON)
                .health(7)
                .currentSection(AdventureSectionDto.builder()
                        .id("end")
                        .text("You won")
                        .type(SectionType.END)
                        .options(List.of())
                        .build())
                .lastConsequenceMessage("Victory!")
                .build();

        when(adventureService.makeChoice(eq(1L), eq(99L))).thenReturn(dto);

        mockMvc.perform(post("/adventures/1/choices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.lastConsequenceMessage").value("Victory!"));
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/pause returns OK")
    void pauseAdventure_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/adventures/1/pause"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/resume returns OK")
    void resumeAdventure_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/adventures/1/resume"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/stop returns OK")
    void abandonAdventure_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/adventures/1/stop"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /adventures/{id}/save returns OK")
    void saveAdventure_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/adventures/1/save"))
                .andExpect(status().isOk());
    }
}


