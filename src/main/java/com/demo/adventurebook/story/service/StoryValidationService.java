package com.demo.adventurebook.story.service;

import com.demo.adventurebook.story.dto.BookDto;
import com.demo.adventurebook.story.dto.ConsequenceDto;
import com.demo.adventurebook.story.dto.OptionDto;
import com.demo.adventurebook.story.dto.SectionDto;
import com.demo.adventurebook.story.entity.SectionType;
import com.demo.adventurebook.story.service.internal.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates a {@link BookDto} against the adventure book rules.
 * <p>
 * Validation only happens on the incoming DTO representation (either from a JSON
 * file or a POST request body). Once a {@link com.demo.adventurebook.story.entity.Story}
 * is persisted, its validity is captured in the {@code valid} column; there is no need
 * to re-validate the entity.
 * </p>
 * <p>
 * A book is invalid if any of the following conditions are met:
 * <ul>
 *     <li>Book has none, or more than one beginning (SectionType.BEGIN)</li>
 *     <li>Book has no ending (SectionType.END) (multiple endings are allowed)</li>
 *     <li>Book has an invalid next section id (gotoId does not match any section id)</li>
 *     <li>A non-ending section (BEGIN or NODE) has no options</li>
 * </ul>
 */
@Component
public class StoryValidationService {

    private static final int MAX_CONSEQUENCE_AMOUNT = 10;

    public ValidationResult validate(BookDto book) {
        if (book == null) {
            return ValidationResult.invalid("Book cannot be null");
        }

        List<String> errors = new ArrayList<>();

        validateForBasicErrors(book, errors);

        List<SectionDto> sections = book.getSections();
        if (isInvalidForBasicSectionErrors(sections, errors)) {
            return ValidationResult.invalid(errors);
        }

        validateForBeginningSection(sections, errors);

        validateForEndingSection(sections, errors);

        Set<String> sectionIds = extractSectionIdsWhileWatchingForDuplicates(sections, errors);

        validateSectionsOptionsAndConsequences(sectionIds, sections, errors);

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private void validateForBasicErrors(BookDto book, List<String> errors) {
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            errors.add("Book title cannot be blank");
        }
        if (book.getAuthor() == null || book.getAuthor().isBlank()) {
            errors.add("Book author cannot be blank");
        }
        if (book.getDifficulty() == null) {
            errors.add("Book difficulty cannot be null");
        }
    }

    private boolean isInvalidForBasicSectionErrors(List<SectionDto> sections, List<String> errors) {
        if (sections == null || sections.isEmpty()) {
            errors.add("Book has no sections");
            errors.add("Book has no beginning section (type BEGIN)");
            errors.add("Book has no ending section (type END)");
            return true;
        }

        if (sections.stream().anyMatch(Objects::isNull)) {
            errors.add("Book contains a null section entry");
            return true;
        }
        return false;
    }

    private void validateForBeginningSection(List<SectionDto> sections, List<String> errors) {
        long beginCount = sections.stream()
                .filter(s -> s.getType() == SectionType.BEGIN)
                .count();
        if (beginCount == 0) {
            errors.add("Book has no beginning section (type BEGIN)");
        } else if (beginCount > 1) {
            errors.add(String.format("Book has more than one beginning section (found %d)", beginCount));
        }
    }

    private void validateForEndingSection(List<SectionDto> sections, List<String> errors) {
        long endCount = sections.stream()
                .filter(s -> s.getType() == SectionType.END)
                .count();
        if (endCount == 0) {
            errors.add("Book has no ending section (type END)");
        }
    }

    private Set<String> extractSectionIdsWhileWatchingForDuplicates(List<SectionDto> sections, List<String> errors) {
        Set<String> sectionIds = new HashSet<>();
        for (SectionDto section : sections) {
            if (section.getId() == null || section.getId().isBlank()) {
                errors.add("Section has missing or blank id");
            } else if (!sectionIds.add(section.getId())) {
                errors.add(String.format("Duplicate section id: '%s'", section.getId()));
            }
        }
        return sectionIds;
    }

    private void validateSectionsOptionsAndConsequences(Set<String> sectionIds, List<SectionDto> sections, List<String> errors) {
        for (SectionDto section : sections) {
            String sectionId = section.getId();

            // New Rule: A section MUST have a type
            if (section.getType() == null) {
                errors.add(String.format("Section '%s' does not have a type", sectionId));
            }

            // Rule: A non-ending section (BEGIN or NODE) has no options
            if (section.getType() == SectionType.BEGIN || section.getType() == SectionType.NODE) {
                if (section.getOptions() == null || section.getOptions().isEmpty()) {
                    errors.add(String.format("Non-ending section '%s' of type %s has no options", sectionId, section.getType()));
                }
            }

            // Rule: Book has an invalid next section id (gotoId does not match any section id)
            List<OptionDto> options = section.getOptions();
            if (options != null) {
                validateOptionsAndConsequences(sectionId, sectionIds, options, errors);
            }
        }
    }

    private void validateOptionsAndConsequences(String sectionId, Set<String> sectionIds, List<OptionDto> options, List<String> errors) {
        for (OptionDto option : options) {
            if (option == null) {
                errors.add(String.format("Section '%s' has a null option entry", sectionId));
                continue;
            }
            String gotoId = option.getGotoId();
            if (gotoId == null || gotoId.isBlank() || !sectionIds.contains(gotoId)) {
                errors.add(String.format("Section '%s' has option with invalid gotoId: '%s'", sectionId, gotoId));
            }
            ConsequenceDto consequence = option.getConsequence();
            if (consequence != null) {
                validateConsequences(sectionId, consequence, errors);
            }
        }
    }

    private void validateConsequences(String sectionId, ConsequenceDto consequence, List<String> errors) {
        if (consequence.getType() == null) {
            errors.add(String.format("Section '%s' has an option with a consequence but no type", sectionId));
        }
        String value = consequence.getValue();
        Integer amount = parseConsequenceAmount(value);
        if (amount == null || amount < 1 || amount > MAX_CONSEQUENCE_AMOUNT) {
            errors.add(String.format("Section '%s' has an option with an invalid consequence value: '%s' (expected an integer from 1 to %d)", sectionId, value, MAX_CONSEQUENCE_AMOUNT));
        }
    }

    private Integer parseConsequenceAmount(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
