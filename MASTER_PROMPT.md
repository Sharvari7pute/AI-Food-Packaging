# MASTER PROMPT — PackSmart (SIH26236)

> **To Claude Code:** This file is the complete brief for the project. Read ALL of it before writing any code.
>
> **Before writing any code, ask me for these (Section 13):**
> 1. The GitHub repository URL (our whole team works in this ONE repo — do not create any other repo).
> 2. Supabase connection details (Session pooler host, user, password).
> 3. Gemini API key (optional — the app must work without it).
>
> Then build ALL phases in order (Section 12). After each phase: run the tests and the build, fix everything until green, commit, **push to the team repo**, and print a short summary before moving to the next phase. All features in this brief are required. If something is ambiguous, choose the simplest option that keeps the demo working and note it in `docs/DECISIONS.md`.

---

## 0. What we are building (read this first)

**Problem (SIH26236, Ministry of Food Processing Industries):** Small food businesses, farmers and startups pick packaging by guesswork. Wrong packaging → oxidation (rancid oil), moisture (soggy chips), microbial spoilage, short shelf life, food waste. Choosing correctly needs knowledge of barrier properties (OTR, WVTR), food properties (water activity, fat, respiration) and storage conditions.

**Product: PackSmart** — a web app where a user picks a food (or describes it in plain language), enters storage conditions, and gets:
- the top packaging options (single films AND multi-layer laminates) with thickness,
- barrier specs (OTR, WVTR), estimated shelf life and what limits it,
- MAP gas mix + breathable film for fresh fruits/vegetables,
- cost per 1000 packs, carbon footprint and recyclability,
- a plain-language explanation (English / Hindi / Marathi),
- a downloadable, QR-verified packaging spec sheet (PDF) to send to a packaging supplier.

**Golden rule (very important — judges will ask):** every packaging decision and every number comes from OUR deterministic engine (rules + physics formulas below). Gemini is used only to (a) turn a natural-language query into form fields, (b) explain engine results in plain language, (c) answer chatbot questions grounded in our database, (d) estimate properties of an unknown food, clearly labelled "AI-estimated — verify". Gemini NEVER picks materials and NEVER invents numbers. The whole app must work with no Gemini key (graceful fallbacks).

---

## 1. Tech stack

**Monorepo** with two apps:

```
packsmart/
├── MASTER_PROMPT.md
├── README.md
├── docs/
├── data-files/            (user-provided real data; see Section 3)
├── backend/               Spring Boot
└── frontend/              Next.js
```

**Backend** (`backend/`)
- Java 21, Spring Boot 3.x, Maven with Maven wrapper
- Spring Web, Spring Data JPA, Validation, Lombok, springdoc-openapi (Swagger at `/swagger-ui.html`)
- **Database: Supabase PostgreSQL from day one** — for local development AND production. Connection from env vars `DB_URL`, `DB_USER`, `DB_PASSWORD`, loaded locally from `backend/.env` (gitignored; use `spring-dotenv` or `spring.config.import=optional:file:.env[.properties]`) and on Render from environment variables. HikariCP `maximum-pool-size: 5` (Supabase free tier). `spring.jpa.hibernate.ddl-auto=update`.
- Automated tests only (`src/test`) use H2 in PostgreSQL mode, so tests never modify the shared Supabase data. The running app never uses H2.
- CSV parsing: Apache Commons CSV
- Gemini: Google Gen AI Java SDK (`com.google.genai:google-genai`). Key from env `GEMINI_API_KEY`, model from env `GEMINI_MODEL` (default `gemini-2.5-flash`). Key stays on the server only.
- PDF: OpenPDF (`com.github.librepdf:openpdf`). QR: ZXing (`com.google.zxing:core` + `javase`)
- Tests: JUnit 5, AssertJ, Spring Boot Test, MockMvc
- Base package `com.packsmart`

**Frontend** (`frontend/`)
- Next.js (App Router) + TypeScript + Tailwind CSS + shadcn/ui + Recharts + lucide-react icons
- API base URL from env `NEXT_PUBLIC_API_URL` (default `http://localhost:8080`)
- Light/dark mode, fully responsive (judges may open it on a phone)
- UI language toggle EN / हिंदी / मराठी for static labels (simple JSON dictionary); Gemini handles explanation language

---

## 2. Backend structure

