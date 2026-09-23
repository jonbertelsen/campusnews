package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Connection;
import java.util.Map;

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

    private static final String PROXY_HOST = "193.181.218.140";
    private static final int PROXY_PORT = 3128;

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/153.0.0.0 Safari/537.36";

    private static final DateTimeFormatter SOURCE_DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter SOURCE_TIME =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter API_DATE_TIME =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<Activity> fetchActivities() throws IOException {
        /*
         * Første request opretter en ASP.NET-session.
         * CAFS returnerer muligvis 403, men sender alligevel
         * ASP.NET_SessionId som cookie.
         */
        Connection.Response sessionResponse = Jsoup.connect(BASE_URL + "/")
                .proxy(PROXY_HOST, PROXY_PORT)
                .userAgent(USER_AGENT)
                .timeout(10_000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .method(Connection.Method.GET)
                .execute();

        Map<String, String> cookies = sessionResponse.cookies();

        if (!cookies.containsKey("ASP.NET_SessionId")) {
            throw new IOException(
                    "CAFS oprettede ikke en ASP.NET-session. HTTP-status: "
                            + sessionResponse.statusCode()
            );
        }

        /*
         * Andet request genbruger session-cookien og sender forsiden
         * som Referer. Det svarer til det fungerende curl-forløb.
         */
        Connection.Response activitiesResponse = Jsoup.connect(ACTIVITIES_URL)
                .proxy(PROXY_HOST, PROXY_PORT)
                .userAgent(USER_AGENT)
                .referrer(BASE_URL + "/")
                .cookies(cookies)
                .timeout(10_000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .method(Connection.Method.GET)
                .execute();

        if (activitiesResponse.statusCode() != 200) {
            throw new IOException(
                    "Kunne ikke hente aktiviteter fra CAFS. HTTP-status: "
                            + activitiesResponse.statusCode()
            );
        }

        Document document = activitiesResponse.parse();

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
