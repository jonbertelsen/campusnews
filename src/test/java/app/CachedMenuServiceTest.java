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

class CachedMenuServiceTest {
    @Test
    void reusesFreshCache() throws IOException {
        StubMenuScraper scraper = new StubMenuScraper(menu("Carapulka"));
        CachedMenuService service = new CachedMenuService(
                scraper,
                Duration.ofMinutes(5),
                Clock.fixed(Instant.parse("2026-09-03T10:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals("Carapulka", service.getMenu().items().getFirst().title());
        assertEquals("Carapulka", service.getMenu().items().getFirst().title());
        assertEquals(1, scraper.calls);
    }

    @Test
    void returnsCacheWhenRefreshFails() throws IOException {
        StubMenuScraper scraper = new StubMenuScraper(menu("Carapulka"));
        CachedMenuService service = new CachedMenuService(
                scraper,
                Duration.ZERO,
                Clock.fixed(Instant.parse("2026-09-03T10:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals("Carapulka", service.getMenu().items().getFirst().title());
        scraper.exception = new IOException("network failed");

        assertEquals("Carapulka", service.getMenu().items().getFirst().title());
        assertEquals(2, scraper.calls);
    }

    @Test
    void throwsWhenRefreshFailsWithoutCache() {
        StubMenuScraper scraper = new StubMenuScraper(menu("Carapulka"));
        scraper.exception = new IOException("network failed");
        CachedMenuService service = new CachedMenuService(
                scraper,
                Duration.ofMinutes(5),
                Clock.systemUTC()
        );

        assertThrows(IOException.class, service::getMenu);
    }

    private static WeeklyMenu menu(String title) {
        return new WeeklyMenu(
                36,
                2026,
                List.of(new MenuItem(
                        "torsdag",
                        "torsdag 3. sep",
                        "2026-09-03",
                        title,
                        "med ris",
                        "35 kr.",
                        35,
                        "data:image/png;base64,abc123"
                ))
        );
    }

    private static class StubMenuScraper extends CafsMenuScraper {
        private final WeeklyMenu menu;
        private IOException exception;
        private int calls;

        private StubMenuScraper(WeeklyMenu menu) {
            this.menu = menu;
        }

        @Override
        public WeeklyMenu fetchMenu() throws IOException {
            calls++;
            if (exception != null) {
                throw exception;
            }
            return menu;
        }
    }
}