```
backend/src/main/java/com/packsmart/
├── PackSmartApplication.java
├── config/        CorsConfig, EngineProperties, GeminiProperties, OpenApiConfig
├── controller/    CommodityController, MaterialController, LaminateController,
│                  RecommendController, SimulateController, AiController,
│                  ReportController, CityController, HealthController
├── dto/           request/response records
├── entity/        Commodity, Material, MaterialExtra, Laminate, MapTarget, City, Recommendation
├── repository/
├── seed/          CsvDataSeeder
├── service/
│   ├── engine/    RequirementService, BarrierCalculator, LaminateService,
│   │              ThicknessCalculator, PackEconomics, ScoringService,
│   │              ShelfLifeService, MapCalculator, RecommendationEngine
│   ├── ai/        GeminiClient, ExplainService, ChatService, QueryParserService,
│   │              FoodEstimatorService (all with non-AI fallbacks)
│   └── report/    PdfReportService, QrService
└── exception/     GlobalExceptionHandler
backend/src/main/resources/
├── application.yml          (Supabase via env / backend/.env)
└── data/  materials.csv, material_extras.csv, laminates.csv,
           commodities.csv, map_targets.csv, cities.csv
```

Engine services must be **pure** (no DB/network inside calculation methods) so they are easy to unit test. `RecommendationEngine` loads data from repositories and passes it in.

---

## 3. Data

All CSVs: header row, comma separated, UTF-8, empty cell = null. Parse with Apache Commons CSV (never `split(",")`; values contain commas inside quotes). Booleans are `yes`/`no`.

**Seeder:** `CsvDataSeeder` (ApplicationRunner). For each data table, if empty, load from `classpath:data/<file>.csv`. Log `Loaded N <table>`. A bad row is logged and skipped, never crashes the app. Material names are the join key between files — match them exactly (trim whitespace).
**Reseed (because the database is shared on Supabase):** if env `RESEED_ON_START=true`, delete and reload ONLY the data tables (materials, material_extras, laminates, commodities, map_targets, cities) from CSV — never touch `recommendations`. This is how the team loads new verified CSVs: replace the file, push, run once with `RESEED_ON_START=true`, then set it back to `false`.

**Real vs dummy data:** `data-files/materials.csv` is REAL (researched by our team) — copy it into `backend/src/main/resources/data/materials.csv` unchanged. All other files below are DUMMY/APPROXIMATE placeholders written into `resources/data/` exactly as given; the team will replace them later with verified data using the SAME columns. Never hardcode food or material values in code.

### 3.1 `materials.csv` (REAL — from `data-files/`)

Columns: `name,type,otr_25um,wvtr_25um,cost_per_kg_inr,density_g_cm3,min_temp_c,max_temp_c,recyclable,biodegradable,transparent,heat_sealable,strength_1to5,source_url,notes`

- OTR: cc/(m²·day·atm) at 23 °C, normalised to 25 µm. WVTR: g/(m²·day) at 38 °C / 90 % RH, normalised to 25 µm.
- `type` ∈ plastic, metal, paper, bio, coating, rigid. Type `rigid` (Glass, Tinplate) is reference-only: shown in the library and comparisons, never recommended as a flexible pack.
- `notes` containing "approx" or "VERIFY" → surface a small "approx data" badge in the UI for that material.

### 3.2 `material_extras.csv` (DUMMY)

Extra per-material data kept separate so the research sheet format stays unchanged.
`family` = polymer family used for mono-material recyclability. `co2e_kg_per_kg` = approx cradle-to-gate emission factor.

```csv
name,family,co2e_kg_per_kg,notes
LDPE,PE,1.9,approx
LLDPE,PE,1.9,approx
HDPE,PE,1.8,approx
CPP,PP,1.7,approx
BOPP,PP,1.9,approx
Metalized BOPP,PP,2.0,approx
PET,PET,2.7,approx
Metalized PET,PET,2.8,approx
Aluminium foil,AL,9.0,approx
Nylon (PA),PA,9.0,approx
EVOH,EVOH,6.0,approx
PVDC coated film,PET,3.0,approx (PVDC on PET base)
PLA,PLA,1.3,approx
Paper (coated),PAPER,1.1,approx
Glass,GLASS,0.9,approx
Tinplate,TIN,2.5,approx
```

### 3.3 `laminates.csv` (DUMMY structures)

`layers` = `Material:thickness_um` separated by `;`, outside → inside. Material names contain spaces and brackets, so split on `;` then on the LAST `:`.

