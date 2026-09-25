"use client"

import { useEffect, useMemo, useState } from "react"
import { ArrowDown, ArrowUp, CheckCircle2, FlaskConical, Loader2, Plus, Trash2, XCircle } from "lucide-react"
import { api } from "@/lib/api"
import { useApi, useDebounced } from "@/lib/hooks"
import { useI18n } from "@/lib/i18n"
import { inr, num } from "@/lib/format"
import type { LaminateEvaluateRequest, LaminateEvaluateResponse, StorageType } from "@/lib/types"
import { Container, PageHeader } from "@/components/common/page-header"
import { ApproxBadge, EcoBadges } from "@/components/common/badges"
import { ErrorState } from "@/components/common/states"
import { Field, NumberInput } from "@/components/common/field"
import { Segmented } from "@/components/common/segmented"
import { LayerStack } from "@/components/results/layer-stack"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

interface LayerRow {
  id: number
  material: string
  thicknessUm: number
}

const MAX_LAYERS = 8
const DEBOUNCE_MS = 300
let nextId = 1

export function BuilderPage() {
  const { t } = useI18n()
  const materials = useApi(() => api.materials())
  const laminates = useApi(() => api.laminates())
  const foods = useApi(() => api.commodities())

  const [layers, setLayers] = useState<LayerRow[]>([
    { id: nextId++, material: "PET", thicknessUm: 12 },
    { id: nextId++, material: "LDPE", thicknessUm: 50 },
  ])
  const [packWeightG, setPackWeightG] = useState<number | null>(100)
  const [testOn, setTestOn] = useState(false)
  const [foodId, setFoodId] = useState<number | null>(null)
  const [days, setDays] = useState<number | null>(90)
  const [storage, setStorage] = useState<StorageType>("AMBIENT")
  const [temp, setTemp] = useState<number | null>(30)
  const [rh, setRh] = useState<number | null>(65)

  const [result, setResult] = useState<LaminateEvaluateResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const types = useMemo(() => Object.fromEntries((materials.data ?? []).map((m) => [m.name, m.type])), [materials.data])
  const flexible = (materials.data ?? []).filter((m) => !m.rigid)

  const body: LaminateEvaluateRequest | null = useMemo(() => {
    if (!layers.length || layers.some((l) => !l.material || !(l.thicknessUm > 0))) return null
    const test =
      testOn && foodId && days && temp != null && rh != null
        ? { commodityId: foodId, packWeightG, shelfLifeDays: days, storageType: storage, storageTempC: temp, relativeHumidityPct: rh }
        : null
    return { layers: layers.map(({ material, thicknessUm }) => ({ material, thicknessUm })), packWeightG, test }
  }, [layers, packWeightG, testOn, foodId, days, storage, temp, rh])
  const debounced = useDebounced(body, DEBOUNCE_MS)

  useEffect(() => {
    if (!debounced) return
    const ctrl = new AbortController()
    // eslint-disable-next-line react-hooks/set-state-in-effect -- request status for the new structure
    setBusy(true)
    setError(null)
    api
      .evaluateLaminate(debounced, ctrl.signal)
      .then(setResult)
      .catch((e: Error) => e.name !== "AbortError" && setError(e.message))
      .finally(() => !ctrl.signal.aborted && setBusy(false))
    return () => ctrl.abort()
  }, [debounced])

  const update = (id: number, patch: Partial<LayerRow>) => setLayers((ls) => ls.map((l) => (l.id === id ? { ...l, ...patch } : l)))
  const move = (i: number, dir: -1 | 1) =>
    setLayers((ls) => {
      const j = i + dir
      if (j < 0 || j >= ls.length) return ls
      const copy = [...ls]
      ;[copy[i], copy[j]] = [copy[j], copy[i]]
      return copy
    })

  if (materials.error) return <Container><ErrorState message={materials.error} onRetry={materials.retry} /></Container>

  return (
    <Container>
      <PageHeader
        title={t("nav.builder")}
        description="Design your own structure layer by layer (outside → inside). Barrier, cost, CO₂ and recyclability update live from the engine."
      />
      <div className="grid gap-6 lg:grid-cols-[1.2fr_1fr]">
        <div className="space-y-6">
          <section className="rounded-2xl border bg-card p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
              <h2 className="font-semibold">Layers</h2>
              <div className="flex flex-wrap gap-1.5">
                <span className="self-center text-xs text-muted-foreground">Start from:</span>
                {(laminates.data ?? []).map((l) => (
                  <button
                    key={l.id}
                    type="button"
                    onClick={() => setLayers(l.layers.map((x) => ({ id: nextId++, material: x.material, thicknessUm: x.thicknessUm })))}
                    className="rounded-full border px-2.5 py-0.5 text-xs text-muted-foreground hover:border-primary hover:text-foreground"
                  >
                    {l.name}
                  </button>
                ))}
              </div>
            </div>
            {materials.loading ? (
              <Skeleton className="h-40" />
            ) : (
              <ol className="space-y-2">
                {layers.map((l, i) => (
                  <li key={l.id} className="flex flex-wrap items-center gap-2 rounded-xl border bg-muted/30 p-2.5">
                    <span className="w-14 text-xs text-muted-foreground">{i === 0 ? "outside" : i === layers.length - 1 ? "inside" : `layer ${i + 1}`}</span>
                    <select
                      value={l.material}
                      onChange={(e) => update(l.id, { material: e.target.value })}
                      className="h-9 min-w-40 flex-1 rounded-lg border bg-background px-2 text-sm dark:bg-input/30"
                      aria-label={`Material of layer ${i + 1}`}
                    >
                      {flexible.map((m) => (
                        <option key={m.id} value={m.name}>
                          {m.name}
                        </option>
                      ))}
                    </select>
                    <div className="flex items-center gap-1">
                      <input
                        type="number"
                        min={1}
                        max={500}
                        value={l.thicknessUm}
                        onChange={(e) => update(l.id, { thicknessUm: Number(e.target.value) })}
                        className="h-9 w-20 rounded-lg border bg-background px-2 text-sm dark:bg-input/30"
                        aria-label={`Thickness of layer ${i + 1} in micrometres`}
                      />
                      <span className="text-xs text-muted-foreground">µm</span>
                    </div>
                    <div className="ml-auto flex">
                      <Button variant="ghost" size="icon-sm" onClick={() => move(i, -1)} disabled={i === 0} aria-label="Move up">
                        <ArrowUp />
                      </Button>
                      <Button variant="ghost" size="icon-sm" onClick={() => move(i, 1)} disabled={i === layers.length - 1} aria-label="Move down">
                        <ArrowDown />
                      </Button>
                      <Button variant="ghost" size="icon-sm" onClick={() => setLayers((ls) => ls.filter((x) => x.id !== l.id))} disabled={layers.length === 1} aria-label="Remove layer">
                        <Trash2 />
                      </Button>
                    </div>
                  </li>
                ))}
              </ol>
            )}
            <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
              <Button
                variant="outline"
                onClick={() => setLayers((ls) => [...ls, { id: nextId++, material: flexible[0]?.name ?? "LDPE", thicknessUm: 25 }])}
                disabled={layers.length >= MAX_LAYERS}
              >
                <Plus /> Add layer
              </Button>
              <div className="flex items-center gap-2 text-sm">
                <span className="text-muted-foreground">Pack weight</span>
                <div className="w-24">
                  <NumberInput id="b-weight" value={packWeightG} onChange={setPackWeightG} min={1} />
                </div>
                <span className="text-muted-foreground">g</span>
              </div>
            </div>
            <div className="mt-5">
              <LayerStack layers={layers} types={types} />
            </div>
          </section>

          <section className="rounded-2xl border bg-card p-5 shadow-sm">
            <label className="flex cursor-pointer items-center gap-2 font-semibold">
              <input type="checkbox" checked={testOn} onChange={(e) => setTestOn(e.target.checked)} className="size-4 accent-[var(--primary)]" />
              <FlaskConical className="size-4 text-primary" /> Test against a food
            </label>
            {testOn && (
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <Field label="Food" htmlFor="b-food">
                  <select
                    id="b-food"
                    value={foodId ?? ""}
                    onChange={(e) => setFoodId(e.target.value ? Number(e.target.value) : null)}
                    className="h-10 w-full rounded-lg border bg-background px-2 text-sm dark:bg-input/30"
                  >
                    <option value="">Choose…</option>
                    {(foods.data ?? []).map((f) => (
                      <option key={f.id} value={f.id}>
                        {f.name} {f.nameHi ? `· ${f.nameHi}` : ""}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Shelf life (days)" htmlFor="b-days">
                  <NumberInput id="b-days" value={days} onChange={setDays} min={1} max={730} />
                </Field>
                <Field label="Storage" className="sm:col-span-2">
                  <Segmented<StorageType>
                    ariaLabel="Storage"
                    value={storage}
                    onChange={(v) => {
                      setStorage(v)
                      setTemp(v === "FROZEN" ? -18 : v === "CHILLED" ? 4 : 30)
                    }}
                    options={[
                      { value: "AMBIENT", label: "Ambient" },
                      { value: "CHILLED", label: "Chilled" },
                      { value: "FROZEN", label: "Frozen" },
                    ]}
                  />
                </Field>
                <Field label="Temperature (°C)" htmlFor="b-temp">
                  <NumberInput id="b-temp" value={temp} onChange={setTemp} min={-40} max={60} />
                </Field>
                <Field label="Humidity (% RH)" htmlFor="b-rh">
                  <NumberInput id="b-rh" value={rh} onChange={setRh} min={0} max={100} />
                </Field>
              </div>
            )}
          </section>
        </div>

        <aside className="space-y-4 lg:sticky lg:top-20 lg:self-start">
          <section className="rounded-2xl border bg-card p-5 shadow-sm">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-semibold">Structure properties</h2>
              {busy && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
            </div>
            {error && <p className="mb-3 text-sm text-destructive">{error}</p>}
            {!result ? (
              <Skeleton className="h-48" />
            ) : (
              <div className={cn("space-y-4 transition-opacity", busy && "opacity-60")}>
                <div className="grid grid-cols-2 gap-3">
                  <Metric label="OTR" value={num(result.otr)} unit="cc/m²·day·atm" />
                  <Metric label="WVTR" value={num(result.wvtr)} unit="g/m²·day" />
                  <Metric label="Cost" value={inr(result.costPer1000Inr)} unit="per 1000 packs" />
                  <Metric label="CO₂e" value={`${num(result.co2eKgPer1000, 2)} kg`} unit="per 1000 packs" />
                  <Metric label="Thickness" value={`${num(result.totalThicknessUm)} µm`} unit={`${num(result.gramsPerPack, 2)} g/pack`} />
                  <Metric label="Use temp." value={`${num(result.minTempC)}…${num(result.maxTempC)}`} unit="°C" />
                </div>
                <div className="flex flex-wrap gap-1.5 text-[11px]">
                  <EcoBadges recyclable={result.recyclable} biodegradable={result.biodegradable} />
                  {!result.recyclable && <span className="rounded-full bg-muted px-2 py-0.5 text-muted-foreground">Not recyclable ({result.family})</span>}
                  <span className={cn("rounded-full px-2 py-0.5", result.heatSealable ? "bg-success/15 text-success" : "bg-destructive/10 text-destructive")}>
                    {result.heatSealable ? "Inner layer heat-sealable" : "Inner layer NOT heat-sealable"}
                  </span>
                  <span className="rounded-full bg-muted px-2 py-0.5 text-muted-foreground">{result.transparent ? "Transparent" : "Opaque"}</span>
                  {result.approx && <ApproxBadge />}
                </div>
              </div>
            )}
          </section>

          {result?.test && (
            <section className={cn("rounded-2xl border p-5 shadow-sm", result.test.passes ? "border-success/50 bg-success/10" : "border-destructive/40 bg-destructive/5")}>
              <h2 className="flex items-center gap-2 font-semibold">
                {result.test.passes ? <CheckCircle2 className="size-5 text-success" /> : <XCircle className="size-5 text-destructive" />}
                {result.test.passes ? `Works for ${result.test.commodity}` : `Not suitable for ${result.test.commodity}`}
              </h2>
              <p className="mt-1 text-sm">
                Estimated shelf life <b>{result.test.estimatedShelfLifeDays} days</b>
                {result.test.limitingFactor !== "NONE" && ` (limited by ${result.test.limitingFactor.toLowerCase()})`}.
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {result.test.mapRequiredOtr != null
                  ? `Produce needs OTR ≥ ${num(result.test.mapRequiredOtr)}`
                  : `Required OTR ≤ ${result.test.requiredOtr != null ? num(result.test.requiredOtr) : "no limit"} · WVTR ≤ ${result.test.requiredWvtr != null ? num(result.test.requiredWvtr) : "no limit"}`}
              </p>
              {result.test.failReasons.length > 0 && (
                <ul className="mt-3 space-y-1 text-sm text-destructive">
                  {result.test.failReasons.map((r) => (
                    <li key={r}>✗ {r}</li>
                  ))}
                </ul>
              )}
            </section>
          )}
        </aside>
      </div>
    </Container>
  )
}

function Metric({ label, value, unit }: { label: string; value: string; unit: string }) {
  return (
    <div className="rounded-xl bg-muted/50 p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="truncate text-lg font-semibold">{value}</p>
      <p className="truncate text-[11px] text-muted-foreground">{unit}</p>
    </div>
  )
}
