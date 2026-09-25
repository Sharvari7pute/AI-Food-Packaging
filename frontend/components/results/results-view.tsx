"use client"

import { useMemo, useState } from "react"
import { Ban, CheckCircle2, Eye, FlaskConical, ShieldCheck, Snowflake, Sparkles, SunDim, Truck } from "lucide-react"
import type { RecommendResponse } from "@/lib/types"
import { api } from "@/lib/api"
import { useApi } from "@/lib/hooks"
import { useI18n } from "@/lib/i18n"
import { dateTime, inr, num, titleCase } from "@/lib/format"
import { ApproxBadge } from "@/components/common/badges"
import { OptionCard } from "./option-card"
import { ShelfLifeChart } from "./shelf-life-chart"
import { MapPanel } from "./map-panel"
import { WhatIf } from "./what-if"
import { ExplanationBox } from "./explanation-box"
import { ResultActions } from "./result-actions"
import { LayerStack } from "./layer-stack"
import { cn } from "@/lib/utils"
import type { ReactNode } from "react"

function Section({ title, icon, children, className, action }: { title: ReactNode; icon?: ReactNode; children: ReactNode; className?: string; action?: ReactNode }) {
  return (
    <section className={cn("rounded-2xl border bg-card p-5 shadow-sm sm:p-6", className)}>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h2 className="flex items-center gap-2 text-lg font-semibold">
          {icon}
          {title}
        </h2>
        {action}
      </div>
      {children}
    </section>
  )
}

