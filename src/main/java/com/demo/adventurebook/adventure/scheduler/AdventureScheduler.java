package com.demo.adventurebook.adventure.scheduler;

import com.demo.adventurebook.adventure.service.AdventureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdventureScheduler {
    private final Clock clock;
    private final AdventureService adventureService;

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @SchedulerLock(
            name = "cleanUpOldUnsavedAdventures",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT5M"
    )
    public void cleanUpOldUnsavedAdventures() {
        log.info("Starting the clean up process for old unsaved adventures");

        var cutoff = Instant.now(clock).minus(Duration.ofHours(24));
        var ids = this.adventureService.findPurgeCandidateIds(cutoff);

        int removed = 0;
        int skipped = 0;
        var failed = new ArrayList<Long>();

        for (long id : ids) {
            try {
                if (adventureService.purgeAdventure(id, cutoff)) {
                    removed++;
                } else {
                    skipped++;
                    log.debug("Adventure {} no longer eligible, skipping", id);
                }
            } catch (Exception e) {
                failed.add(id);
                log.warn("Failed to purge adventure {}", id, e);
            }
        }
        log.info("Clean up finished: {} removed, {} skipped, {} failed {}", removed, skipped, failed.size(), failed);
    }
}
