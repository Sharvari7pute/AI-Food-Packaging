// Types mirroring the backend DTOs (backend/src/main/java/com/packsmart/dto).

export type StorageType = "AMBIENT" | "CHILLED" | "FROZEN"
export type Transport = "LOCAL" | "LONG_DISTANCE"
export type Priority = "DEFAULT" | "ECO" | "BUDGET"
export type Lang = "en" | "hi" | "mr"
export type Level = "high" | "medium" | "low"

export interface Overrides {
  moisturePct?: number | null
  waterActivity?: number | null
  fatPct?: number | null
  respirationRate?: number | null
}

export interface CustomCommodity {
  name: string
  nameHi?: string | null
  category?: string | null
  moisturePct?: number | null
  waterActivity: number
  criticalAw?: number | null
  fatPct: number
  o2Sensitive?: Level | null
  lightSensitive?: Level | null
  respiring?: boolean | null
  respirationRate?: number | null
  defaultShelfLifeDays?: number | null
  aiEstimated?: boolean | null
  ph?: number | null
  storageTempMinC?: number | null
  storageTempMaxC?: number | null
  mainDeteriorationFactor?: string | null
}

export interface RecommendRequest {
  commodityId?: number | null
  customCommodity?: CustomCommodity | null
  overrides?: Overrides | null
  packWeightG: number | null
  packAreaM2?: number | null
  shelfLifeDays: number | null
  storageType: StorageType | null
  storageTempC: number | null
  relativeHumidityPct: number | null
  transport?: Transport | null
  priority?: Priority | null
  language?: Lang | null
}

export interface LayerDto {
  material: string
  thicknessUm: number
}

export interface Scores {
  barrier: number
  cost: number
  eco: number
  strength: number
  total: number
}

export interface CurvePoint {
  day: number
  o2UsedPct: number | null
  moistureUsedPct: number | null
}

export interface OptionDto {
  rank: number
  name: string
  kind: "FILM" | "LAMINATE"
  layers: LayerDto[]
  totalThicknessUm: number
  otr: number
  wvtr: number
  scores: Scores
  estimatedShelfLifeDays: number
  limitingFactor: "OXYGEN" | "MOISTURE" | "NONE"
  shelfLifeO2Days: number | null
  shelfLifeMoistureDays: number | null
  costPer1000Inr: number
  co2eKgPer1000: number
  gramsPerPack: number
  recyclable: boolean
  biodegradable: boolean
  family: string
  transparent: boolean
  heatSealable: boolean
  strength: number
  minTempC: number
  maxTempC: number
  approxData: boolean
  perforationNeeded: boolean
  reasons: string[]
  curve: CurvePoint[]
}

export interface NearMissDto {
  name: string
  kind: string
  layers: LayerDto[]
  otr: number
  wvtr: number
  bestAchievableShelfLifeDays: number
  limitingFactor: string
  costPer1000Inr: number
  reasons: string[]
}

export interface AvoidDto {
  name: string
  kind: string
  thicknessUm: number
  otr: number
  wvtr: number
  reason: string
}

export interface MapDto {
  mapTargetFound: boolean
  targetO2Min: number | null
  targetO2Max: number | null
  targetCo2Min: number | null
  targetCo2Max: number | null
  storageTempC: number | null
  gasMix: { o2Pct: number; co2Pct: number; n2Pct: number }
  respirationMlPerDay: number
  requiredOtr: number
  film: string
  filmThicknessUm: number
  filmOtr: number
  perforationNeeded: boolean
  reasons: string[]
  note: string | null
}

export interface InputsEcho {
  commodityId: number | null
  commodity: string
  moisturePct: number | null
  waterActivity: number
  criticalAw: number | null
  fatPct: number
  o2Sensitive: Level | null
  lightSensitive: Level | null
  respiring: boolean
  respirationRate: number | null
  ph?: number | null
  recommendedStorageTempMinC?: number | null
  recommendedStorageTempMaxC?: number | null
  mainDeteriorationFactor?: string | null
  packWeightG: number
  packAreaM2: number
  areaEstimated: boolean
  shelfLifeDays: number
  storageType: StorageType
  storageTempC: number
  relativeHumidityPct: number
  transport: Transport
  priority: Priority
  language: Lang
}

