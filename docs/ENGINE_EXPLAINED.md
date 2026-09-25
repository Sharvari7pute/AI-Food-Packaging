# How the PackSmart engine works (in simple words)

Use this document to answer judges' questions. Every number below comes from the code (`backend/src/main/java/com/packsmart/service/engine/`) running on our current data files.

**Golden rule:** the engine (rules + physics) decides every material and every number. Gemini only turns text into form fields, explains results in plain language, answers chat questions from our database and estimates unknown foods (always labelled "AI-estimated — verify").

All constants (Q10, thresholds, weights, …) live in `backend/src/main/resources/application.yml` under `engine:`. None are hard-coded in the services.

---

## Units used everywhere

| Quantity | Unit | Measured at |
|---|---|---|
| OTR (oxygen transmission rate) | cc O₂ / (m²·day·atm) | 23 °C, normalised to 25 µm |
| WVTR (water vapour transmission rate) | g water / (m²·day) | 38 °C / 90 % RH, normalised to 25 µm |
| aw (water activity) | 0–1 | — |
| Respiration rate | mL O₂ / (kg·h) | ~20 °C |

Lower OTR or WVTR means a better barrier.

---

## Step A — What does the food need? (`RequirementService`)

| Rule | Result |
|---|---|
| fat > 10 % (only if aw ≤ 0.9) **or** O₂ sensitivity = high | oxygen barrier **HIGH** |
| O₂ sensitivity = medium | oxygen barrier **MEDIUM** |
| otherwise | oxygen barrier **LOW** (no OTR limit) |
| fresh produce (respiring) | moisture mode **BREATHABLE** → MAP (Step H) |
| aw < 0.70 | **KEEP_OUT** (dry food must not absorb water) |
| aw > 0.90 | **KEEP_IN** (moist food must not dry out) |
| otherwise | **MODERATE** (no WVTR limit) |
| light sensitivity = high | pack must be **opaque** |
| storage FROZEN | every layer must work at ≤ −25 °C |
| transport LONG_DISTANCE | strength ≥ 3/5 |

*Why is the fat rule limited to aw ≤ 0.9?* Moist foods like paneer (fat 14.8 %, aw 0.973) are spoiled by microbes long before their fat goes rancid. See `docs/DECISIONS.md` #5.

The food data also gives each food's **main deterioration factor** and **recommended storage temperature** (e.g. paneer: microbial spoilage, 3–5 °C). These are shown on the results page and the PDF, with a warning when the chosen temperature is outside the range. They inform the user but don't change the barrier maths.

## Step B — How good must the barrier be? (`BarrierCalculator`)

**Temperature (Q10 rule).** Gas and water move faster when it's hot. Every 10 °C doubles the rate (Q10 = 2):

```
tf(T, Tref) = Q10 ^ ((T − Tref) / 10)
```

**Pack area** (if not given), a flat pouch estimate: `area = 0.02 + 0.0004 × packWeightG` m².

**Required OTR:** the food can absorb only a small amount of oxygen before it goes off (the tolerance: high = 1, medium = 10, low = 50 mL O₂ per kg food).

```
O2_tolerance_mL = tolerance[level] × packWeightKg
OTR_required    = O2_tolerance_mL / (area × shelfLifeDays × 0.21 × tf(T, 23))
```

(0.21 atm is the oxygen partial pressure of air; inside the pack we assume ~0.)

**Required WVTR:** the food may gain (or lose) at most 2 % of its weight in water.

```
allowedWater_g = 0.02 × packWeightG
drivingFactor  = |RH_outside − aw × 100| / 90
WVTR_required  = allowedWater_g / (area × shelfLifeDays × drivingFactor × tf(T, 38))
```

