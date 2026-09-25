"use client"

import { useMemo, useState, type ReactNode } from "react"
import { Check, X } from "lucide-react"
import { PolarAngleAxis, PolarGrid, PolarRadiusAxis, Radar, RadarChart, Legend, ResponsiveContainer, Tooltip } from "recharts"
import { api } from "@/lib/api"
import { useApi } from "@/lib/hooks"
import { useI18n } from "@/lib/i18n"
import { inr, num } from "@/lib/format"
import type { RecommendResponse } from "@/lib/types"
import { Container, PageHeader } from "@/components/common/page-header"
import { ApproxBadge } from "@/components/common/badges"
import { CardGridSkeleton, ErrorState } from "@/components/common/states"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"

const MAX_SELECTED = 3
const COLORS = ["var(--chart-1)", "var(--chart-2)", "var(--chart-4)"]
// Display-only log scales for the radar (barrier: lower transmission = better).
const OTR_RANGE = { best: 0.001, worst: 10000 }
const WVTR_RANGE = { best: 0.001, worst: 500 }

interface Item {
  key: string
  group: "Your result" | "Laminate" | "Material"
  name: string
  structure: string
  otr: number | null
  wvtr: number | null
  cost: number | null
  co2: number | null
  eco: number | null
  strength: number | null
  recyclable: boolean
  heatSealable: boolean | null
  temp: string
  approx: boolean
  note: string
}

function logScore(v: number | null, r: { best: number; worst: number }): number {
  if (v == null) return 0
  const x = Math.log10(Math.max(v, r.best))
  return Math.max(0, Math.min(1, (Math.log10(r.worst) - x) / (Math.log10(r.worst) - Math.log10(r.best))))
}