```csv
name,layers,typical_use
PET/AL/PE,PET:12;Aluminium foil:9;LDPE:50,"High-barrier snacks, coffee, masala"
BOPP/Metalized PET/PE,BOPP:20;Metalized PET:12;LDPE:30,"Chips, namkeen"
PET/PE,PET:12;LDPE:50,"Medium-barrier dry foods"
Nylon/PE,Nylon (PA):25;LDPE:60,"Vacuum packs, paneer, meat"
PE/EVOH/PE,LDPE:40;EVOH:5;LDPE:40,"Recyclable-ready high oxygen barrier"
BOPP/CPP,BOPP:20;CPP:30,"Mono-PP recyclable pack"
Kraft/PE,Paper (coated):70;LDPE:25,"Paper-based atta, sugar, dal"
```

### 3.4 `commodities.csv` (DUMMY)

```csv
name,name_hi,category,moisture_pct,water_activity,critical_aw,fat_pct,o2_sensitive,light_sensitive,respiring,respiration_rate,default_shelf_life_days,source_url,notes
Chips,चिप्स,dry_snack,2,0.2,0.4,35,high,high,no,,90,dummy,approx
Namkeen,नमकीन,dry_snack,3,0.3,0.45,30,high,high,no,,120,dummy,approx
Biscuit,बिस्किट,bakery,4,0.3,0.5,20,medium,medium,no,,180,dummy,approx
Atta,आटा,staple,12,0.6,0.7,1.5,low,low,no,,90,dummy,approx
Masala powder,मसाला पाउडर,staple,9,0.5,0.6,5,medium,high,no,,365,dummy,approx
Coffee powder,कॉफी पाउडर,other,3,0.3,0.45,12,high,high,no,,365,dummy,approx
Paneer,पनीर,dairy,53,0.97,,22,medium,medium,no,,7,dummy,approx
Milk,दूध,dairy,87,0.99,,3.5,medium,high,no,,2,dummy,approx
Bread,ब्रेड,bakery,38,0.95,,4,low,low,no,,5,dummy,approx
Frozen matar,फ्रोज़न मटर,frozen,79,0.98,,0.4,low,low,no,,270,dummy,approx
Tamatar,टमाटर,fruit,94,0.98,,0.2,low,low,yes,15,7,dummy,approx
Seb,सेब,fruit,86,0.98,,0.2,low,low,yes,7,30,dummy,approx
Kela,केला,fruit,75,0.98,,0.3,low,low,yes,30,5,dummy,approx
Palak,पालक,vegetable,91,0.99,,0.4,low,low,yes,50,3,dummy,approx
```

`respiration_rate` = mL O₂/(kg·h) at ~20 °C.

### 3.5 `map_targets.csv` (DUMMY)

```csv
name,target_o2_min,target_o2_max,target_co2_min,target_co2_max,storage_temp_c,source_url
Tamatar,3,5,3,5,12,dummy
Seb,1,3,1,3,2,dummy
Kela,2,5,2,5,13,dummy
Palak,7,10,5,10,2,dummy
```

### 3.6 `cities.csv` (DUMMY — for climate-aware auto-fill)

```csv
name,state,summer_temp_c,summer_rh_pct,monsoon_temp_c,monsoon_rh_pct,winter_temp_c,winter_rh_pct
Nagpur,Maharashtra,38,40,30,80,22,50
Mumbai,Maharashtra,32,75,29,88,27,65
Pune,Maharashtra,34,45,26,82,22,50
Delhi,Delhi,36,45,31,75,16,65
Chennai,Tamil Nadu,34,70,31,78,26,72
Kolkata,West Bengal,33,78,30,86,21,65
Guwahati,Assam,30,82,29,88,19,75
Jaipur,Rajasthan,38,35,31,70,18,50
Shimla,Himachal Pradesh,22,65,19,85,8,60
```

---

## 4. Engine (the core IP)

All tunable constants in `EngineProperties` (`application.yml` → `engine.*`); no magic numbers in services. Every public engine method has a Javadoc with the formula it implements. Every decision adds a human-readable **reason** string (returned to the UI as a "decision trace").

```yaml
engine:
  reference-otr-temp-c: 23
  reference-wvtr-temp-c: 38
  reference-respiration-temp-c: 20
  q10: 2.0
  o2-partial-pressure-atm: 0.21
  o2-tolerance-ml-per-kg: { high: 1.0, medium: 10.0, low: 50.0 }
  allowed-moisture-gain-fraction: 0.02
  keep-out-aw-threshold: 0.7
  keep-in-aw-threshold: 0.9
  thickness-options-um: [12, 15, 20, 25, 30, 40, 50, 60, 75, 100]
  min-thickness-um: 20
  max-thickness-um: 100
  max-shelf-life-days: 730
  overkill-margin: 20
  score-weights:
    default:  { barrier: 0.40, cost: 0.25, eco: 0.20, strength: 0.15 }
    eco:      { barrier: 0.30, cost: 0.15, eco: 0.40, strength: 0.15 }
    budget:   { barrier: 0.30, cost: 0.45, eco: 0.10, strength: 0.15 }
```

