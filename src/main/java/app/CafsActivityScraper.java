package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CafsActivityScraper {
    static final String BASE_URL = "https://cafs.dk";
    static final String ACTIVITIES_URL = BASE_URL + "/Home/Aktiviteter";

    private static final DateTimeFormatter SOURCE_DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter SOURCE_TIME =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter API_DATE_TIME =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<Activity> fetchActivities() throws IOException {
        Document document = Jsoup.connect(ACTIVITIES_URL)
                .userAgent("Mozilla/5.0 (compatible; CampusNewsBot/1.0)")
                .timeout(10_000)
                .get();

        return parseActivities(document.body().html());
    }

    List<Activity> parseActivities(String html) {
        Document document = Jsoup.parseBodyFragment(html, BASE_URL);
        List<Activity> activities = new ArrayList<>();

        for (Element block : document.body().children()) {
            parseActivityBlock(block).ifPresent(activities::add);
        }

        return activities;
    }

    Optional<Activity> parseActivityBlock(Element block) {
        Element titleElement = block.selectFirst("div[style*=font-size: 18px]");
        Element dateElement = findDateElement(block);
        Element locationElement = findLocationElement(block);

        if (titleElement == null || dateElement == null) {
            return Optional.empty();
        }

        String title = cleanText(titleElement.text());
        DateRange dateRange = parseDateRange(dateElement.text());
        String location = parseLocation(locationElement);
        String logoUrl = parseLogoUrl(block);

        return Optional.of(new Activity(
                title,
                formatDateTime(dateRange.start()),
                formatDateTime(dateRange.end()),
                location,
                logoUrl
        ));
    }

    DateRange parseDateRange(String text) {
        String[] parts = cleanText(text).split("\\s+[–-]\\s*", 2);
        LocalDateTime start = parseStartDateTime(parts[0]);
        LocalDateTime end = null;

        if (parts.length == 2 && !parts[1].isBlank()) {
            try {
                end = parseEndDateTime(parts[1], start.toLocalDate());
            } catch (DateTimeParseException ignored) {
                end = null;
            }
        }

        return new DateRange(start, end);
    }

    private Element findDateElement(Element block) {
        for (Element element : block.select("div[style*=color: #666666]")) {
            String text = element.text();
            if (text.contains("–") || text.matches(".*\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}.*")) {
                return element;
            }
        }
        return null;
    }

    private Element findLocationElement(Element block) {
        for (Element element : block.select("div[style*=color: #666666]")) {
            if (cleanText(element.text()).startsWith("Sted:")) {
                return element;
            }
        }
        return null;
    }

    private String parseLocation(Element locationElement) {
        if (locationElement == null) {
            return null;
        }

        return cleanText(locationElement.text()).replaceFirst("^Sted:\\s*", "");
    }

    private String parseLogoUrl(Element block) {
        Element image = block.selectFirst("img[src]");
        if (image == null) {
            return null;
        }

        return image.absUrl("src");
    }

    private LocalDateTime parseStartDateTime(String text) {
        return LocalDateTime.parse(cleanText(text), SOURCE_DATE_TIME);
    }

    private LocalDateTime parseEndDateTime(String text, LocalDate startDate) {
        String cleaned = cleanText(text);

        try {
            return LocalDateTime.parse(cleaned, SOURCE_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            LocalTime endTime = LocalTime.parse(cleaned, SOURCE_TIME);
            return LocalDateTime.of(startDate, endTime);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : API_DATE_TIME.format(dateTime);
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
    }

    record DateRange(LocalDateTime start, LocalDateTime end) {
    }
}
