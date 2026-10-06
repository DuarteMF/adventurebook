package com.demo.adventurebook.story.dto;

import com.demo.adventurebook.story.entity.ConsequenceType;
import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.SectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.assertj.core.api.Assertions.assertThat;

class BookDtoDeserializationTest {

    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        // Ensure our String-or-number deserializer is registered for tests that
        // exercise DTO deserialization of numeric/string ids. Use the builder API
        // so we can add the module before building the mapper instance.
        SimpleModule mod = new SimpleModule();
        mod.addDeserializer(String.class, new com.demo.adventurebook.config.JacksonConfig.StringOrNumberDeserializer());

        mapper = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addModule(mod)
                .build();
    }

    @Test
    @DisplayName("Deserializes JSON with numeric IDs and string IDs to String")
    void shouldDeserializeNumericAndStringIds() {
        String json = """
                {
                  "title": "Test Caverns",
                  "author": "Explorer",
                  "difficulty": "EASY",
                  "unknownFieldToIgnore": "someValue",
                  "sections": [
                    {
                      "id": 1,
                      "text": "Start node with numeric id",
                      "type": "BEGIN",
                      "options": [
                        {
                          "description": "Choice 1",
                          "gotoId": 100
                        }
                      ]
                    },
                    {
                      "id": "100",
                      "text": "Node with string id",
                      "type": "END"
                    }
                  ]
                }
                """;

        BookDto book = mapper.readValue(json, BookDto.class);

        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo("Test Caverns");
        assertThat(book.getDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(book.getSections()).hasSize(2);

        SectionDto section1 = book.getSections().getFirst();
        assertThat(section1.getId()).isEqualTo("1");
        assertThat(section1.getType()).isEqualTo(SectionType.BEGIN);
        assertThat(section1.getOptions().getFirst().getGotoId()).isEqualTo("100");

        SectionDto section2 = book.getSections().get(1);
        assertThat(section2.getId()).isEqualTo("100");
        assertThat(section2.getType()).isEqualTo(SectionType.END);
    }

    @Test
    @DisplayName("Deserializes consequences with numeric or string value and case-insensitive enums")
    void shouldDeserializeConsequences() {
        String json = """
                {
                  "title": "Escape Room",
                  "author": "Daniel",
                  "difficulty": "hard",
                  "type": "AdventureType",
                  "sections": [
                    {
                      "id": "500",
                      "text": "Danger room",
                      "type": "BEGIN",
                      "options": [
                        {
                          "description": "Touch the flame",
                          "gotoId": 600,
                          "consequence": {
                            "type": "LOSE_HEALTH",
                            "value": 5,
                            "text": "Burned fingers"
                          }
                        },
                        {
                          "description": "Drink potion",
                          "gotoId": "600",
                          "consequence": {
                            "type": "GAIN_HEALTH",
                            "value": "10",
                            "text": "Healed"
                          }
                        }
                      ]
                    },
                    {
                      "id": "600",
                      "text": "Done",
                      "type": "END"
                    }
                  ]
                }
                """;

        BookDto book = mapper.readValue(json, BookDto.class);

        assertThat(book.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(book.getSections().getFirst().getOptions().getFirst().getConsequence().getType()).isEqualTo(ConsequenceType.LOSE_HEALTH);
        assertThat(book.getSections().getFirst().getOptions().getFirst().getConsequence().getValue()).isEqualTo("5");
        assertThat(book.getSections().getFirst().getOptions().get(1).getConsequence().getType()).isEqualTo(ConsequenceType.GAIN_HEALTH);
        assertThat(book.getSections().getFirst().getOptions().get(1).getConsequence().getValue()).isEqualTo("10");
    }
}




