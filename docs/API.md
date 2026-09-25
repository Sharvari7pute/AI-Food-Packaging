# PackSmart REST API

Base URL: `http://localhost:8080` (local) or your Render backend URL. Everything is under `/api`. Interactive docs are at **`/swagger-ui.html`**.

Errors always look like `{ "error": "message", "details": ["field: problem", ...] }`: **400** for invalid input and **404** when something doesn't exist.

## Health & catalog

| Method | Path | Returns |
|---|---|---|
| GET | `/api/health` | `{"status":"ok","aiEnabled":false}` |
| GET | `/api/commodities` | `[{"id":1,"name":"Chips","nameHi":"चिप्स","category":"dry_snack","respiring":false}, …]` |
| GET | `/api/commodities/{id}` | full food row (moisture, aw, fat, sensitivities, respiration, `approx`) |
| GET | `/api/materials` | all materials + `family`, `co2eKgPerKg`, `approx`, `rigid`, `ecoScore`, `referenceCostPer1000Inr` (25 µm, 100 g pack) |
| GET | `/api/laminates` | laminates with computed `otr`, `wvtr`, temps, `recyclable`, `family`, cost/CO₂e for a 100 g reference pack |
| GET | `/api/cities` | cities with summer/monsoon/winter temperature and RH |

## Recommend

### `POST /api/recommend` — run the engine and save the result

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

- `storageType`: AMBIENT | CHILLED | FROZEN · `transport`: LOCAL | LONG_DISTANCE · `priority`: DEFAULT | ECO | BUDGET · `language`: en | hi | mr
- Instead of `commodityId` you can send `customCommodity` (same fields as commodities.csv: `name`, `waterActivity`, `fatPct`, `o2Sensitive`, `lightSensitive`, `respiring`, `respirationRate`, …), e.g. an AI-estimated food the user checked.
- Validation: packWeightG 1–50000, shelfLifeDays 1–730, storageTempC −40…60, relativeHumidityPct 0–100.

Response (shortened, real numbers):

```json
{
  "id": 1, "shareId": "69c76331-89e7-4efd-a8cc-f6cd3cad741b", "commodity": "Chips", "commodityHi": "चिप्स",
  "inputs": { "packWeightG": 100, "packAreaM2": 0.06, "areaEstimated": true, "shelfLifeDays": 90, "storageTempC": 30, "...": "..." },
  "requirements": { "o2Barrier": "HIGH", "moistureMode": "KEEP_OUT", "opaque": true, "needsMap": false,
                    "reasons": ["Fat 35% > 10% → oxygen barrier HIGH (fats turn rancid with oxygen)", "..."] },
  "requiredOtr": 0.05428, "requiredWvtr": 1.161,
  "options": [{
    "rank": 1, "name": "PET/AL/PE", "kind": "LAMINATE",
    "layers": [{"material":"PET","thicknessUm":12},{"material":"Aluminium foil","thicknessUm":9},{"material":"LDPE","thicknessUm":50}],
    "totalThicknessUm": 71, "otr": 0.01389, "wvtr": 0.02768,
    "scores": {"barrier":1.0,"cost":1.0,"eco":0.1,"strength":1.0,"total":0.82},
    "estimatedShelfLifeDays": 351, "limitingFactor": "OXYGEN",
    "costPer1000Inr": 1137.42, "co2eKgPer1000": 21.068, "recyclable": false, "family": "mixed", "approxData": true,
    "curve": [{"day":0,"o2UsedPct":0,"moistureUsedPct":0}, "..."]
  }],
  "nearMisses": [{"name":"Aluminium foil","bestAchievableShelfLifeDays":730,"reasons":["Not heat sealable on its own → cannot close the pack"]}],
  "avoid": {"name":"LDPE","thicknessUm":50,"reason":"OTR 3937 > required 0.05428 → too much oxygen gets in; ..."},
  "map": null,
  "disclaimer": "Prototype estimates based on literature values. Validate with lab shelf-life testing."
}
```

For respiring produce, `map` is filled in: `targetO2Min/Max`, `targetCo2Min/Max`, `gasMix`, `requiredOtr`, `film`, `filmThicknessUm`, `filmOtr`, `perforationNeeded`, `storageTempC`.

### `POST /api/simulate`
Same body and response as `/recommend`, but nothing is saved (`id` and `shareId` are null). Used by the What-if simulator.

### History & sharing

| Method | Path | Notes |
|---|---|---|
| GET | `/api/recommendations?page=0&size=20` | `{content:[{id, shareId, commodity, topOption, estimatedShelfLifeDays, createdAt}], page, size, totalElements, totalPages}`, newest first |
| GET | `/api/recommendations/{id}` | a saved result |
| GET | `/api/recommendations/share/{shareId}` | public read-only result (verify page) |

## Laminate Builder — `POST /api/laminates/evaluate`

```json
{
  "layers": [{"material": "BOPP", "thicknessUm": 20}, {"material": "CPP", "thicknessUm": 30}],
  "packWeightG": 100,
  "test": {"commodityId": 1, "shelfLifeDays": 90, "storageType": "AMBIENT", "storageTempC": 30, "relativeHumidityPct": 70}
}
```

Response: `otr`, `wvtr`, `totalThicknessUm`, `gramsPerPack`, `costPer1000Inr`, `co2eKgPer1000`, `recyclable`, `family`, `heatSealable`, `transparent`, temps, `approx`, plus `test: {commodity, passes, requiredOtr, requiredWvtr, estimatedShelfLifeDays, limitingFactor, failReasons[]}` when a test was requested.

## AI helpers (all work without a Gemini key; `aiUsed` tells you which path answered)

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/api/ai/parse` | `{"query":"mujhe 500 gram paneer 10 din fridge mein rakhna hai"}` | `{draft: RecommendRequest, commodityName:"Paneer", missingFields:["storageTempC","relativeHumidityPct"], aiUsed, note}` |
| POST | `/api/ai/explain` | `{"recommendationId":1,"language":"hi"}` (or `shareId`) | `{text, language, aiUsed}` |
| POST | `/api/ai/chat` | `{"messages":[{"role":"user","content":"What is EVOH?"}],"language":"en","recommendationId":1}` | `{reply, aiUsed}` |
| POST | `/api/ai/estimate-food` | `{"name":"Peanut chikki"}` | `{food: CustomCommodity, existingCommodityId, aiEstimated:true, aiUsed, note}`; **404** without AI |

## Reports

| Method | Path | Returns |
|---|---|---|
| GET | `/api/report/{shareId}/pdf` | A4 Packaging Spec Sheet (PDF) with a QR code to `{FRONTEND_URL}/report/{shareId}` |
| GET | `/api/report/{shareId}/qr` | PNG QR code |