export interface RecommendResponse {
  id: number | null
  shareId: string | null
  commodity: string
  commodityHi: string | null
  aiEstimatedFood: boolean
  inputs: InputsEcho
  requirements: {
    o2Barrier: "HIGH" | "MEDIUM" | "LOW"
    moistureMode: "BREATHABLE" | "KEEP_OUT" | "KEEP_IN" | "MODERATE"
    opaque: boolean
    frozen: boolean
    needsMap: boolean
    needsStrength: boolean
    reasons: string[]
  }
  requiredOtr: number | null
  requiredWvtr: number | null
  options: OptionDto[]
  nearMisses: NearMissDto[]
  avoid: AvoidDto | null
  map: MapDto | null
  disclaimer: string
  createdAt: string | null
}

export interface CommoditySummary {
  id: number
  name: string
  nameHi: string | null
  category: string | null
  respiring: boolean
}

export interface CommodityDto extends CommoditySummary {
  moisturePct: number | null
  waterActivity: number | null
  criticalAw: number | null
  fatPct: number | null
  o2Sensitive: Level | null
  lightSensitive: Level | null
  respirationRate: number | null
  defaultShelfLifeDays: number | null
  ph: number | null
  storageTempMinC: number | null
  storageTempMaxC: number | null
  mainDeteriorationFactor: string | null
  sourceUrl: string | null
  notes: string | null
  approx: boolean
}

export interface MaterialDto {
  id: number
  name: string
  type: string
  otr25um: number | null
  wvtr25um: number | null
  costPerKgInr: number | null
  densityGCm3: number | null
  minTempC: number | null
  maxTempC: number | null
  recyclable: boolean | null
  biodegradable: boolean | null
  transparent: boolean | null
  heatSealable: boolean | null
  strength: number | null
  sourceUrl: string | null
  notes: string | null
  family: string | null
  co2eKgPerKg: number | null
  approx: boolean
  rigid: boolean
  ecoScore: number | null
  referenceCostPer1000Inr: number | null
  referenceCo2eKgPer1000: number | null
}

export interface LaminateDto {
  id: number
  name: string
  typicalUse: string | null
  layers: LayerDto[]
  totalThicknessUm: number
  otr: number
  wvtr: number
  minTempC: number
  maxTempC: number
  transparent: boolean
  heatSealable: boolean
  strength: number
  recyclable: boolean
  biodegradable: boolean
  family: string
  approx: boolean
  referenceAreaM2: number
  costPer1000Inr: number
  co2eKgPer1000: number
  ecoScore: number
}

export interface CityDto {
  id: number
  name: string
  state: string
  summerTempC: number | null
  summerRhPct: number | null
  monsoonTempC: number | null
  monsoonRhPct: number | null
  winterTempC: number | null
  winterRhPct: number | null
}

export interface LaminateEvaluateRequest {
  layers: { material: string; thicknessUm: number }[]
  packWeightG?: number | null
  packAreaM2?: number | null
  test?: {
    commodityId: number
    packWeightG?: number | null
    shelfLifeDays: number
    storageType: StorageType
    storageTempC: number
    relativeHumidityPct: number
    transport?: Transport | null
  } | null
}

export interface LaminateEvaluateResponse {
  otr: number
  wvtr: number
  totalThicknessUm: number
  areaM2: number
  gramsPerPack: number
  costPer1000Inr: number
  co2eKgPer1000: number
  recyclable: boolean
  biodegradable: boolean
  family: string
  transparent: boolean
  heatSealable: boolean
  strength: number
  minTempC: number
  maxTempC: number
  approx: boolean
  test: {
    commodity: string
    passes: boolean
    requiredOtr: number | null
    requiredWvtr: number | null
    mapRequiredOtr: number | null
    estimatedShelfLifeDays: number
    limitingFactor: string
    failReasons: string[]
    requirementReasons: string[]
  } | null
}

export interface RecommendationSummary {
  id: number
  shareId: string
  commodity: string
  topOption: string | null
  estimatedShelfLifeDays: number | null
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ParseResponse {
  draft: RecommendRequest
  commodityName: string | null
  missingFields: string[]
  aiUsed: boolean
  note: string | null
}

export interface ChatMessage {
  role: "user" | "assistant"
  content: string
}

export interface EstimateFoodResponse {
  food: CustomCommodity
  existingCommodityId: number | null
  aiEstimated: boolean
  aiUsed: boolean
  note: string | null
}
