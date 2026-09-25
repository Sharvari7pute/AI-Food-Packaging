"use client"

import { Clock, Coins, Factory, Info } from "lucide-react"
import type { OptionDto, RecommendResponse } from "@/lib/types"
import { useI18n } from "@/lib/i18n"
import { inr, num, titleCase } from "@/lib/format"
import { ApproxBadge, EcoBadges } from "@/components/common/badges"
import { LayerStack } from "./layer-stack"
import { BarrierBar } from "./barrier-bar"
import { cn } from "@/lib/utils"

export function OptionCard({
  option,
  result,
  types,
  baselineDays,
}: {
  option: OptionDto
  result: RecommendResponse
  types?: Record<string, string>
  baselineDays?: number | null
}) {
  const { t } = useI18n()
  const map = result.map
  const delta = baselineDays != null && baselineDays !== option.estimatedShelfLifeDays ? option.estimatedShelfLifeDays - baselineDays : null
  return (
    <article
      data-testid="option-card"
      className={cn(
        "flex flex-col gap-4 rounded-2xl border bg-card p-5 shadow-sm transition-shadow hover:shadow-md",
        option.rank === 1 && "border-primary/50 ring-1 ring-primary/30",
      )}
    >
      <header className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={cn(
                "rounded-full px-2.5 py-0.5 text-xs font-bold",
                option.rank === 1 ? "bg-primary text-primary-foreground" : "bg-muted text-foreground",
              )}
            >
              #{option.rank}
            </span>
            <span className="text-xs text-muted-foreground">{option.kind === "LAMINATE" ? "Laminate" : "Single film"}</span>
          </div>
          <h3 className="mt-1.5 truncate text-lg font-semibold" title={option.name}>
            {option.name}
          </h3>
          <p className="text-xs text-muted-foreground">Total {num(option.totalThicknessUm)} µm · score {num(option.scores.total, 2)}</p>
        </div>
        <div className="text-right">
          <p className="text-2xl font-bold text-brand">{option.estimatedShelfLifeDays}</p>
          <p className="text-[11px] text-muted-foreground">{t("results.days")}</p>
          {delta != null && (
            <p className={cn("text-xs font-medium", delta < 0 ? "text-destructive" : "text-success")}>
              {delta > 0 ? "+" : ""}
              {delta} d
            </p>
          )}
        </div>
      </header>

      <LayerStack layers={option.layers} types={types} />

      <div className="space-y-2.5">
        {map ? (
          <BarrierBar label="OTR (breathability)" unit="" value={option.otr} required={map.requiredOtr} mode="min" />
        ) : (
          <>
            <BarrierBar label="OTR" unit="cc/m²·d" value={option.otr} required={result.requiredOtr} />
            <BarrierBar label="WVTR" unit="g/m²·d" value={option.wvtr} required={result.requiredWvtr} />
          </>
        )}
      </div>

      <dl className="grid grid-cols-3 gap-2 text-center">
        <div className="rounded-lg bg-muted/60 p-2">
          <dt className="flex items-center justify-center gap-1 text-[11px] text-muted-foreground">
            <Clock className="size-3" /> {t("results.limitedBy")}
          </dt>
          <dd className="text-sm font-semibold">{option.limitingFactor === "NONE" ? "Default" : titleCase(option.limitingFactor)}</dd>
        </div>
        <div className="rounded-lg bg-muted/60 p-2">
          <dt className="flex items-center justify-center gap-1 text-[11px] text-muted-foreground">
            <Coins className="size-3" /> ₹/1000
          </dt>
          <dd className="text-sm font-semibold">{inr(option.costPer1000Inr)}</dd>
        </div>
        <div className="rounded-lg bg-muted/60 p-2">
          <dt className="flex items-center justify-center gap-1 text-[11px] text-muted-foreground">
            <Factory className="size-3" /> CO₂e/1000
          </dt>
          <dd className="text-sm font-semibold">{num(option.co2eKgPer1000, 1)} kg</dd>
        </div>
      </dl>

      <div className="flex flex-wrap items-center gap-1.5">
        <EcoBadges recyclable={option.recyclable} biodegradable={option.biodegradable} />
        {!option.recyclable && !option.biodegradable && (
          <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">Not recyclable ({option.family})</span>
        )}
        {option.perforationNeeded && (
          <span className="rounded-full bg-brand/15 px-2 py-0.5 text-[11px] font-medium text-brand">Micro-perforated</span>
        )}
        {option.approxData && <ApproxBadge />}
      </div>

      {option.reasons.length > 0 && (
        <p className="flex gap-1.5 border-t pt-3 text-xs text-muted-foreground">
          <Info className="mt-0.5 size-3.5 shrink-0" /> {option.reasons[0]}
        </p>
      )}
    </article>
  )
}
