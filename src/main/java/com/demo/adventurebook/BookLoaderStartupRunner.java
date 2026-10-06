package com.demo.adventurebook;

import com.demo.adventurebook.story.ingest.service.BookLoaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.book-loader.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
class BookLoaderStartupRunner {
    private final BookLoaderService bookLoaderService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        bookLoaderService.loadBooks();
    }
}