export function ComparePage({ recommendationId }: { recommendationId: number | null }) {
  const { t } = useI18n()
  const materials = useApi(() => api.materials())
  const laminates = useApi(() => api.laminates())
  const rec = useApi<RecommendResponse | null>(() => (recommendationId ? api.recommendation(recommendationId) : Promise.resolve(null)), [recommendationId])

  const items: Item[] = useMemo(() => {
    const out: Item[] = []
    rec.data?.options.forEach((o) =>
      out.push({
        key: `opt-${o.rank}`,
        group: "Your result",
        name: `#${o.rank} ${o.name}`,
        structure: o.layers.map((l) => `${l.material} ${l.thicknessUm}`).join(" / ") + " µm",
        otr: o.otr,
        wvtr: o.wvtr,
        cost: o.costPer1000Inr,
        co2: o.co2eKgPer1000,
        eco: o.scores.eco,
        strength: o.strength,
        recyclable: o.recyclable,
        heatSealable: o.heatSealable,
        temp: `${num(o.minTempC)} to ${num(o.maxTempC)} °C`,
        approx: o.approxData,
        note: `${o.estimatedShelfLifeDays} days for ${rec.data?.commodity}`,
      }),
    )
    laminates.data?.forEach((l) =>
      out.push({
        key: `lam-${l.id}`,
        group: "Laminate",
        name: l.name,
        structure: l.layers.map((x) => `${x.material} ${x.thicknessUm}`).join(" / ") + " µm",
        otr: l.otr,
        wvtr: l.wvtr,
        cost: l.costPer1000Inr,
        co2: l.co2eKgPer1000,
        eco: l.ecoScore,
        strength: l.strength,
        recyclable: l.recyclable,
        heatSealable: l.heatSealable,
        temp: `${num(l.minTempC)} to ${num(l.maxTempC)} °C`,
        approx: l.approx,
        note: l.typicalUse ?? "",
      }),
    )
    materials.data?.forEach((m) =>
      out.push({
        key: `mat-${m.id}`,
        group: "Material",
        name: m.name,
        structure: `${m.name} 25 µm${m.rigid ? " (rigid reference)" : ""}`,
        otr: m.otr25um,
        wvtr: m.wvtr25um,
        cost: m.referenceCostPer1000Inr,
        co2: m.referenceCo2eKgPer1000,
        eco: m.ecoScore,
        strength: m.strength,
        recyclable: !!m.recyclable,
        heatSealable: m.heatSealable,
        temp: `${num(m.minTempC)} to ${num(m.maxTempC)} °C`,
        approx: m.approx,
        note: m.type,
      }),
    )
    return out
  }, [materials.data, laminates.data, rec.data])

  const defaultKeys = useMemo(() => {
    const own = items.filter((i) => i.group === "Your result").slice(0, MAX_SELECTED)
    const ownNames = new Set(own.map((i) => i.name.replace(/^#\d+ /, "")))
    const fill = items.filter((i) => i.group === "Laminate" && !ownNames.has(i.name)).slice(0, Math.max(0, 2 - own.length))
    return [...own, ...fill].map((i) => i.key)
  }, [items])
  const [picked, setPicked] = useState<string[] | null>(null)
  const selectedKeys = picked ?? defaultKeys
  const selected = selectedKeys.map((k) => items.find((i) => i.key === k)).filter((x): x is Item => !!x)

  function toggle(key: string) {
    const cur = selectedKeys
    if (cur.includes(key)) setPicked(cur.filter((k) => k !== key))
    else if (cur.length < MAX_SELECTED) setPicked([...cur, key])
    else setPicked([...cur.slice(1), key])
  }

  const minCost = Math.min(...selected.map((s) => s.cost ?? Infinity))
  const radar = [
    { axis: "O₂ barrier", ...Object.fromEntries(selected.map((s) => [s.key, logScore(s.otr, OTR_RANGE)])) },
    { axis: "Moisture barrier", ...Object.fromEntries(selected.map((s) => [s.key, logScore(s.wvtr, WVTR_RANGE)])) },
    { axis: "Cost", ...Object.fromEntries(selected.map((s) => [s.key, s.cost ? minCost / s.cost : 0])) },
    { axis: "Eco", ...Object.fromEntries(selected.map((s) => [s.key, s.eco ?? 0])) },
    { axis: "Strength", ...Object.fromEntries(selected.map((s) => [s.key, (s.strength ?? 0) / 5])) },
  ]

  const error = materials.error || laminates.error || rec.error
  const loading = materials.loading || laminates.loading

  return (
    <Container>
      <PageHeader
        title={t("nav.compare")}
        description="Pick 2–3 materials, laminates or options from your result and compare them side by side. Single films are shown at 25 µm; costs are for a 100 g reference pack (your result uses your own pack)."
      />
      {error && <ErrorState message={error} onRetry={() => (materials.retry(), laminates.retry(), rec.retry())} />}
      {loading && <CardGridSkeleton count={3} />}
      {!loading && !error && (
        <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
          <aside className="space-y-4 rounded-2xl border bg-card p-4 shadow-sm lg:max-h-[80vh] lg:overflow-y-auto">
            {(["Your result", "Laminate", "Material"] as const).map((g) => {
              const groupItems = items.filter((i) => i.group === g)
              if (!groupItems.length) return null
              return (
                <div key={g}>
                  <p className="mb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">{g}</p>
                  <div className="flex flex-wrap gap-1.5">
                    {groupItems.map((i) => {
                      const idx = selectedKeys.indexOf(i.key)
                      return (
                        <button
                          key={i.key}
                          type="button"
                          onClick={() => toggle(i.key)}
                          aria-pressed={idx >= 0}
                          className={cn(
                            "flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                            idx >= 0 ? "border-transparent text-white" : "text-muted-foreground hover:text-foreground",
                          )}
                          style={idx >= 0 ? { background: COLORS[idx] } : undefined}
                        >
                          {i.name}
                        </button>
                      )
                    })}
                  </div>
                </div>
              )
            })}
            <p className="text-xs text-muted-foreground">Selecting a 4th item replaces the oldest.</p>
          </aside>

          <div className="space-y-6">
            <div className="rounded-2xl border bg-card p-4 shadow-sm">
              {selected.length < 2 ? (
                <p className="p-10 text-center text-sm text-muted-foreground">Pick at least two items to compare.</p>
              ) : (
                <div className="h-80">
                  <ResponsiveContainer width="100%" height="100%">
                    <RadarChart data={radar} outerRadius="75%">
                      <PolarGrid stroke="var(--border)" />
                      <PolarAngleAxis dataKey="axis" tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                      <PolarRadiusAxis domain={[0, 1]} tick={false} axisLine={false} />
                      {selected.map((s, i) => (
                        <Radar key={s.key} name={s.name} dataKey={s.key} stroke={COLORS[i]} fill={COLORS[i]} fillOpacity={0.18} strokeWidth={2} />
                      ))}
                      <Legend wrapperStyle={{ fontSize: 12 }} />
                      <Tooltip formatter={(v) => Number(v).toFixed(2)} contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 12 }} />
                    </RadarChart>
                  </ResponsiveContainer>
                </div>
              )}
              <p className="text-center text-[11px] text-muted-foreground">Outer = better on every axis (barrier on a log scale; cost relative to the cheapest selected).</p>
            </div>

            {selected.length > 0 && (
              <div className="overflow-x-auto rounded-2xl border bg-card shadow-sm">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-40">Property</TableHead>
                      {selected.map((s, i) => (
                        <TableHead key={s.key}>
                          <span className="flex items-center gap-1.5">
                            <span className="size-2.5 rounded-full" style={{ background: COLORS[i] }} /> {s.name}
                            {s.approx && <ApproxBadge />}
                          </span>
                        </TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    <Row label="Structure" cells={selected.map((s) => s.structure)} />
                    <Row label="OTR (cc/m²·day·atm)" cells={selected.map((s) => num(s.otr))} best={bestIdx(selected.map((s) => s.otr), "min")} />
                    <Row label="WVTR (g/m²·day)" cells={selected.map((s) => num(s.wvtr))} best={bestIdx(selected.map((s) => s.wvtr), "min")} />
                    <Row label="₹ / 1000 packs" cells={selected.map((s) => inr(s.cost))} best={bestIdx(selected.map((s) => s.cost), "min")} />
                    <Row label="CO₂e kg / 1000" cells={selected.map((s) => num(s.co2, 2))} best={bestIdx(selected.map((s) => s.co2), "min")} />
                    <Row label="Recyclable" cells={selected.map((s) => <YesNo key={s.key} v={s.recyclable} />)} />
                    <Row label="Heat sealable" cells={selected.map((s) => <YesNo key={s.key} v={!!s.heatSealable} />)} />
                    <Row label="Strength (1–5)" cells={selected.map((s) => num(s.strength))} best={bestIdx(selected.map((s) => s.strength), "max")} />
                    <Row label="Use temperature" cells={selected.map((s) => s.temp)} />
                    <Row label="Notes" cells={selected.map((s) => s.note)} />
                  </TableBody>
                </Table>
              </div>
            )}
          </div>
        </div>
      )}
    </Container>
  )
}

function bestIdx(values: (number | null)[], mode: "min" | "max"): number {
  let best = -1
  values.forEach((v, i) => {
    if (v == null) return
    if (best < 0 || (mode === "min" ? v < (values[best] as number) : v > (values[best] as number))) best = i
  })
  return values.filter((v) => v != null).length > 1 ? best : -1
}

function Row({ label, cells, best = -1 }: { label: string; cells: ReactNode[]; best?: number }) {
  return (
    <TableRow>
      <TableCell className="text-xs font-medium text-muted-foreground">{label}</TableCell>
      {cells.map((c, i) => (
        <TableCell key={i} className={cn("text-sm whitespace-normal", i === best && "font-semibold text-success")}>
          {c}
        </TableCell>
      ))}
    </TableRow>
  )
}

function YesNo({ v }: { v: boolean }) {
  return v ? <Check className="size-4 text-success" /> : <X className="size-4 text-muted-foreground" />
}
