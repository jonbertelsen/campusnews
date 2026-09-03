package app;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.time.Duration;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Duration cacheTimeToLive = Duration.ofMinutes(5);
        createApp(
                new CachedActivityService(new CafsActivityScraper(), cacheTimeToLive),
                new CachedMenuService(new CafsMenuScraper(), cacheTimeToLive)
        ).start(7070);
    }

    static Javalin createApp(CachedActivityService activityService, CachedMenuService menuService) {
        return Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.json(Map.of("status", "ok")));
            config.routes.get("/activities", ctx -> {
                try {
                    ctx.json(activityService.getActivities());
                } catch (Exception exception) {
                    ctx.status(HttpStatus.BAD_GATEWAY)
                            .json(Map.of(
                                    "error", "Could not fetch activities from cafs.dk",
                                    "message", exception.toString()
                            ));
                }
            });
            config.routes.get("/menu", ctx -> {
                try {
                    ctx.json(menuService.getMenu());
                } catch (Exception exception) {
                    ctx.status(HttpStatus.BAD_GATEWAY)
                            .json(Map.of(
                                    "error", "Could not fetch menu from cafs.dk",
                                    "message", exception.toString()
                            ));
                }
            });
        });
    }
}
