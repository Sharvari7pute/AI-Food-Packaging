// Typed client for the PackSmart backend. The base URL comes from NEXT_PUBLIC_API_URL.
import type {
  ChatMessage,
  CityDto,
  CommodityDto,
  CommoditySummary,
  EstimateFoodResponse,
  LaminateDto,
  LaminateEvaluateRequest,
  LaminateEvaluateResponse,
  Lang,
  MaterialDto,
  Page,
  ParseResponse,
  RecommendationSummary,
  RecommendRequest,
  RecommendResponse,
} from "./types"

// "/" means same origin (API proxied by the frontend, see next.config.ts).
export const API_URL = (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080").replace(/\/$/, "")

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public details: string[] = [],
  ) {
    super(message)
    this.name = "ApiError"
  }
}

async function request<T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T> {
  let res: Response
  try {
    res = await fetch(`${API_URL}${path}`, {
      ...init,
      headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
      cache: "no-store",
    })
  } catch (e) {
    if ((e as Error).name === "AbortError") throw e
    throw new ApiError(
      "Cannot reach the PackSmart server. It may be waking up (free hosting sleeps) - try again in a minute.",
      0,
    )
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    let details: string[] = []
    try {
      const body = await res.json()
      message = body.error || message
      details = body.details || []
    } catch {
      /* not JSON */
    }
    throw new ApiError(message, res.status, details)
  }
  return res.json() as Promise<T>
}

const post = <T>(path: string, body: unknown, signal?: AbortSignal) =>
  request<T>(path, { method: "POST", body: JSON.stringify(body), signal })

export const api = {
  health: () => request<{ status: string; aiEnabled: boolean }>("/api/health"),
  commodities: () => request<CommoditySummary[]>("/api/commodities"),
  commodity: (id: number) => request<CommodityDto>(`/api/commodities/${id}`),
  materials: () => request<MaterialDto[]>("/api/materials"),
  laminates: () => request<LaminateDto[]>("/api/laminates"),
  cities: () => request<CityDto[]>("/api/cities"),
  evaluateLaminate: (body: LaminateEvaluateRequest, signal?: AbortSignal) =>
    post<LaminateEvaluateResponse>("/api/laminates/evaluate", body, signal),
  recommend: (body: RecommendRequest) => post<RecommendResponse>("/api/recommend", body),
  simulate: (body: RecommendRequest, signal?: AbortSignal) => post<RecommendResponse>("/api/simulate", body, signal),
  history: (page = 0, size = 20) => request<Page<RecommendationSummary>>(`/api/recommendations?page=${page}&size=${size}`),
  recommendation: (id: number | string) => request<RecommendResponse>(`/api/recommendations/${id}`),
  shared: (shareId: string) => request<RecommendResponse>(`/api/recommendations/share/${shareId}`),
  parse: (query: string) => post<ParseResponse>("/api/ai/parse", { query }),
  explain: (recommendationId: number | null, shareId: string | null, language: Lang) =>
    post<{ text: string; language: Lang; aiUsed: boolean }>("/api/ai/explain", { recommendationId, shareId, language }),
  chat: (messages: ChatMessage[], language: Lang, recommendationId?: number | null) =>
    post<{ reply: string; aiUsed: boolean }>("/api/ai/chat", { messages, language, recommendationId }),
  estimateFood: (name: string) => post<EstimateFoodResponse>("/api/ai/estimate-food", { name }),
}

export const pdfUrl = (shareId: string) => `${API_URL}/api/report/${shareId}/pdf`
export const qrUrl = (shareId: string) => `${API_URL}/api/report/${shareId}/qr`

/** Rebuilds an engine request from a saved result (used by What-if and "run again"). */
export function requestFromResult(r: RecommendResponse): RecommendRequest {
  const i = r.inputs
  const base: RecommendRequest = {
    packWeightG: i.packWeightG,
    packAreaM2: i.areaEstimated ? null : i.packAreaM2,
    shelfLifeDays: i.shelfLifeDays,
    storageType: i.storageType,
    storageTempC: i.storageTempC,
    relativeHumidityPct: i.relativeHumidityPct,
    transport: i.transport,
    priority: i.priority,
    language: i.language,
  }
  if (i.commodityId != null) {
    return {
      ...base,
      commodityId: i.commodityId,
      overrides: {
        moisturePct: i.moisturePct,
        waterActivity: i.waterActivity,
        fatPct: i.fatPct,
        respirationRate: i.respirationRate,
      },
    }
  }
  return {
    ...base,
    customCommodity: {
      name: i.commodity,
      moisturePct: i.moisturePct,
      waterActivity: i.waterActivity,
      criticalAw: i.criticalAw,
      fatPct: i.fatPct,
      o2Sensitive: i.o2Sensitive,
      lightSensitive: i.lightSensitive,
      respiring: i.respiring,
      respirationRate: i.respirationRate,
      aiEstimated: r.aiEstimatedFood,
    },
  }
}