### 4.1 Input — `RecommendRequest`

```json
{
  "commodityId": 1,
  "overrides": { "moisturePct": null, "waterActivity": null, "fatPct": null, "respirationRate": null },
  "packWeightG": 100,
  "packAreaM2": null,
  "shelfLifeDays": 90,
  "storageType": "AMBIENT",
  "storageTempC": 30,
  "relativeHumidityPct": 70,
  "transport": "LOCAL",
  "priority": "DEFAULT",
  "language": "en"
}
```
- `storageType` AMBIENT | CHILLED | FROZEN. `transport` LOCAL | LONG_DISTANCE. `priority` DEFAULT | ECO | BUDGET. `language` en | hi | mr.
- Non-null `overrides` replace commodity DB values for this request only.
- `packAreaM2` null → estimate `area = 0.02 + 0.0004 × packWeightG` (m²; flat pouch approximation).
- Validation: packWeightG 1–50000, shelfLifeDays 1–730, storageTempC −40…60, RH 0–100. 400 with field errors otherwise.

### 4.2 Step A — RequirementService → `Requirements`

- `o2Barrier` = HIGH if `fat_pct > 10` or `o2_sensitive == high`; MEDIUM if `o2_sensitive == medium`; else LOW.
- `moistureMode`: BREATHABLE if respiring; else KEEP_OUT if `aw < keepOutAwThreshold`; else KEEP_IN if `aw > keepInAwThreshold`; else MODERATE.
- `opaque` = `light_sensitive == high`.
- `frozen` = storageType FROZEN (materials need `min_temp_c ≤ −25`).
- `needsMap` = respiring. `needsStrength` = transport LONG_DISTANCE (strength ≥ 3).
- Reason examples: "Fat 35% > 10% → oxygen barrier HIGH", "aw 0.20 < 0.70 → keep moisture OUT", "Light-sensitive → opaque pack required".

### 4.3 Step B — BarrierCalculator

Temperature factor (Q10 rule): `tf(T, Tref) = q10 ^ ((T − Tref) / 10)`

Required OTR (non-respiring; skip if o2Barrier LOW):
```
O2_tolerance_mL = o2ToleranceMlPerKg[o2Sensitivity] × packWeightKg     (HIGH uses "high" tolerance)
OTR_required    = O2_tolerance_mL / (area × shelfLifeDays × 0.21 × tf(T, 23))
```
Required WVTR (KEEP_OUT or KEEP_IN only):
```
allowedWater_g = allowedMoistureGainFraction × packWeightG
drivingFactor  = |RH_outside − aw×100| / 90
WVTR_required  = allowedWater_g / (area × shelfLifeDays × drivingFactor × tf(T, 38))
```
If drivingFactor < 0.05 → no WVTR requirement.

### 4.4 Step C — LaminateService

```
OTR_layer(t) = OTR_25 × 25 / t          (same for WVTR)
1 / OTR_total = Σ 1 / OTR_layer         (layers in series)
```
Laminate properties: min_temp = max of layers; max_temp = min of layers; transparent = all layers transparent; heat_sealable = innermost layer; strength = max layer; biodegradable = all layers; **recyclable = all layers share one `family` AND that family's material is recyclable** (mono-material); family "mixed" otherwise. Also expose `evaluate(List<Layer>)` for the custom Laminate Builder.

### 4.5 Step D — ThicknessCalculator

Single films: smallest thickness in `thickness-options-um` within min/max satisfying `OTR(t) ≤ OTR_required` and `WVTR(t) ≤ WVTR_required` (ignore checks with no requirement). None → fails ("even at 100 µm OTR is X > required Y"). Laminates use fixed thicknesses.

### 4.6 Step E — PackEconomics (cost + carbon)

Per layer: `grams_per_pack = area_m2 × thickness_um × density_g_cm3` (this identity gives grams directly).
```
cost_per_pack_inr     = Σ grams × cost_per_kg_inr / 1000
cost_per_1000_inr     = cost_per_pack_inr × 1000
co2e_kg_per_1000      = Σ grams × co2e_kg_per_kg          (kg CO2e per 1000 packs)
```

### 4.7 Step F — ScoringService

Candidates = all non-rigid single films (at their Step-D thickness) + all laminates.

