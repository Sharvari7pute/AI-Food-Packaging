"use client"

import { useMemo, useState } from "react"
import { ExternalLink, Search } from "lucide-react"
import { api } from "@/lib/api"
import { useApi } from "@/lib/hooks"
import { useI18n } from "@/lib/i18n"
import { num } from "@/lib/format"
import { materialColor } from "@/lib/material-colors"
import type { MaterialDto } from "@/lib/types"
import { Container, PageHeader } from "@/components/common/page-header"
import { ApproxBadge, EcoBadges } from "@/components/common/badges"
import { CardGridSkeleton, EmptyState, ErrorState } from "@/components/common/states"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

// Display filter only: "high barrier" = OTR ≤ 10 or WVTR ≤ 1 at 25 µm.
const HIGH_BARRIER_OTR = 10
const HIGH_BARRIER_WVTR = 1

type Filter = "recyclable" | "biodegradable" | "transparent" | "highBarrier"
const FILTERS: { key: Filter; label: string; test: (m: MaterialDto) => boolean }[] = [
  { key: "recyclable", label: "Recyclable", test: (m) => !!m.recyclable },
  { key: "biodegradable", label: "Biodegradable", test: (m) => !!m.biodegradable },
  { key: "transparent", label: "Transparent", test: (m) => !!m.transparent },
  {
    key: "highBarrier",
    label: "High barrier",
    test: (m) => (m.otr25um ?? Infinity) <= HIGH_BARRIER_OTR || (m.wvtr25um ?? Infinity) <= HIGH_BARRIER_WVTR,
  },
]

export function MaterialsPage() {
  const { t } = useI18n()
  const { data, error, loading, retry } = useApi(() => api.materials())
  const [active, setActive] = useState<Set<Filter>>(new Set())
  const [q, setQ] = useState("")

  const shown = useMemo(
    () =>
      (data ?? []).filter(
        (m) =>
          FILTERS.every((f) => !active.has(f.key) || f.test(m)) &&
          (!q.trim() || `${m.name} ${m.type} ${m.family ?? ""}`.toLowerCase().includes(q.trim().toLowerCase())),
      ),
    [data, active, q],
  )

  const toggle = (f: Filter) =>
    setActive((s) => {
      const n = new Set(s)
      if (n.has(f)) n.delete(f)
      else n.add(f)
      return n
    })

  return (
    <Container>
      <PageHeader
        title={t("nav.materials")}
        description="Every material the engine knows, with barrier values normalised to 25 µm. Rows marked approx or VERIFY are still being checked by our team."
      />
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative sm:w-72">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search materials…" className="h-10 pl-9" />
        </div>
        <div className="flex flex-wrap gap-2">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              type="button"
              onClick={() => toggle(f.key)}
              aria-pressed={active.has(f.key)}
              className={cn(
                "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors",
                active.has(f.key) ? "border-primary bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground",
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>
      {loading && <CardGridSkeleton />}
      {error && <ErrorState message={error} onRetry={retry} />}
      {data && shown.length === 0 && <EmptyState title="No material matches these filters" />}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {shown.map((m) => (
          <MaterialCard key={m.id} m={m} />
        ))}
      </div>
    </Container>
  )
}

function MaterialCard({ m }: { m: MaterialDto }) {
  return (
    <article className="flex flex-col gap-4 rounded-2xl border bg-card p-5 shadow-sm">
      <header className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-3">
          <span className="size-9 shrink-0 rounded-lg ring-1 ring-border" style={{ background: materialColor(m.name, m.type) }} />
          <div>
            <h3 className="font-semibold">{m.name}</h3>
            <p className="text-xs text-muted-foreground">
              {m.type}
              {m.family ? ` · family ${m.family}` : ""}
              {m.rigid ? " · reference only" : ""}
            </p>
          </div>
        </div>
        {m.approx && <ApproxBadge />}
      </header>
      <dl className="grid grid-cols-2 gap-2 text-sm">
        <Prop label="OTR @25µm" value={`${num(m.otr25um)}`} unit="cc/m²·day·atm" />
        <Prop label="WVTR @25µm" value={`${num(m.wvtr25um)}`} unit="g/m²·day" />
        <Prop label="Cost" value={`₹${num(m.costPerKgInr)}`} unit="per kg" />
        <Prop label="Density" value={num(m.densityGCm3)} unit="g/cm³" />
        <Prop label="Use temp." value={`${num(m.minTempC)}…${num(m.maxTempC)}`} unit="°C" />
        <Prop label="CO₂e" value={num(m.co2eKgPerKg)} unit="kg per kg (approx)" />
      </dl>
      <div className="flex flex-wrap gap-1.5 text-[11px]">
        <EcoBadges recyclable={!!m.recyclable} biodegradable={!!m.biodegradable} />
        <Flag on={!!m.transparent} label="Transparent" off="Opaque" />
        <Flag on={!!m.heatSealable} label="Heat sealable" off="Not sealable" />
        <span className="rounded-full bg-muted px-2 py-0.5 text-muted-foreground">Strength {m.strength ?? "-"}/5</span>
      </div>
      {m.notes && <p className="text-xs text-muted-foreground">{m.notes}</p>}
      {m.sourceUrl && m.sourceUrl.startsWith("http") && (
        <a href={m.sourceUrl} target="_blank" rel="noreferrer" className="mt-auto flex items-center gap-1 text-xs font-medium text-primary hover:underline">
          <ExternalLink className="size-3" /> Source
        </a>
      )}
    </article>
  )
}

function Prop({ label, value, unit }: { label: string; value: string; unit: string }) {
  return (
    <div className="rounded-lg bg-muted/50 px-3 py-2">
      <dt className="text-[11px] text-muted-foreground">{label}</dt>
      <dd className="font-semibold">
        {value} <span className="text-[10px] font-normal text-muted-foreground">{unit}</span>
      </dd>
    </div>
  )
}

function Flag({ on, label, off }: { on: boolean; label: string; off: string }) {
  return <span className={cn("rounded-full px-2 py-0.5", on ? "bg-brand/10 text-brand" : "bg-muted text-muted-foreground")}>{on ? label : off}</span>
}
