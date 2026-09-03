package app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CafsMenuScraperTest {
    private final CafsMenuScraper scraper = new CafsMenuScraper();

    @Test
    void parsesWeeklyMenu() {
        WeeklyMenu menu = scraper.parseMenu("""
                <html>
                <body>
                    <div class="body-content">
                        <h1>Ugens menu</h1>
                        <h2>Menu uge 36 - 2026</h2>
                        <div class="row">
                            <div class="col-md-3">
                                <h4 style="margin-top: 9px;">torsdag 3. sep</h4>
                            </div>
                            <div class="col-md-9">
                                <div style="width: 480px;">
                                    <div style="float: left; max-width: 85%;">
                                        <h3 style="margin-top: 0px; margin-bottom: 0px;">Carapulka</h3>
                                        <p style="font-size: 20px; margin-bottom: 6px;">med ris</p>
                                    </div>
                                    <div style="float: right; min-width: 50px;">
                                        <p style="background-color: steelblue; color: white;">35 kr.</p>
                                    </div>
                                    <p><img src="data:image/png;base64,abc123" /></p>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-3">
                                <h4 style="margin-top: 9px;">fredag 4. sep</h4>
                            </div>
                            <div class="col-md-9">
                                <div style="width: 480px;">
                                    <div style="float: left; max-width: 85%;">
                                        <h3 style="margin-top: 0px; margin-bottom: 0px;">Ramen suppe</h3>
                                        <p style="font-size: 20px; margin-bottom: 6px;">V&#230;lg mellem kylling eller svampe</p>
                                    </div>
                                    <div style="float: right; min-width: 50px;">
                                        <p style="background-color: steelblue; color: white;">35 kr.</p>
                                    </div>
                                    <p><img src="/Graphics/Menu/ramen.png" /></p>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """);

        assertEquals(36, menu.weekNumber());
        assertEquals(2026, menu.year());
        assertEquals(2, menu.items().size());

        MenuItem firstItem = menu.items().getFirst();
        assertEquals("torsdag", firstItem.dayName());
        assertEquals("torsdag 3. sep", firstItem.dateText());
        assertEquals("2026-09-03", firstItem.date());
        assertEquals("Carapulka", firstItem.title());
        assertEquals("med ris", firstItem.description());
        assertEquals("35 kr.", firstItem.priceText());
        assertEquals(35, firstItem.priceDkk());
        assertEquals("data:image/png;base64,abc123", firstItem.imageUrl());

        MenuItem secondItem = menu.items().get(1);
        assertEquals("fredag", secondItem.dayName());
        assertEquals("2026-09-04", secondItem.date());
        assertEquals("Ramen suppe", secondItem.title());
        assertEquals("Vælg mellem kylling eller svampe", secondItem.description());
        assertEquals("https://cafs.dk/Graphics/Menu/ramen.png", secondItem.imageUrl());
    }

    @Test
    void keepsDateTextWhenDateCannotBeParsed() {
        WeeklyMenu menu = scraper.parseMenu("""
                <div class="body-content">
                    <h2>Menu uge 1 - 2026</h2>
                    <div class="row">
                        <div class="col-md-3"><h4>mandag uden dato</h4></div>
                        <div class="col-md-9">
                            <h3>Dagens ret</h3>
                            <p style="font-size: 20px;">med salat</p>
                            <p style="background-color: steelblue;">pris kommer</p>
                        </div>
                    </div>
                </div>
                """);

        MenuItem item = menu.items().getFirst();
        assertEquals("mandag uden dato", item.dateText());
        assertEquals("mandag", item.dayName());
        assertNull(item.date());
        assertNull(item.priceDkk());
        assertNull(item.imageUrl());
    }
}