If `drivingFactor < 0.05` (the air humidity roughly equals the food's aw), there's no WVTR limit.

## Step C — Laminates (`LaminateService`)

The film's value is scaled from the 25 µm reference: `OTR(t) = OTR_25 × 25 / t` (same for WVTR). Layers act like resistors in series:

```
1 / OTR_total = Σ 1 / OTR_layer
```

Laminate properties: min temp = the highest layer min; max temp = the lowest layer max; transparent only if all layers are; heat sealable = the inner layer; strength = the strongest layer; biodegradable only if all layers are; **recyclable only if every layer is from the same polymer family and recyclable** (mono-material).

## Step D — Thickness of single films (`ThicknessCalculator`)

The engine tries 20, 25, 30, 40, 50, 60, 75 and 100 µm and picks the thinnest that meets both limits. If even 100 µm fails, the film is rejected with a reason like "even at 100 µm OTR is 0.25 > required 0.054".

## Step E — Cost and carbon (`PackEconomics`)

```
grams_per_pack   = area_m2 × thickness_um × density_g_cm3     (the units give grams directly)
cost_per_1000    = Σ grams × cost_per_kg / 1000 × 1000
co2e_kg_per_1000 = Σ grams × co2e_kg_per_kg
```

## Step F — Filter and score (`ScoringService`)

A pack is **rejected** if it fails the barrier, is not usable at the storage temperature (or not at −25 °C for frozen), is transparent when the food needs opaque, is too weak for long distance, or its inner layer can't be heat-sealed.

The remaining packs get four scores from 0 to 1:

| Score | Rule |
|---|---|
| barrier | 1.0 if the safety margin (required ÷ actual, tighter of OTR/WVTR) ≤ 20, else 0.8 (over-engineered) |
| cost | cheapest passing pack's cost ÷ this pack's cost |
| eco | biodegradable 1.0 · recyclable mono-material 0.8 · contains aluminium 0.1 · other 0.3 |
| strength | strength ÷ 5 |

Weights by priority: **Balanced** 0.40/0.25/0.20/0.15 · **Eco** 0.30/0.15/0.40/0.15 · **Budget** 0.30/0.45/0.10/0.15 (barrier/cost/eco/strength). The top 3 are shown. **Avoid** = plain LDPE 50 µm if it fails (otherwise the worst failed pack). If fewer than 3 pass, **near misses** show the best failed packs and the shelf life they *would* reach.

## Step G — Shelf life (`ShelfLifeService`)

The same physics run backwards:

```
shelfLife_O2  = O2_tolerance_mL / (OTR × area × 0.21 × tf(T, 23))
shelfLife_H2O = allowedWater_g  / (WVTR × area × drivingFactor × tf(T, 38))
estimated     = min(applicable), capped at 730 days → limiting factor OXYGEN or MOISTURE
```

If neither applies (e.g. fresh produce), the food's typical shelf life is shown with limiting factor NONE. The **curve** plots `100 × day / shelfLife` for oxygen and moisture; the pack "fails" when a line crosses 100 %.

## Step H — Fresh produce and MAP (`MapCalculator`)

Fruits and vegetables keep breathing. The film must let in just enough oxygen to hold the target O₂ level:

```
RR_total_mL_per_day = respiration_rate × packWeightKg × 24 × tf(T, 20)
targetO2            = (target_o2_min + target_o2_max) / 2
OTR_required_map    = RR_total_mL_per_day / (area × (0.21 − targetO2 / 100))
```

Among heat-sealable films and thicknesses, the one with OTR ≥ the requirement and **closest** to it wins (too much OTR lets in extra oxygen). If none breathes enough → LDPE 25 µm with micro-perforations.

*Example (Tomato, 500 g, 12 °C):* RR = 17.5 × 0.5 × 24 × 0.574 = 120.6 mL/day → OTR_required_map = 120.6 / (0.22 × 0.17) = **3225** → **LDPE 60 µm** (OTR 3281). Target gas mix 4 % O₂, 4 % CO₂, 92 % N₂ (UC Davis: 3–5 % O₂, 3–5 % CO₂).

---

## Full worked example — Chips

**Input:** Chips (fat 34 %, aw 0.20, O₂ sensitivity high, light sensitivity high), 100 g pack, 90 days, ambient 30 °C, 70 % RH, local transport, balanced priority.

### A. Requirements
- Fat 34 % > 10 % and O₂ sensitivity high → **oxygen barrier HIGH**
- aw 0.20 < 0.70 → **KEEP_OUT** moisture
- Light-sensitive → **opaque**

### B. Barrier limits
- area = 0.02 + 0.0004 × 100 = **0.06 m²**
- tf(30, 23) = 2^0.7 = **1.6245**; tf(30, 38) = 2^−0.8 = **0.5743**
- O₂ tolerance = 1.0 mL/kg × 0.1 kg = **0.1 mL**
- OTR_required = 0.1 / (0.06 × 90 × 0.21 × 1.6245) = **0.05428** cc/m²·day·atm
- allowed water = 0.02 × 100 = **2 g**; drivingFactor = |70 − 20| / 90 = **0.5556**
- WVTR_required = 2 / (0.06 × 90 × 0.5556 × 0.5743) = **1.161** g/m²·day

### C. The winning laminate, PET 12 / Aluminium foil 9 / LDPE 50 µm

| Layer | OTR at thickness | WVTR at thickness |
|---|---|---|
| PET 12 µm | 63 × 25/12 = 131.25 | 18.3 × 25/12 = 38.13 |
| Aluminium foil 9 µm | 0.005 × 25/9 = 0.01389 | 0.01 × 25/9 = 0.02778 |
| LDPE 50 µm | 7874 × 25/50 = 3937 | 19.8 × 25/50 = 9.9 |
| **Whole structure (series)** | **0.01389** ≤ 0.05428 ✓ | **0.02768** ≤ 1.161 ✓ |

Opaque ✓ (foil), inner layer LDPE heat-sealable ✓, usable −50 to 80 °C ✓.

### E. Cost and carbon (area 0.06 m²)

| Layer | grams/pack | ₹/pack | CO₂e kg per 1000 packs |
|---|---|---|---|
| PET | 0.06 × 12 × 1.39 = 1.001 | 1.001 × 150/1000 = 0.150 | 1.001 × 2.7 = 2.70 |
| Aluminium foil | 0.06 × 9 × 2.7 = 1.458 | 1.458 × 450/1000 = 0.656 | 1.458 × 9.0 = 13.12 |
| LDPE | 0.06 × 50 × 0.92 = 2.760 | 2.760 × 120/1000 = 0.331 | 2.760 × 1.9 = 5.24 |
| **Total** | **5.219 g** | **₹1.137 → ₹1137 per 1000 packs** | **21.07 kg** |

### F. Score
- margin = min(0.05428/0.01389, 1.161/0.02768) = min(3.9, 41.9) = 3.9 ≤ 20 → barrier **1.0**
- cost = 1.0 (it's the only passing pack) · eco = **0.1** (contains aluminium) · strength = 5/5 = **1.0**
- total = 0.40×1.0 + 0.25×1.0 + 0.20×0.1 + 0.15×1.0 = **0.82**

### G. Shelf life
- shelfLife_O2 = 0.1 / (0.01389 × 0.06 × 0.21 × 1.6245) = **351.8 days**
- shelfLife_H2O = 2 / (0.02768 × 0.06 × 0.5556 × 0.5743) = **3774 days**
- estimated = **351 days, limited by OXYGEN** (the 90 days wanted are met ~4× over)

### Avoid and near misses
- **Avoid LDPE 50 µm:** OTR 3937 > 0.05428 (about 72,500× too high), WVTR 9.9 > 1.161, and transparent.
- Only one pack passed, so near misses are shown: Aluminium foil 20 µm alone (730 days, but can't be heat-sealed), Metalized PET 100 µm (19 days, OTR 0.25 too high), PE/EVOH/PE (6 days, and transparent).

---

## Validation against literature

`CommodityValidationTest` runs the engine for all 25 foods with default inputs and compares the derived requirements with the packaging direction in `commodity_validation.csv` (literature). **22 of 25 match.** See [VALIDATION_REPORT.md](VALIDATION_REPORT.md). The validation file is never fed into the engine.

## Likely judge questions

- **"Why not just trust the AI?"** LLMs make up numbers. Our engine is deterministic and auditable: the same input always gives the same answer, with every reason shown. Gemini only translates and explains.
- **"Where does the data come from?"** Material OTR/WVTR values were researched by our team (sources on the Methodology page and in the PDF). Rows marked approx/VERIFY are still being checked. Food properties (IFCT 2017, USDA FDC, FDA) and MAP targets (UC Davis) are now sourced too, with notes on every approximate value. Laminates, CO₂ factors and city climate are still placeholders.
- **"How accurate is the shelf life?"** It's a first estimate from literature data. It ignores seals, pinholes and microbial growth, so it must be confirmed with lab shelf-life tests (this is printed on every spec sheet).
- **"What happens offline / without an AI key?"** Everything still works. The parser uses keyword matching, explanations use templates, and Pack-Bot answers from database lookups and an FAQ.