export function ResultsView({ result, readOnly = false }: { result: RecommendResponse; readOnly?: boolean }) {
  const { t } = useI18n()
  const [sim, setSim] = useState<RecommendResponse | null>(null)
  const materials = useApi(() => api.materials())
  const types = useMemo(() => Object.fromEntries((materials.data ?? []).map((m) => [m.name, m.type])), [materials.data])

  const shown = sim ?? result
  const i = shown.inputs
  const req = shown.requirements
  const baselineDays = (name: string) => (sim ? result.options.find((o) => o.name === name)?.estimatedShelfLifeDays ?? null : null)

  const chips = [
    { on: req.o2Barrier !== "LOW", label: `O₂ barrier ${req.o2Barrier}`, icon: <ShieldCheck className="size-3.5" /> },
    { on: true, label: titleCase(req.moistureMode), icon: <FlaskConical className="size-3.5" /> },
    { on: req.opaque, label: "Opaque", icon: <SunDim className="size-3.5" /> },
    { on: req.frozen, label: "Frozen-grade", icon: <Snowflake className="size-3.5" /> },
    { on: req.needsStrength, label: "Strong", icon: <Truck className="size-3.5" /> },
    { on: req.needsMap, label: "MAP", icon: <Sparkles className="size-3.5" /> },
  ].filter((c) => c.on)

  return (
    <div className="space-y-6">
      {/* Summary strip */}
      <section className="overflow-hidden rounded-2xl border bg-gradient-to-br from-brand to-primary text-white shadow-md">
        <div className="grid gap-4 p-5 sm:p-6 md:grid-cols-[1.3fr_1fr]">
          <div>
            <p className="text-sm text-white/75">{readOnly ? t("results.verified") : t("results.summary")}</p>
            <h1 className="mt-1 flex flex-wrap items-center gap-2 text-2xl font-bold sm:text-3xl">
              {shown.commodity} {shown.commodityHi && <span className="font-normal text-white/80">{shown.commodityHi}</span>}
              {shown.aiEstimatedFood && (
                <span className="rounded-md bg-warning px-2 py-0.5 text-xs font-semibold text-black">AI-estimated food — verify</span>
              )}
            </h1>
            <p className="mt-2 text-sm text-white/85">
              {num(i.packWeightG)} g pack · {i.shelfLifeDays} days wanted · {titleCase(i.storageType)} {num(i.storageTempC)} °C ·{" "}
              {num(i.relativeHumidityPct)}% RH · {i.transport === "LONG_DISTANCE" ? "long distance" : "local"} · area {num(i.packAreaM2)} m²
              {i.areaEstimated ? " (est.)" : ""}
            </p>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {chips.map((c) => (
                <span key={c.label} className="flex items-center gap-1 rounded-full bg-white/15 px-2.5 py-1 text-xs font-medium backdrop-blur">
                  {c.icon}
                  {c.label}
                </span>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3 self-center">
            <Stat label="Required OTR" value={shown.map ? `≥ ${num(shown.map.requiredOtr)}` : shown.requiredOtr != null ? `≤ ${num(shown.requiredOtr)}` : "no limit"} unit="cc/m²·day·atm" />
            <Stat label="Required WVTR" value={shown.requiredWvtr != null ? `≤ ${num(shown.requiredWvtr)}` : "no limit"} unit="g/m²·day" />
            <Stat label="Best shelf life" value={shown.options[0] ? `${shown.options[0].estimatedShelfLifeDays} d` : "-"} unit={shown.options[0]?.name ?? "no option"} />
            <Stat label="Best cost" value={shown.options[0] ? inr(shown.options[0].costPer1000Inr) : "-"} unit={t("results.per1000")} />
          </div>
        </div>
        {sim && (
          <div className="flex items-center gap-2 bg-black/20 px-5 py-2 text-xs">
            <Eye className="size-3.5" /> What-if preview — not saved. Reset the sliders to see the saved result.
          </div>
        )}
      </section>

      {!readOnly && result.id != null && result.shareId && <ResultActions id={result.id} shareId={result.shareId} />}
      {readOnly && result.createdAt && (
        <p className="text-sm text-muted-foreground">
          Generated {dateTime(result.createdAt)} · ID {result.shareId}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_1.4fr]">
        <Section title={t("results.why")} icon={<CheckCircle2 className="size-5 text-primary" />}>
          <ul className="space-y-2.5" data-testid="decision-trace">
            {req.reasons.map((r) => (
              <li key={r} className="flex gap-2.5 text-sm">
                <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-success" />
                <span>{r}</span>
              </li>
            ))}
          </ul>
        </Section>
        <Section title={t("results.explanation")} icon={<Sparkles className="size-5 text-violet-500" />}>
          <ExplanationBox id={result.id} shareId={result.shareId} initialLang={result.inputs.language} />
        </Section>
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{t("results.options")}</h2>
          {shown.options.some((o) => o.approxData) && <ApproxBadge label="some data approximate" />}
        </div>
        {shown.options.length === 0 ? (
          <div className="rounded-2xl border border-dashed p-8 text-center text-sm text-muted-foreground">
            No pack meets every requirement under these conditions. Look at the near misses below — they show the best shelf life
            you can reach.
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {shown.options.map((o) => (
              <OptionCard key={o.name + o.rank} option={o} result={shown} types={types} baselineDays={baselineDays(o.name)} />
            ))}
          </div>
        )}
      </div>

      {shown.map && (
        <Section title={t("results.map")} icon={<Sparkles className="size-5 text-brand" />}>
          <MapPanel map={shown.map} />
        </Section>
      )}

      {shown.options.length > 0 && (
        <Section title={t("results.curve")}>
          <ShelfLifeChart options={shown.options} targetDays={i.shelfLifeDays} />
        </Section>
      )}

      {!readOnly && (
        <Section title={t("results.whatIf")}>
          <WhatIf base={result} onResult={setSim} />
        </Section>
      )}

      <div className="grid items-start gap-6 lg:grid-cols-2">
        {shown.avoid && (
          <section className="rounded-2xl border border-destructive/40 bg-destructive/5 p-5 sm:p-6" data-testid="avoid-card">
            <h2 className="mb-3 flex items-center gap-2 text-lg font-semibold text-destructive">
              <Ban className="size-5" /> {t("results.avoid")}: {shown.avoid.name} {num(shown.avoid.thicknessUm)} µm
            </h2>
            <ul className="space-y-1.5 text-sm">
              {shown.avoid.reason.split("; ").map((r) => (
                <li key={r} className="flex gap-2">
                  <span className="text-destructive">✗</span> {r}
                </li>
              ))}
            </ul>
          </section>
        )}
        {shown.nearMisses.length > 0 && (
          <Section title={t("results.nearMisses")} className="border-warning/50">
            <p className="mb-3 text-xs text-muted-foreground">Fewer than 3 packs passed. These came closest — and what they would achieve:</p>
            <ul className="space-y-3">
              {shown.nearMisses.map((n) => (
                <li key={n.name} className="rounded-xl border p-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium">{n.name}</span>
                    <span className="text-xs text-muted-foreground">
                      best achievable: <b className="text-foreground">{n.bestAchievableShelfLifeDays} days</b>
                    </span>
                  </div>
                  <div className="mt-2">
                    <LayerStack layers={n.layers} types={types} />
                  </div>
                  <p className="mt-2 text-xs text-destructive">{n.reasons.join(" · ")}</p>
                </li>
              ))}
            </ul>
          </Section>
        )}
      </div>

      <p className="rounded-xl bg-muted/50 p-4 text-xs text-muted-foreground">{shown.disclaimer}</p>
    </div>
  )
}

function Stat({ label, value, unit }: { label: string; value: string; unit: string }) {
  return (
    <div className="rounded-xl bg-white/12 p-3 backdrop-blur">
      <p className="text-[11px] text-white/70">{label}</p>
      <p className="truncate text-lg font-bold">{value}</p>
      <p className="truncate text-[10px] text-white/70">{unit}</p>
    </div>
  )
}