Hard filters (failed candidates keep a reason):
- barrier fails; temperature: `min_temp_c > storageTempC`, FROZEN and `min_temp_c > −25`, or `max_temp_c < storageTempC`
- opaque required but transparent
- needsStrength and strength < 3
- not heat sealable (single film) / innermost layer not heat sealable (laminate)

Scores 0–1: **barrier** = 1.0 if margin (required/actual, tighter of OTR/WVTR) ≤ overkillMargin else 0.8; **cost** = min cost_per_pack / candidate cost_per_pack; **eco** = biodegradable 1.0, recyclable mono-material 0.8, contains AL 0.1, else 0.3; **strength** = strength/5. Weights by `priority`.

Output: top 3 `options`; `avoid` = plain LDPE 50 µm if it failed (else lowest failed), with reason; **`nearMisses`** = if fewer than 3 pass, up to 3 best failed candidates with the shelf life they WOULD achieve ("best achievable: 45 days") — this is useful for the user, never hide it.

### 4.8 Step G — ShelfLifeService

```
shelfLife_O2  = O2_tolerance_mL / (OTR_actual × area × 0.21 × tf(T, 23))
shelfLife_H2O = allowedWater_g  / (WVTR_actual × area × drivingFactor × tf(T, 38))
estimated     = min(applicable) capped at maxShelfLifeDays; limitingFactor OXYGEN | MOISTURE | NONE
```
If nothing applies → commodity default, limitingFactor NONE.
**Shelf-life curve:** for each option return points for day 0…ceil(1.5 × estimated) (max 60 points): `o2UsedPct = 100 × day / shelfLife_O2`, `moistureUsedPct = 100 × day / shelfLife_H2O`. The UI draws these against a 100 % "spoilage line".

### 4.9 Step H — MapCalculator (respiring produce)

```
RR_total_mL_per_day = respiration_rate × packWeightKg × 24 × tf(T, 20)
targetO2            = (target_o2_min + target_o2_max) / 2
OTR_required_map    = RR_total_mL_per_day / (area × (0.21 − targetO2/100))
```
Among heat-sealable non-rigid single films × allowed thicknesses pick the one with OTR ≥ OTR_required_map and closest to it (too much OTR lets in excess oxygen). If none → LDPE 25 µm + micro-perforations (`perforationNeeded = true`). Return target O₂/CO₂ ranges and storage temperature from `map_targets`; missing row → `mapTarget = null` + note. For respiring produce skip Steps B/D oxygen logic; rank by closeness to OTR_required_map.

### 4.10 RecommendationEngine
Orchestrates A → H, computes economics for each option, saves a `Recommendation` (request JSON, response JSON, createdAt, public `shareId` UUID), returns `RecommendResponse`. `simulate()` runs the same pipeline without saving (for What-if).

---

## 5. AI layer (Gemini) — with fallbacks

`GeminiClient` wraps the SDK. If `GEMINI_API_KEY` is missing or a call fails/times out (8 s), every AI service falls back and the response has `"aiUsed": false`.

1. **QueryParserService** — `POST /api/ai/parse` `{ "query": "mujhe 500 gram paneer 10 din fridge mein rakhna hai" }` → a `RecommendRequest` draft + `missingFields`. Prompt Gemini to return JSON only (use response MIME type `application/json` with a schema), mapping the food to one of our commodity names (send the list). Fallback: keyword matcher (commodity name / Hindi name, numbers followed by g/kg/din/days, words fridge/chilled/frozen).
2. **ExplainService** — `POST /api/ai/explain` `{ recommendationId, language }` → 3–5 sentence explanation in en/hi/mr for a small business owner. Prompt includes ONLY engine output (commodity, requirements + reasons, top option, thickness, shelf life, limiting factor, avoid reason). Instruction: "Use only these numbers. Do not add new numbers or materials." Fallback: template sentence built from the reasons.
3. **ChatService ("Pack-Bot")** — `POST /api/ai/chat` `{ messages: [...], language }`. System prompt: packaging assistant for Indian food MSMEs; answer only from the provided context (materials table, commodities table, laminates, and — if given — the current recommendation); if unknown, say so. Fallback: "Pack-Bot needs internet/AI key; here are the materials..." plus a simple FAQ match.
4. **FoodEstimatorService** — `POST /api/ai/estimate-food` `{ name }` → estimated commodity properties (same fields as commodities.csv) with `"aiEstimated": true`. UI shows a yellow "AI-estimated — verify" banner and lets the user edit before running the engine. Not saved to DB. Fallback: 404 with a message to pick the closest food.

Log every AI prompt/response at DEBUG. Never send API keys to the frontend.

---

## 6. Reports & QR

