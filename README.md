# Campus News API

Et lille Java API bygget med Javalin, som scraper indhold fra
`https://cafs.dk` og udstiller det som JSON.

Projektet henter to typer data:

- Aktiviteter fra `https://cafs.dk/Home/Aktiviteter`
- Ugens menu fra `https://cafs.dk/Home/MenuOversigt`

Begge endpoints scraper CAFS live, men bruger en kort cache på 5 minutter, så
API'et ikke henter samme side igen for hvert eneste request.

## Teknologier

- Java 25
- Maven
- Javalin 7.2.2
- JSoup 1.23.2
- JUnit 5

## Projektstruktur

```text
src/main/java/app
├── Main.java
├── Activity.java
├── CafsActivityScraper.java
├── CachedActivityService.java
├── WeeklyMenu.java
├── MenuItem.java
├── CafsMenuScraper.java
└── CachedMenuService.java
```

`Main` starter Javalin-serveren på port `7070` og registrerer routes.

Scraper-klasserne henter HTML fra CAFS med JSoup og parser det relevante
indhold ud. Service-klasserne ligger mellem routes og scrapers og sørger for
cache og fallback, hvis CAFS midlertidigt ikke kan hentes.

## Endpoints

### `GET /health`

Returnerer en simpel status, som kan bruges til at se om serveren kører.

```json
{
  "status": "ok"
}
```

### `GET /activities`

Returnerer listen af aktiviteter fra CAFS.

Eksempel:

```json
[
  {
    "title": "AMU Kursus: §17",
    "startDateTime": "2026-09-07T08:00:00",
    "endDateTime": "2026-09-07T15:25:00",
    "location": "Minervavej 2, smedeværkstedet",
    "logoUrl": "https://cafs.dk/Graphics/Logo/logo_cabh.png"
  }
]
```

Felter:

- `title`: aktivitetens titel
- `startDateTime`: startdato og starttid i ISO-format
- `endDateTime`: sluttidspunkt i ISO-format, eller `null`
- `location`: sted uden prefixet `Sted:`
- `logoUrl`: absolut URL til institutionens logo

CAFS viser nogle aktiviteter med fuld slutdato og andre kun med sluttid. Hvis
slutværdien kun er et tidspunkt, bruger parseren startdatoen som dato for
`endDateTime`.

### `GET /menu`

Returnerer ugens menu.

Eksempel:

```json
{
  "weekNumber": 36,
  "year": 2026,
  "items": [
    {
      "dayName": "torsdag",
      "dateText": "torsdag 3. sep",
      "date": "2026-09-03",
      "title": "Carapulka",
      "description": "med ris",
      "priceText": "35 kr.",
      "priceDkk": 35,
      "imageUrl": "data:image/png;base64,..."
    }
  ]
}
```

Felter:

- `weekNumber`: ugenummer fra CAFS-overskriften
- `year`: årstal fra CAFS-overskriften
- `items`: menupunkter for ugen
- `dayName`: ugedag, fx `torsdag`
- `dateText`: den oprindelige datotekst fra CAFS
- `date`: dato i ISO-format, eller `null` hvis teksten ikke kan parses
- `title`: rettens navn
- `description`: rettens beskrivelse
- `priceText`: pris som tekst, fx `35 kr.`
- `priceDkk`: pris som tal, eller `null`
- `imageUrl`: værdien fra menuens `img src`

På den nuværende CAFS-side er menuens billeder typisk indlejret direkte som
store `data:image/png;base64,...` værdier. Det betyder, at `/menu` kan returnere
et stort JSON-svar.

## Sådan virker koden

### `Main`

`Main` opretter en cache-levetid på 5 minutter og bygger to services:

- `CachedActivityService`
- `CachedMenuService`

Derefter startes Javalin på port `7070`.

Routes registreres direkte på `config.routes`:

- `/health`
- `/activities`
- `/menu`

Hvis scraping fejler, returnerer `/activities` og `/menu` HTTP `502 Bad Gateway`
med en kort fejlbesked.

### Aktiviteter

`CafsActivityScraper` henter HTML-fragmentet fra `/Home/Aktiviteter`.

Parseren finder hver aktivitet i de gentagne HTML-blokke og udtrækker:

- titel
- startdato og tid
- evt. sluttidspunkt
- sted
- logo

Datoer på CAFS har formatet `dd-MM-yyyy HH:mm`. API'et returnerer dem som
ISO-lignende lokale dato-tid-strenge, fx `2026-09-07T08:00:00`.

### Ugens menu

`CafsMenuScraper` henter hele siden `/Home/MenuOversigt`.

Parseren finder først overskriften, fx:

```text
Menu uge 36 - 2026
```

Derefter læses hver menurække fra `.body-content > div.row`. Herfra udtrækkes
dag, dato, ret, beskrivelse, pris og billede.

Danske månedsforkortelser som `sep`, `okt` og `dec` mappes til månedsnumre, så
`torsdag 3. sep` kan blive til `2026-09-03`.

### Cache

`CachedActivityService` og `CachedMenuService` bruger samme princip:

1. Hvis der findes frisk cache, returneres cachen.
2. Hvis cachen er udløbet, forsøger servicen at scrape CAFS igen.
3. Hvis ny scraping lykkes, gemmes resultatet i cache.
4. Hvis ny scraping fejler, men der findes gammel cache, returneres den gamle cache.
5. Hvis scraping fejler og der ikke findes cache, kastes fejlen videre til routen.

Metoderne er `synchronized`, så to samtidige requests ikke opdaterer den samme
cache på samme tid.

## Kør projektet

Start serveren med Maven:

```bash
mvn exec:java
```

Serveren lytter derefter på:

```text
http://localhost:7070
```

Test endpoints i browseren eller med `curl`:

```bash
curl http://localhost:7070/health
curl http://localhost:7070/activities
curl http://localhost:7070/menu
```

## Kør tests

```bash
mvn test
```

Testene dækker blandt andet:

- parsing af aktivitetsdatoer
- parsing af aktiviteter fra HTML-fragment
- parsing af ugens menu
- danske tegn i menuindhold
- konvertering af menudatoer til ISO-format
- cache-genbrug og fallback ved netværksfejl

## Begrænsninger

API'et afhænger af HTML-strukturen på `cafs.dk`. Hvis CAFS ændrer markup,
selectors eller datoformater, skal scraperne sandsynligvis opdateres.

Der er ingen database i projektet. Cachen ligger kun i hukommelsen og nulstilles,
når serveren genstartes.
