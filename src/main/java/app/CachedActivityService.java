package app;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class CachedActivityService {
    private final CafsActivityScraper scraper;
    private final Clock clock;
    private final Duration timeToLive;

    private List<Activity> cachedActivities = List.of();
    private Instant cachedAt = Instant.EPOCH;

    public CachedActivityService(CafsActivityScraper scraper, Duration timeToLive) {
        this(scraper, timeToLive, Clock.systemUTC());
    }

    CachedActivityService(CafsActivityScraper scraper, Duration timeToLive, Clock clock) {
        this.scraper = scraper;
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    public synchronized List<Activity> getActivities() throws IOException {
        if (hasFreshCache()) {
            return cachedActivities;
        }

        try {
            List<Activity> activities = List.copyOf(scraper.fetchActivities());
            cachedActivities = activities;
            cachedAt = Instant.now(clock);
            return activities;
        } catch (IOException exception) {
            if (!cachedActivities.isEmpty()) {
                return cachedActivities;
            }
            throw exception;
        }
    }

    private boolean hasFreshCache() {
        return !cachedActivities.isEmpty()
                && Instant.now(clock).isBefore(cachedAt.plus(timeToLive));
    }
}
