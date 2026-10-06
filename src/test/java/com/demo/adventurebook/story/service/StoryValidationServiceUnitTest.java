package com.demo.adventurebook.story.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.OptionDto;
import com.demo.adventurebook.story.dto.SectionDto;
import com.demo.adventurebook.story.entity.SectionType;
import com.demo.adventurebook.story.service.internal.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoryValidationServiceUnitTest {

    private final StoryValidationService validationService = new StoryValidationService();

    @Test
    @DisplayName("validate returns invalid for null book")
    void validate_nullBook() {
        ValidationResult result = validationService.validate(null);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Book cannot be null");
    }

    @Test
    @DisplayName("validate accepts a minimal valid book")
    void validate_minimalValidBook() {
        SectionDto begin = new SectionDto();
        begin.setId("s1");
        begin.setType(SectionType.BEGIN);
        OptionDto opt = new OptionDto();
        opt.setGotoId("s2");
        begin.setOptions(List.of(opt));

        SectionDto end = new SectionDto();
        end.setId("s2");
        end.setType(SectionType.END);

        BookDto book = new BookDto();
        book.setTitle("T");
        book.setAuthor("A");
        book.setDifficulty(com.demo.adventurebook.story.entity.Difficulty.EASY);
        book.setSections(List.of(begin, end));

        ValidationResult result = validationService.validate(book);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("validate detects invalid consequence values and missing types")
    void validate_consequenceAndTypeErrors() {
        SectionDto begin = new SectionDto();
        begin.setId("s1");
        begin.setType(SectionType.BEGIN);
        OptionDto opt = new OptionDto();
        opt.setGotoId("s2");
        // consequence with no type and invalid amount
        com.demo.adventurebook.story.dto.ConsequenceDto cons = new com.demo.adventurebook.story.dto.ConsequenceDto();
        cons.setValue("not-a-number");
        opt.setConsequence(cons);
        begin.setOptions(List.of(opt));

        SectionDto end = new SectionDto();
        end.setId("s2");
        end.setType(SectionType.END);

        BookDto book = new BookDto();
        book.setTitle("T");
        book.setAuthor("A");
        book.setDifficulty(com.demo.adventurebook.story.entity.Difficulty.EASY);
        book.setSections(List.of(begin, end));

        ValidationResult result = validationService.validate(book);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(s -> s.contains("consequence"));
    }
}

