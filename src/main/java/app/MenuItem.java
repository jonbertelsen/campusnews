package app;

public record MenuItem(
        String dayName,
        String dateText,
        String date,
        String title,
        String description,
        String priceText,
        Integer priceDkk,
        String imageUrl
) {
}
