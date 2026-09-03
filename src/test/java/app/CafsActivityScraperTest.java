package app;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CafsActivityScraperTest {
    private final CafsActivityScraper scraper = new CafsActivityScraper();

    @Test
    void parsesSameDayEndTime() {
        CafsActivityScraper.DateRange dateRange =
                scraper.parseDateRange("07-09-2026 08:00 – 15:25");

        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), dateRange.start());
        assertEquals(LocalDateTime.of(2026, 9, 7, 15, 25), dateRange.end());
    }

    @Test
    void parsesFullEndDateTime() {
        CafsActivityScraper.DateRange dateRange =
                scraper.parseDateRange("31-08-2026 07:00 – 18-09-2026 14:25");

        assertEquals(LocalDateTime.of(2026, 8, 31, 7, 0), dateRange.start());
        assertEquals(LocalDateTime.of(2026, 9, 18, 14, 25), dateRange.end());
    }

    @Test
    void allowsMissingEndDateTime() {
        CafsActivityScraper.DateRange dateRange =
                scraper.parseDateRange("07-09-2026 08:00 – ");

        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), dateRange.start());
        assertNull(dateRange.end());
    }

    @Test
    void allowsInvalidEndDateTime() {
        CafsActivityScraper.DateRange dateRange =
                scraper.parseDateRange("07-09-2026 08:00 – ikke-et-tidspunkt");

        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), dateRange.start());
        assertNull(dateRange.end());
    }

    @Test
    void parsesActivitiesFromHtmlFragment() {
        String html = """
                <div style="margin-bottom: 12px;">
                    <div>
                        <div style="float: left; max-width: 70%;">
                            <div style="font-size: 18px; line-height: 20px;">AMU Kursus: &#167;17</div>
                            <div style="color: #666666; font-size: 12px; margin-left: 1px;">
                                07-09-2026 08:00 &ndash;
                                <span>15:25</span>
                            </div>
                            <div style="color: #666666; font-size: 12px; margin-left: 1px;">Sted: Minervavej 2, smedev&#230;rkstedet</div>
                        </div>
                        <div style="float: right; max-width: 30%;">
                            <img src="/Graphics/Logo/logo_cabh.png" class="img-fluid" />
                        </div>
                        <div style="clear: both;"></div>
                    </div>
                    <hr />
                </div>
                """;

        List<Activity> activities = scraper.parseActivities(html);

        assertEquals(1, activities.size());
        Activity activity = activities.getFirst();
        assertEquals("AMU Kursus: §17", activity.title());
        assertEquals("2026-09-07T08:00:00", activity.startDateTime());
        assertEquals("2026-09-07T15:25:00", activity.endDateTime());
        assertEquals("Minervavej 2, smedeværkstedet", activity.location());
        assertEquals("https://cafs.dk/Graphics/Logo/logo_cabh.png", activity.logoUrl());
    }
}