- `GET /api/report/{shareId}/pdf` → **Packaging Spec Sheet** (A4, OpenPDF): header with PackSmart name + date; food + inputs; decision trace (reasons); recommended structure table (layers, thickness, OTR, WVTR vs required); estimated shelf life + limiting factor; MAP section if produce; cost per 1000 packs; CO₂e per 1000 packs; recyclability; "avoid" material; explanation text (if AI used); disclaimer "Prototype estimates from literature data; validate with lab shelf-life tests"; QR code linking to `{FRONTEND_URL}/report/{shareId}` so anyone can verify the spec online.
- `GET /api/report/{shareId}/qr` → PNG QR.
- `GET /api/recommendations/share/{shareId}` → public read-only JSON for the verify page.

---

## 7. REST API (all under `/api`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | `{status:"ok", aiEnabled: bool}` |
| GET | `/commodities` | list (id, name, name_hi, category) |
| GET | `/commodities/{id}` | full commodity (form auto-fill) |
| GET | `/materials` | all materials + extras + `approx` flag |
| GET | `/laminates` | laminates with computed OTR/WVTR/properties |
| POST | `/laminates/evaluate` | custom layers → OTR, WVTR, cost/1000, CO₂e, recyclable (Laminate Builder) |
| GET | `/cities` | cities with seasonal temp/RH |
| POST | `/recommend` | run engine + save → `RecommendResponse` |
| POST | `/simulate` | run engine, no save (What-if) |
| GET | `/recommendations` | history (latest first, paged) |
| GET | `/recommendations/{id}` | one result |
| GET | `/recommendations/share/{shareId}` | public result |
| POST | `/ai/parse`, `/ai/explain`, `/ai/chat`, `/ai/estimate-food` | Section 5 |
| GET | `/report/{shareId}/pdf`, `/report/{shareId}/qr` | Section 6 |

`RecommendResponse` shape:
```json
{
  "id": 12, "shareId": "uuid", "commodity": "Chips",
  "inputs": { "...": "echo of effective inputs incl. estimated area" },
  "requirements": { "o2Barrier": "HIGH", "moistureMode": "KEEP_OUT", "opaque": true, "needsMap": false, "reasons": ["..."] },
  "requiredOtr": 0.05, "requiredWvtr": 1.2,
  "options": [{
    "rank": 1, "name": "PET/AL/PE", "kind": "LAMINATE",
    "layers": [{"material":"PET","thicknessUm":12},{"material":"Aluminium foil","thicknessUm":9},{"material":"LDPE","thicknessUm":50}],
    "totalThicknessUm": 71, "otr": 0.03, "wvtr": 0.03,
    "scores": {"barrier":1.0,"cost":0.9,"eco":0.1,"strength":0.8,"total":0.71},
    "estimatedShelfLifeDays": 160, "limitingFactor": "OXYGEN",
    "costPer1000Inr": 950, "co2eKgPer1000": 4.2,
    "recyclable": false, "biodegradable": false, "family": "mixed",
    "approxData": true,
    "curve": [{"day":0,"o2UsedPct":0,"moistureUsedPct":0}]
  }],
  "nearMisses": [], "avoid": {"name":"LDPE","thicknessUm":50,"reason":"..."},
  "map": null,
  "disclaimer": "Prototype estimates based on literature values. Validate with lab shelf-life testing."
}
```
(Numbers illustrative.) Errors via `GlobalExceptionHandler`: `{ "error": "...", "details": [...] }`, 400 validation / 404 not found. CORS: `http://localhost:3000` + env `FRONTEND_URL`.

---

## 8. Frontend pages & features

Design: clean, modern, trustworthy (think fintech dashboard). Green + deep-blue palette, rounded cards, subtle shadows, good spacing, skeleton loaders, toasts for errors. Every AI-generated text shows a small "✨ AI" tag; every approximate datum shows an "approx" tag.

1. **Home `/`** — hero ("Right pack. Longer shelf life. Less waste."), big CTA, 3-step "how it works", 3 feature cards, short impact stats section (placeholder numbers clearly marked as examples).
2. **Recommend `/recommend`** — two ways in:
   - **"Describe your product"** box (natural language, Hindi/Hinglish OK) → `/ai/parse` → pre-fills the wizard and highlights missing fields.
   - **3-step wizard**: (1) Food: searchable dropdown (English + Hindi names); selecting auto-fills moisture/aw/fat/respiration (editable; "Food not in list?" → `/ai/estimate-food` with AI-estimated banner). (2) Storage: pack weight, shelf life, storage type, **city + season picker that auto-fills temp & RH** (from `/cities`), manual override, transport. (3) Priority: Default / Eco / Budget segmented control; explanation language.
