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

/** Free hosting sleeps when idle: the first requests fail with 502/503/504 (or a network error) while it wakes up. */
const WAKE_STATUSES = new Set([502, 503, 504])
const WAKE_TIMEOUT_MS = 150_000
const RETRY_EVERY_MS = 4_000
export const WAKING_EVENT = "packsmart:waking"

function announceWaking(waking: boolean) {
  if (typeof window !== "undefined") window.dispatchEvent(new CustomEvent(WAKING_EVENT, { detail: waking }))
}

const sleep = (ms: number, signal?: AbortSignal) =>
  new Promise<void>((resolve, reject) => {
    const id = setTimeout(resolve, ms)
    signal?.addEventListener("abort", () => {
      clearTimeout(id)
      reject(new DOMException("Aborted", "AbortError"))
    })
  })

async function request<T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T> {
  const started = Date.now()
  let waking = false
  try {
    for (;;) {
      let res: Response | null = null
      try {
        res = await fetch(`${API_URL}${path}`, {
          ...init,
          headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
          cache: "no-store",
        })
      } catch (e) {
        if ((e as Error).name === "AbortError") throw e
        res = null
      }
      const retryable = res === null || WAKE_STATUSES.has(res.status)
      if (retryable && Date.now() - started < WAKE_TIMEOUT_MS) {
        if (!waking) {
          waking = true
          announceWaking(true)
        }
        await sleep(RETRY_EVERY_MS, init?.signal)
        continue
      }
      if (res === null) {
        throw new ApiError("Cannot reach the PackSmart server. Please check your internet connection and try again.", 0)
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
        if (WAKE_STATUSES.has(res.status)) message = "The server is taking too long to wake up. Please try again in a minute."
        throw new ApiError(message, res.status, details)
      }
      return (await res.json()) as T
    }
  } finally {
    if (waking) announceWaking(false)
  }
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
