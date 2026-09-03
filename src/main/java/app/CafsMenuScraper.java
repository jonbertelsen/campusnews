package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CafsMenuScraper {
    static final String BASE_URL = "https://cafs.dk";
    static final String MENU_URL = BASE_URL + "/Home/MenuOversigt";

    private static final Pattern WEEK_HEADING_PATTERN = Pattern.compile("Menu uge\\s+(\\d+)\\s+-\\s+(\\d+)");
    private static final Pattern DATE_TEXT_PATTERN = Pattern.compile("([\\p{L}æøåÆØÅ]+)\\s+(\\d{1,2})\\.\\s+([\\p{L}æøåÆØÅ]+)\\.?");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+)");
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1),
            Map.entry("feb", 2),
            Map.entry("mar", 3),
            Map.entry("apr", 4),
            Map.entry("maj", 5),
            Map.entry("jun", 6),
            Map.entry("jul", 7),
            Map.entry("aug", 8),
            Map.entry("sep", 9),
            Map.entry("okt", 10),
            Map.entry("nov", 11),
            Map.entry("dec", 12)
    );

    public WeeklyMenu fetchMenu() throws IOException {
        Document document = Jsoup.connect(MENU_URL)
                .userAgent("Mozilla/5.0 (compatible; CampusNewsBot/1.0)")
                .timeout(10_000)
                .get();

        return parseMenu(document.html());
    }

    WeeklyMenu parseMenu(String html) {
        Document document = Jsoup.parse(html, BASE_URL);
        WeekHeading heading = parseWeekHeading(document);
        List<MenuItem> items = new ArrayList<>();

        for (Element row : document.select(".body-content > div.row")) {
            parseMenuItem(row, heading.year()).ifPresent(items::add);
        }

        return new WeeklyMenu(heading.weekNumber(), heading.year(), List.copyOf(items));
    }

    private java.util.Optional<MenuItem> parseMenuItem(Element row, Integer year) {
        Element dateElement = row.selectFirst(".col-md-3 h4");
        Element titleElement = row.selectFirst(".col-md-9 h3");

        if (dateElement == null || titleElement == null) {
            return java.util.Optional.empty();
        }

        String dateText = cleanText(dateElement.text());
        String dayName = parseDayName(dateText);
        String date = parseIsoDate(dateText, year);
        String title = cleanText(titleElement.text());
        String description = parseDescription(row);
        String priceText = parsePriceText(row);
        Integer priceDkk = parsePriceDkk(priceText);
        String imageUrl = parseImageUrl(row);

        return java.util.Optional.of(new MenuItem(
                dayName,
                dateText,
                date,
                title,
                description,
                priceText,
                priceDkk,
                imageUrl
        ));
    }

    private WeekHeading parseWeekHeading(Document document) {
        for (Element heading : document.select("h2")) {
            Matcher matcher = WEEK_HEADING_PATTERN.matcher(cleanText(heading.text()));
            if (matcher.find()) {
                return new WeekHeading(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2))
                );
            }
        }

        return new WeekHeading(null, null);
    }

    private String parseDayName(String dateText) {
        String[] parts = cleanText(dateText).split("\\s+", 2);
        return parts.length == 0 ? null : parts[0].toLowerCase(Locale.forLanguageTag("da-DK"));
    }

    private String parseIsoDate(String dateText, Integer year) {
        if (year == null) {
            return null;
        }

        Matcher matcher = DATE_TEXT_PATTERN.matcher(cleanText(dateText));
        if (!matcher.matches()) {
            return null;
        }

        Integer month = MONTHS.get(matcher.group(3).toLowerCase(Locale.forLanguageTag("da-DK")));
        if (month == null) {
            return null;
        }

        int day = Integer.parseInt(matcher.group(2));
        return API_DATE.format(LocalDate.of(year, month, day));
    }

    private String parseDescription(Element row) {
        Element description = row.selectFirst("div[style*=float: left] p[style*=font-size: 20px]");
        return description == null ? null : cleanText(description.text());
    }

    private String parsePriceText(Element row) {
        Element price = row.selectFirst("p[style*=background-color: steelblue]");
        return price == null ? null : cleanText(price.text());
    }

    private Integer parsePriceDkk(String priceText) {
        if (priceText == null) {
            return null;
        }

        Matcher matcher = PRICE_PATTERN.matcher(priceText);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String parseImageUrl(Element row) {
        Element image = row.selectFirst("img[src]");
        if (image == null) {
            return null;
        }

        String src = image.attr("src");
        if (src.startsWith("/")) {
            return BASE_URL + src;
        }
        return src;
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
    }

    record WeekHeading(Integer weekNumber, Integer year) {
    }
}