3. **Results `/results/[id]`** — the wow page:
   - Summary strip: food, conditions, required OTR/WVTR.
   - **Decision trace** ("Why these requirements?") — reasons as a checklist.
   - **Top 3 option cards**: rank badge, layer stack visual (coloured horizontal bars proportional to thickness), OTR/WVTR vs required (mini bars), shelf life + limiting factor, ₹/1000 packs, CO₂e/1000 packs, recyclable/biodegradable badges, approx badge.
   - **Shelf-life curve chart** (Recharts): O₂ used % and moisture used % over days vs 100 % spoilage line, option selector.
   - **What-if simulator**: sliders for temperature, RH, shelf life, pack weight → debounced `/simulate` → cards and chart update live ("At 40 °C shelf life drops from 160 to 90 days").
   - **Avoid card** (red) and **near misses** (if fewer than 3 options).
   - **MAP panel** for produce: target gas mix as a donut (O₂ / CO₂ / N₂), breathable film, perforation note, storage temperature.
   - **AI explanation** box with language switch EN/हिं/मरा (calls `/ai/explain`).
   - Buttons: Download spec sheet PDF, Show QR, Compare, Ask Pack-Bot about this result, Share link.
4. **Compare `/compare`** — pick 2–3 materials/laminates/options side by side (table + radar chart: barrier O₂, barrier moisture, cost, eco, strength).
5. **Laminate Builder `/builder`** — add/remove/reorder layers (material + thickness), live OTR/WVTR/cost/CO₂e/recyclable via `/laminates/evaluate`; optionally "test against a food" to see if it meets requirements.
6. **Material Library `/materials`** — cards with properties, approx badges, source link, family, filter chips (recyclable, biodegradable, transparent, high barrier).
7. **Pack-Bot `/chat`** (also a floating button on every page) — chat UI with suggested questions; can receive the current result as context.
8. **History `/history`** — past recommendations with re-open and PDF download.
9. **Verify `/report/[shareId]`** — public read-only spec view (what the QR opens), "Verified by PackSmart" header.
10. **How it works `/methodology`** — for judges: the formulas (render nicely), data sources table (from materials `source_url`), assumptions & limitations, "Gemini explains, engine decides" diagram.

---

## 9. Tests (must pass)

Backend unit tests per engine service + MockMvc integration tests (H2 test database seeded from the same CSVs):

| Case | Expected |
|---|---|
| Q10 | `tf(33, 23) == 2.0` |
| Thickness | OTR_25 = 70 at 50 µm → 35 |
| Laminate | layers with OTR 100 and 100 → 50 |
| Economics | 0.06 m² × 50 µm × 0.92 g/cm³ = 2.76 g |
| Chips, 100 g, 90 d, AMBIENT 30 °C, 70 % RH | o2Barrier HIGH, KEEP_OUT, opaque; rank-1 option contains Aluminium foil; LDPE in `avoid`; every option's OTR ≤ requiredOtr |
| Atta, 1000 g, 90 d, 30 °C, 70 % RH | KEEP_OUT; no OTR requirement; every option WVTR ≤ requiredWvtr |
| Paneer, 200 g, 7 d, CHILLED 4 °C, 70 % RH | KEEP_IN; o2Barrier MEDIUM; every option heat-sealable and OTR ≤ requiredOtr |
| Frozen matar, 500 g, 180 d, FROZEN −18 °C | every option has min_temp ≤ −25 |
| Tamatar, 500 g, 7 d, CHILLED 12 °C, 90 % RH | needsMap; map target 3–5 % O₂; recommended film OTR ≥ OTR_required_map (or perforationNeeded) |
| Validation | shelfLifeDays = 0 → 400 |
| AI fallback | with no GEMINI_API_KEY, `/ai/parse` "500 g paneer 10 din fridge" → commodity Paneer, 500 g, 10 days, CHILLED |

Frontend: `npm run build` and `npm run lint` must pass. Add a Playwright smoke test: open home → recommend Chips via wizard → results page shows at least one option card.

---

## 10. Docs to generate

- `README.md` — what it is, screenshots placeholder, how to run backend + frontend locally (with Supabase), env vars, how to replace dummy CSVs (+ `RESEED_ON_START`), how to deploy on Render.
- `docs/ENGINE_EXPLAINED.md` — every formula in simple language + a full **worked example for Chips** with the actual numbers the code computes (the team uses this to answer judges).
- `docs/API.md` — endpoints with sample requests/responses.
- `docs/DECISIONS.md` — any assumption you made.
- `.env.example` files for backend and frontend.

