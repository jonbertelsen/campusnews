package app;

import java.util.List;

public record WeeklyMenu(
        Integer weekNumber,
        Integer year,
        List<MenuItem> items
) {
}
