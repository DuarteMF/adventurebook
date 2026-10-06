package com.demo.adventurebook.config;

import com.demo.adventurebook.story.entity.Difficulty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class ConversionConfig {

    /**
     * Allows {@code ?difficulty=hard} (and any mixed case) in query params,
     * consistent with the Jackson case-insensitive enum config for request bodies.
     */
    @Bean
    Converter<String, Difficulty> difficultyConverter() {
        return source -> Difficulty.valueOf(source.strip().toUpperCase());
    }
}
