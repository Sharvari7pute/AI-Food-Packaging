# Decisions & assumptions

Every choice made where the brief was silent or ambiguous. The rule used: pick the simplest option that keeps the demo working.

## Data

1. **materials.csv source.** The team's latest verified sheet (`SIH26236_Materials_Clean.csv`, shared on 25 Sep 2026) is used as `data-files/materials.csv` and copied unchanged to `backend/src/main/resources/data/materials.csv`. Glass and Tinplate now have OTR/WVTR = 0.
2. **Zero OTR/WVTR (glass, tinplate).** A zero value means a perfect barrier. In a layer stack, one perfect layer makes the whole stack 0. Shelf life against a perfect barrier is infinite and gets capped at `max-shelf-life-days`. Glass and Tinplate are `rigid`, so the engine never recommends them anyway.
3. **Missing extras.** A material with no row in `material_extras.csv` gets family = its own name (upper case) and CO₂e = 0, and a warning is logged at startup.
4. **Incomplete materials.** A material without OTR, WVTR, cost or density still shows in the library but is left out of the engine, with a warning.

## Engine

5. **Fat rule vs. moist foods (Paneer).** Section 4.2 says fat > 10 % → oxygen barrier HIGH, but the Section 9 test expects Paneer (fat 22 %) to be MEDIUM. So the fat rule is applied only when `aw ≤ keep-in-aw-threshold (0.9)`. Moist foods like paneer are spoiled by microbes long before the fat goes rancid. The decision trace says this in plain words. `o2_sensitive = high` always gives HIGH.
6. **O₂ tolerance level.** The tolerance is looked up by the derived oxygen-barrier level (HIGH → `high`, MEDIUM → `medium`), as the brief says ("HIGH uses high tolerance").
7. **Barrier margin.** "Margin" = the tighter (smaller) of `required/actual` for OTR and WVTR. Barrier score = 1.0 if margin ≤ 20, else 0.8 (overkill).
8. **Cost score.** Uses the cheapest *passing* candidate as the reference, clamped to at most 1.
9. **Single films that fail at every thickness** are evaluated at 100 µm (the max) so near misses can show the best shelf life they could reach.
10. **Avoid card.** Plain LDPE 50 µm, checked against all the hard filters. If it passes (e.g. atta, tomato), the lowest-scoring failed candidate is shown instead.
11. **Near misses.** Sorted by the shelf life they would reach (for MAP: closeness to the required OTR). All failed candidates are eligible, and each shows why it failed.
12. **MAP.** The formula compares the film's 23 °C OTR directly with the required OTR, as in the brief. Film temperature correction is not applied (conservative for chilled produce, because real film OTR drops in the cold, so more breathable films are needed). If `map_targets.csv` has no row for a food, a generic 5 % O₂ target is used (`engine.default-map-target-o2-pct`) and a note is shown. If no plain film breathes enough, the option is "LDPE (micro-perforated)" at 25 µm.
13. **Shelf life for produce.** The O₂/moisture shelf-life formulas don't apply to respiring produce (it is limited by ripening), so the commodity default shelf life is shown with limiting factor NONE.
14. **Extra constants.** Numbers that the brief used inline (fat 10 %, drivingFactor divisor 90, min driving factor 0.05, frozen −25 °C, strength 3, area formula, avoid LDPE 50 µm, perforated LDPE 25 µm, curve 1.5× / 60 points, eco scores) also live in `engine.*` in `application.yml`, so services contain no magic numbers.

## API

15. **Custom foods.** `POST /api/recommend` also accepts `customCommodity` instead of `commodityId`. This lets an AI-estimated food, after the user edits it, run through the engine without being saved to the database.
16. **Transport/priority/language** default to LOCAL / DEFAULT / en when omitted. Temperature and RH are required.
17. **Laminate library costs** (`GET /api/laminates`) are shown for a reference 100 g pack (area 0.06 m²).
18. **Explanation storage.** The latest explanation is stored on the recommendation so the PDF can include it (only when AI wrote it).

## Build / infra

19. **Spring Boot 3.5.x.** Spring Initializr now offers only Boot 4, but the brief requires Boot 3.x, so the pom pins `spring-boot-starter-parent 3.5.16`. springdoc 2.8.x is the matching line.
20. **OpenPDF 1.4.2** (classic `com.lowagie` API) was chosen for stability.
21. **.env loading.** `spring.config.import=optional:file:.env[.properties]` is used, so no extra dependency is needed. Run the backend from the `backend/` folder so it finds `.env`.
22. **Supabase details** were not provided while building, so the app was verified with the H2 test database. Put the real values in `backend/.env` (see `backend/.env.example`).
23. **Devanagari in the PDF.** OpenPDF can't shape Devanagari (vowel signs and conjuncts), and its AWT layout processor breaks positioning when mixed with other fonts. So the PDF shows food names in English only. A Hindi/Marathi AI explanation is still embedded with Noto Sans Devanagari but without shaping, so some vowel signs may look out of place. The web verify page renders Hindi/Marathi correctly.
