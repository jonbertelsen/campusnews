package app;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class CachedMenuService {
    private final CafsMenuScraper scraper;
    private final Clock clock;
    private final Duration timeToLive;

    private WeeklyMenu cachedMenu;
    private Instant cachedAt = Instant.EPOCH;

    public CachedMenuService(CafsMenuScraper scraper, Duration timeToLive) {
        this(scraper, timeToLive, Clock.systemUTC());
    }

    CachedMenuService(CafsMenuScraper scraper, Duration timeToLive, Clock clock) {
        this.scraper = scraper;
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    public synchronized WeeklyMenu getMenu() throws IOException {
        if (hasFreshCache()) {
            return cachedMenu;
        }

        try {
            WeeklyMenu menu = scraper.fetchMenu();
            cachedMenu = new WeeklyMenu(menu.weekNumber(), menu.year(), java.util.List.copyOf(menu.items()));
            cachedAt = Instant.now(clock);
            return cachedMenu;
        } catch (IOException exception) {
            if (cachedMenu != null) {
                return cachedMenu;
            }
            throw exception;
        }
    }

    private boolean hasFreshCache() {
        return cachedMenu != null
                && Instant.now(clock).isBefore(cachedAt.plus(timeToLive));
    }
}