---

## 11. Coding rules

- Clean layered code: controller → service → repository; DTOs as Java records.
- No magic numbers; constants in `EngineProperties`.
- No hardcoded food/material data in code — only in CSVs.
- Every calculation returns reasons so the UI can explain "why".
- Frontend: typed API client in `frontend/lib/api.ts`; reusable components; no inline secrets.
- Run tests after every phase; keep them green. Commit after each phase with a clear message.

---

## 12. Build phases (do them in order)

1. **Setup** — connect to the team repo (Section 13), monorepo, backend skeleton, Supabase connection via `backend/.env`, `.env.example` files, `.gitignore`, all CSVs (copy real `data-files/materials.csv`; write dummy files from Section 3), entities, seeder + reseed flag, health endpoint, backend Dockerfile + `render.yaml` (Section 14). ✅ `./mvnw spring-boot:run` connects to Supabase and logs row counts for all 6 files; tables visible in the Supabase Table Editor. Push.
2. **Engine A–D** — Requirement, Barrier, Laminate, Thickness + unit tests.
3. **Engine E–H** — Economics, Scoring, ShelfLife (+ curve), MAP, RecommendationEngine + all Section 9 backend tests.
4. **API** — all non-AI endpoints, validation, errors, CORS, Swagger, MockMvc tests.
5. **AI layer** — Gemini client + 4 services with fallbacks + fallback test.
6. **Reports** — PDF spec sheet + QR + share endpoints.
7. **Frontend core** — Next.js setup, design system, Home, Recommend wizard (incl. city auto-fill + NL box), Results page (cards, decision trace, avoid, MAP panel).
8. **Frontend wow** — shelf-life chart, What-if simulator, Compare, Laminate Builder, Material Library, Pack-Bot, History, Verify page, Methodology page, language toggle, dark mode.
9. **Polish** — loading/empty/error states, mobile check, Playwright smoke test, docs (Section 10), final full test + build run.

At the end, print: how to run everything, the Render deployment steps (Section 14), the list of features built, and anything that needs the team's attention (e.g. data still dummy, missing API key).

---

## 13. Git & repository workflow (ONE shared team repo)

- The team uses ONE GitHub repo for everything (backend + frontend + docs + data). **Never create a new repo, never create a separate repo for frontend or backend.**
- At the start, ask me for the repo URL. Then:
  - If the current folder is not a git repo: `git init`, `git remote add origin <url>`, `git fetch origin`, and if the remote already has commits, `git pull origin main --allow-unrelated-histories` (keep teammates' files; resolve conflicts by keeping both where sensible).
  - If the folder is already a clone of that repo: just `git pull`.
- Put everything inside this repo using the structure in Section 1 (`backend/`, `frontend/`, `docs/`, `data-files/`). Do not delete or overwrite files other team members added unless I say so.
- Work on branch `main` unless I say otherwise. Before every push: `git pull --rebase origin main`, then run tests, then push.
- Commit + push after every phase (and after any fix I ask for), with clear messages, e.g. `feat(engine): barrier + laminate calculations`.
- **Never commit secrets.** `.env` files are gitignored; commit only `.env.example` with placeholder values. If git asks me for credentials, tell me exactly what to do (GitHub login / personal access token).

## 14. Deployment on Render (both apps from the same repo)

Create `render.yaml` at the repo root (Render Blueprint) with two web services:

1. **packsmart-backend** — `runtime: docker`, `rootDir: backend`, `dockerfilePath: ./Dockerfile`, health check `/api/health`, env vars (values set in the Render dashboard, `sync: false`): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `FRONTEND_URL`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `RESEED_ON_START`.
   - Dockerfile: multi-stage (Maven + Java 21 build → slim Java 21 runtime), listens on env `PORT` (default 8080), JVM flags suitable for 512 MB RAM (`-XX:MaxRAMPercentage=75`).
2. **packsmart-frontend** — `runtime: node`, `rootDir: frontend`, build `npm ci && npm run build`, start `npm start` (bind to `PORT`), env `NEXT_PUBLIC_API_URL` = backend URL.

CORS must allow `FRONTEND_URL`. Also add a short "Deploy on Render" section to README: connect the GitHub repo in Render → New → Blueprint → fill env vars → deploy; then set backend `FRONTEND_URL` and frontend `NEXT_PUBLIC_API_URL` to each other's URLs and redeploy. Mention that free Render services sleep after ~15 min idle (first request ~50 s), and suggest an UptimeRobot ping on `/api/health`.
