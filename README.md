# PackSmart — right pack, longer shelf life, less waste

**SIH26236 · Ministry of Food Processing Industries**

Small food businesses, farmers and startups often pick packaging by guesswork. Wrong packaging leads to rancid oil, soggy chips, spoiled paneer and food waste. PackSmart is a web app where you pick a food (or describe it in Hindi/English), enter storage conditions, and get:

- the top packaging options (single films **and** multi-layer laminates) with thickness
- barrier specs (OTR, WVTR) against what the food needs, the estimated shelf life and what limits it
- a MAP gas mix and breathable film for fresh fruits and vegetables
- ₹ per 1000 packs, CO₂e per 1000 packs and recyclability
- a plain-language explanation in English / हिंदी / मराठी
- a downloadable, **QR-verified packaging spec sheet (PDF)** to send to a supplier

> **Golden rule:** every packaging decision and every number comes from our deterministic engine (rules + physics). Gemini only fills forms from text, explains results, answers chat from our database and estimates unknown foods (labelled "AI-estimated — verify"). If Gemini is down or out of quota, Groq is used as a backup with the same prompts and rules. The whole app works **without** any AI key.

Screenshots: _add screenshots of Home, Results and the PDF here._

## Repository layout

```
├── backend/        Spring Boot 3.5 (Java 21) - engine, REST API, AI layer, PDF/QR
├── frontend/       Next.js 16 + TypeScript + Tailwind + shadcn/ui + Recharts
├── data-files/     Real team data (materials.csv)
├── docs/           ENGINE_EXPLAINED.md · API.md · DECISIONS.md
└── render.yaml     Render Blueprint (deploys both apps)
```

## Features

- **Recommend wizard**: describe your product in plain language, or use 3 steps (food → storage → priority); city + season auto-fills temperature and humidity
- **Results**: decision trace, top-3 option cards with layer stacks, barrier bars, shelf-life curve, What-if sliders, MAP donut, avoid card, near misses, AI explanation, PDF / QR / share
- **Compare** (radar chart + table) · **Laminate Builder** (with "test against a food") · **Material Library** · **Pack-Bot** chat (floating on every page) · **History** · public **Verify** page (opened by the QR) · **Methodology** (formulas, data sources, limitations)
- Light/dark mode, mobile-friendly, EN/हिंदी/मराठी labels

## Run locally

Prerequisites: Java 21, Node 20.9+, a Supabase project (free tier works).

### 1. Backend (Supabase PostgreSQL)

In Supabase, go to **Project Settings → Database → Connection string → Session pooler** and note the host, port, user and password.

```bash
cd backend
cp .env.example .env        # gitignored - put your real values here
# DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
# DB_USER=postgres.<project-ref>
# DB_PASSWORD=<your password>
# GEMINI_API_KEY=            (optional)
./mvnw spring-boot:run       # run from backend/ so .env is found
```

On first start the backend creates the tables and logs `Loaded N materials`, … for all 6 CSV files. You can then see the tables in the Supabase Table Editor. API: http://localhost:8080/api/health · Swagger: http://localhost:8080/swagger-ui.html

### 2. Frontend

```bash
cd frontend
cp .env.example .env.local   # NEXT_PUBLIC_API_URL=http://localhost:8080
npm install
npm run dev                  # http://localhost:3000
```

### One public URL (optional)

To serve the API through the frontend's own address (handy for a single tunnel or proxy, and no CORS needed), build and start the frontend with `NEXT_PUBLIC_API_URL=/` and `API_PROXY_TARGET=http://localhost:8080`. Next.js then forwards `/api/*` to the backend.

### Tests

```bash
cd backend && ./mvnw test                               # engine unit tests + MockMvc API tests (H2, never touches Supabase)
cd frontend && npm run lint && npm run build && npm run test:e2e   # Playwright smoke test
```

## Environment variables

