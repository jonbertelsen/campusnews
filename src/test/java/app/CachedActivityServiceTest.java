package app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CachedActivityServiceTest {
    @Test
    void reusesFreshCache() throws IOException {
        StubScraper scraper = new StubScraper(List.of(activity("One")));
        CachedActivityService service = new CachedActivityService(
                scraper,
                Duration.ofMinutes(5),
                Clock.fixed(Instant.parse("2026-09-03T10:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals(1, service.getActivities().size());
        assertEquals(1, service.getActivities().size());
        assertEquals(1, scraper.calls);
    }

    @Test
    void returnsCacheWhenRefreshFails() throws IOException {
        StubScraper scraper = new StubScraper(List.of(activity("One")));
        CachedActivityService service = new CachedActivityService(
                scraper,
                Duration.ZERO,
                Clock.fixed(Instant.parse("2026-09-03T10:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals("One", service.getActivities().getFirst().title());
        scraper.exception = new IOException("network failed");

        assertEquals("One", service.getActivities().getFirst().title());
        assertEquals(2, scraper.calls);
    }

    @Test
    void throwsWhenRefreshFailsWithoutCache() {
        StubScraper scraper = new StubScraper(List.of());
        scraper.exception = new IOException("network failed");
        CachedActivityService service = new CachedActivityService(
                scraper,
                Duration.ofMinutes(5),
                Clock.systemUTC()
        );

        assertThrows(IOException.class, service::getActivities);
    }

    private static Activity activity(String title) {
        return new Activity(title, "2026-09-07T08:00:00", "2026-09-07T15:25:00", "Room", "https://cafs.dk/logo.png");
    }

    private static class StubScraper extends CafsActivityScraper {
        private final List<Activity> activities;
        private IOException exception;
        private int calls;

        private StubScraper(List<Activity> activities) {
            this.activities = activities;
        }

        @Override
        public List<Activity> fetchActivities() throws IOException {
            calls++;
            if (exception != null) {
                throw exception;
            }
            return activities;
        }
    }
}
