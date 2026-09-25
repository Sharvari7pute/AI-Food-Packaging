# Decisions & assumptions

Every choice made where the brief was silent or ambiguous. The rule used: pick the simplest option that keeps the demo working.

## Data

1. **materials.csv source.** The team's latest verified sheet (`SIH26236_Materials_Clean.csv`, shared on 25 Sep 2026) is used as `data-files/materials.csv` and copied unchanged to `backend/src/main/resources/data/materials.csv`. Glass and Tinplate now have OTR/WVTR = 0.
2. **Zero OTR/WVTR (glass, tinplate).** A zero value means a perfect barrier. In a layer stack, one perfect layer makes the whole stack 0. Shelf life against a perfect barrier is infinite and gets capped at `max-shelf-life-days`. Glass and Tinplate are `rigid`, so the engine never recommends them anyway.
3. **Missing extras.** A material with no row in `material_extras.csv` gets family = its own name (upper case) and CO₂e = 0, and a warning is logged at startup.
4. **Incomplete materials.** A material without OTR, WVTR, cost or density still shows in the library but is left out of the engine, with a warning.

## Engine

5. **Fat rule vs. moist foods (Paneer).** Section 4.2 says fat > 10 % → oxygen barrier HIGH, but the Section 9 test expects Paneer (fat 22 % in the dummy data, 14.78 % in the real data) to be MEDIUM. So the fat rule is applied only when `aw ≤ keep-in-aw-threshold (0.9)`. Moist foods like paneer are spoiled by microbes long before the fat goes rancid. The decision trace says this in plain words. `o2_sensitive = high` always gives HIGH.
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
22. **Supabase.** The team's Supabase project (Mumbai session pooler) is configured in the gitignored `backend/.env`. Tables were created and seeded once with `RESEED_ON_START=true`, then it was set back to `false`.
23. **Devanagari in the PDF.** OpenPDF can't shape Devanagari (vowel signs and conjuncts), and its AWT layout processor breaks positioning when mixed with other fonts. So the PDF shows food names in English only. A Hindi/Marathi AI explanation is still embedded with Noto Sans Devanagari but without shaping, so some vowel signs may look out of place. The web verify page renders Hindi/Marathi correctly.

## Real food data (25 Sep 2026)

24. **commodities.csv and map_targets.csv are now real** team data (IFCT 2017, USDA FDC, FDA, UC Davis). Food names are now English (Tomato, Apple, Frozen Peas, …) with Hindi in `name_hi`. The new columns `ph`, `storage_temp_min_c`, `storage_temp_max_c` and `main_deterioration_factor` are stored, returned by `/api/commodities/{id}` and echoed in results (`inputs.recommendedStorageTempMinC/MaxC`, `inputs.mainDeteriorationFactor`). They are shown on the results page, in the wizard and in the PDF. They don't change the barrier maths.
25. **Storage pre-fill.** Picking a food in the wizard pre-selects the storage type and temperature from its recommended range: frozen if max ≤ −10 °C, chilled if max ≤ 15 °C, else ambient, where the city/season temperature is kept. The user can change it. The results page warns when the chosen temperature is outside the recommended range.
26. **Section 9 test updates for real data.** "Tamatar" → Tomato and "Frozen matar" → Frozen Peas. Atta is now medium O₂-sensitive (hydrolytic/oxidative rancidity), so it gets an OTR limit; the test now checks KEEP_OUT, MEDIUM, and every option within both the WVTR and OTR limits (instead of "no OTR requirement").
27. **Validation file.** `commodity_validation.csv` lives only in `data-files/` and `backend/src/test/resources/validation/`. It is never seeded or read by the engine. `CommodityValidationTest` maps each literature direction to engine requirements (table in `docs/VALIDATION_REPORT.md`). The test fails if matches drop below 22 of 25. Default inputs: 100 g pack, the food's default shelf life, the middle of its recommended storage range, RH 65 % ambient / 90 % cold. The 3 mismatches are real model limits: carrot (literature "moisture retention"; engine treats all respiring produce as MAP), coffee powder (data says medium O₂ sensitivity; literature says high barrier) and chicken (MAP trays; engine only models MAP for respiring produce). Packaging form (jar, tray, vacuum, N₂ flush) is not modelled.

## Gemini

28. **Model.** `gemini-2.5-flash` returns 404 for new keys ("no longer available … use gemini-3.8-flash"), so the default is now `gemini-3.8-flash`. Gemini 3 models get `thinkingLevel=low` (`GEMINI_THINKING_LEVEL`) so answers fit the 8 s timeout. The old `thinkingBudget=0` is only sent to 2.5 Flash.
29. **No SDK retries.** The free-tier key allows 5 requests/minute on this model. On a 429 (quota) or 503 (overload) the SDK used to retry until our 8 s timeout. Retries are now off, so the app falls back immediately.
30. **Explanations are cached** on the recommendation per language. Re-opening a result or the verify page doesn't spend Gemini quota.
31. **Parser without response schema.** JSON mode with a response schema was consistently slower on Gemini 3. The parser asks for JSON mode and lists the keys in the prompt, and every field is still validated (known food name, number ranges, enum values) before use. The food estimator keeps the schema.
32. **Model fallback chain.** The free tier allows only **20 requests per day per model** (and 5 per minute). Quotas are per model, so the client tries `GEMINI_MODEL` (gemini-3.8-flash), then `GEMINI_FALLBACK_MODELS` (default `gemini-3.1-flash-lite,gemini-3.5-flash`), all inside the same 8 s budget. After that, the non-AI fallback answers. For a demo day, enable billing on the Google AI project (pay-as-you-go removes these limits).
33. **AI parse + keyword merge.** Any field the AI leaves empty (e.g. a food it didn't map) is filled from the keyword matcher. The AI answer is matched on the English name, the part before a bracket, or the Hindi name.
34. **Groq as backup AI.** Order: Gemini main model, then Gemini fallback models, then **Groq** (`GROQ_API_KEY`, `GROQ_MODEL`, default `openai/gpt-oss-120b`), then the non-AI templates. Groq gets exactly the same prompts (engine numbers only, "don't invent numbers") through its OpenAI-compatible `/chat/completions` endpoint, with JSON mode for form-filling and food estimation. It uses the JDK HTTP client, so no extra dependency. It has its own 8 s timeout, so a worst case (Gemini times out, then Groq) can take about 16 s before templates. `/api/health` reports `aiEnabled=true` if either provider has a key. Groq's key stays on the server.
35. **Groq model.** `llama-3.3-70b-versatile` is no longer offered on the team's Groq account, so the default `GROQ_MODEL` is `openai/gpt-oss-120b` (tested: correct JSON for parsing in ~1 s, Hindi explanations, chat). `qwen/qwen3.8-27b` also works and is a bit faster.
36. **Prompt hardening (found while testing Groq).** The food-estimator prompt now lists its JSON keys, because providers without response schemas need them in the prompt. Pack-Bot must not choose a pack or shelf life itself: it quotes the current recommendation or sends the user to the recommender, in plain text. Explanations must quote the engine's *estimated* shelf life, not the wanted one.