| Where | Variable | Purpose |
|---|---|---|
| backend | `DB_URL`, `DB_USER`, `DB_PASSWORD` | Supabase session pooler (JDBC URL) |
| backend | `FRONTEND_URL` | CORS origin + link inside QR codes (e.g. `https://packsmart-frontend.onrender.com`) |
| backend | `GEMINI_API_KEY` | optional; without it every AI feature falls back |
| backend | `GEMINI_MODEL` | default `gemini-3.8-flash` (`gemini-2.5-flash` is retired for new keys) |
| backend | `GROQ_API_KEY` | optional backup AI; used only when every Gemini model fails or hits its quota |
| backend | `GROQ_MODEL` | default `llama-3.3-70b-versatile` (see console.groq.com/docs/models) |
| backend | `GEMINI_THINKING_LEVEL` | default `low` (keeps Gemini 3 answers under the 8 s timeout) |
| backend | `RESEED_ON_START` | `true` once to reload the data tables from the CSVs |
| backend | `PORT` | set automatically by Render (default 8080) |
| frontend | `NEXT_PUBLIC_API_URL` | backend base URL |

Secrets never go into git: only the `.env.example` files are committed.

## Replacing dummy data with verified data

`materials.csv`, `commodities.csv` (25 foods, incl. pH, recommended storage temperature and main deterioration factor) and `map_targets.csv` are **real** team data. `material_extras.csv` (CO₂ factors), `laminates.csv` and `cities.csv` are still **placeholders**.

`data-files/commodity_validation.csv` is **not** loaded into the app. It is used only by `CommodityValidationTest`, which checks the engine's output against literature packaging directions (**22 of 25 match**, see [docs/VALIDATION_REPORT.md](docs/VALIDATION_REPORT.md)).

1. Edit the file in `backend/src/main/resources/data/`, keeping the **same columns** (header row, comma-separated, UTF-8, `yes`/`no` booleans, empty cell = unknown). Material names are the join key between files, so spell them exactly the same. Laminate layers look like `PET:12;Aluminium foil:9;LDPE:50` (outside → inside). Put `approx` or `VERIFY` in a material's notes to show an "approx" badge.
2. Keep the CSVs in `data-files/` in sync with the copies in `backend/src/main/resources/data/`.
3. Run `./mvnw test`, then commit and push.
4. Because the database is shared, run the backend **once** with `RESEED_ON_START=true` (locally in `.env` or on Render). This deletes and reloads only the data tables (never `recommendations`). Then set it back to `false`.

Bad rows are logged and skipped, never crashing the app. Startup also warns about names that don't match between files.

## Deploy on Render

`render.yaml` deploys both services from this one repo.

1. Push to GitHub. In Render, go to **New → Blueprint** and pick this repo. Render reads `render.yaml` and creates `packsmart-backend` (Docker) and `packsmart-frontend` (Node).
2. Fill in the env vars Render asks for:
   - backend: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `GEMINI_API_KEY` (optional), `GEMINI_MODEL` = `gemini-3.8-flash`, `GROQ_API_KEY` (optional backup), `GROQ_MODEL` = `llama-3.3-70b-versatile`, `RESEED_ON_START` = `false`, `FRONTEND_URL` (any placeholder for now)
   - frontend: `NEXT_PUBLIC_API_URL` (placeholder for now)
3. Deploy. Then set backend `FRONTEND_URL` = the frontend URL and frontend `NEXT_PUBLIC_API_URL` = the backend URL, and **redeploy both**. `NEXT_PUBLIC_*` is baked in at build time, so the frontend needs a rebuild.
4. Check `https://<backend>/api/health`.

Free Render services sleep after ~15 minutes idle, so the first request then takes ~50 s. Before a demo, open the site a minute early, or add a free [UptimeRobot](https://uptimerobot.com) monitor that pings `https://<backend>/api/health` every 5 minutes.

## Docs

- [docs/ENGINE_EXPLAINED.md](docs/ENGINE_EXPLAINED.md): every formula in simple words, plus a full Chips worked example
- [docs/API.md](docs/API.md): endpoints with sample requests and responses
- [docs/DECISIONS.md](docs/DECISIONS.md): assumptions made during the build
- [docs/VALIDATION_REPORT.md](docs/VALIDATION_REPORT.md): engine vs literature packaging directions for all 25 foods

_Prototype estimates from literature data. Validate with lab shelf-life tests._
