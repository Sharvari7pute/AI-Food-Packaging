"use client"

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts"
import { Thermometer, Wind } from "lucide-react"
import type { MapDto } from "@/lib/types"
import { num } from "@/lib/format"

const COLORS = ["var(--chart-2)", "var(--chart-3)", "var(--chart-5)"]

export function MapPanel({ map }: { map: MapDto }) {
  const data = [
    { name: "O₂", value: map.gasMix.o2Pct },
    { name: "CO₂", value: map.gasMix.co2Pct },
    { name: "N₂", value: map.gasMix.n2Pct },
  ]
  return (
    <div className="grid items-center gap-6 md:grid-cols-[220px_1fr]">
      <div className="relative mx-auto h-52 w-52">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={data} dataKey="value" innerRadius={62} outerRadius={92} paddingAngle={2} stroke="none">
              {data.map((d, i) => (
                <Cell key={d.name} fill={COLORS[i]} />
              ))}
            </Pie>
            <Tooltip formatter={(v) => `${v}%`} contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 12 }} />
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 grid place-items-center text-center">
          <div>
            <p className="text-xs text-muted-foreground">target</p>
            <p className="text-sm font-semibold">
              {num(map.gasMix.o2Pct)}% O₂
              <br />
              {num(map.gasMix.co2Pct)}% CO₂
            </p>
          </div>
        </div>
      </div>
      <div className="space-y-3 text-sm">
        <div className="flex flex-wrap gap-3">
          {data.map((d, i) => (
            <span key={d.name} className="flex items-center gap-1.5 text-xs">
              <span className="size-2.5 rounded-full" style={{ background: COLORS[i] }} /> {d.name} {num(d.value)}%
            </span>
          ))}
        </div>
        <dl className="grid gap-2 sm:grid-cols-2">
          <div className="rounded-lg bg-muted/50 p-3">
            <dt className="text-xs text-muted-foreground">Target range</dt>
            <dd className="font-medium">
              {map.mapTargetFound
                ? `O₂ ${num(map.targetO2Min)}–${num(map.targetO2Max)}% · CO₂ ${num(map.targetCo2Min)}–${num(map.targetCo2Max)}%`
                : "Generic target (no data for this food)"}
            </dd>
          </div>
          <div className="rounded-lg bg-muted/50 p-3">
            <dt className="flex items-center gap-1 text-xs text-muted-foreground">
              <Wind className="size-3" /> Breathable film
            </dt>
            <dd className="font-medium">
              {map.film} {map.filmThicknessUm} µm {map.perforationNeeded && "+ micro-perforations"}
            </dd>
          </div>
          <div className="rounded-lg bg-muted/50 p-3">
            <dt className="text-xs text-muted-foreground">Film OTR vs required</dt>
            <dd className="font-medium">
              {num(map.filmOtr)} ≥ {num(map.requiredOtr)}
            </dd>
          </div>
          <div className="rounded-lg bg-muted/50 p-3">
            <dt className="flex items-center gap-1 text-xs text-muted-foreground">
              <Thermometer className="size-3" /> Store at
            </dt>
            <dd className="font-medium">{map.storageTempC != null ? `${num(map.storageTempC)} °C` : "-"}</dd>
          </div>
        </dl>
        {map.perforationNeeded && (
          <p className="rounded-lg border border-brand/30 bg-brand/10 p-3 text-xs">
            No plain film breathes enough for this produce — ask your supplier for micro-perforated film so the produce does not
            suffocate.
          </p>
        )}
        {map.note && <p className="text-xs text-muted-foreground">{map.note}</p>}
        <ul className="space-y-1 text-xs text-muted-foreground">
          {map.reasons.map((r) => (
            <li key={r}>• {r}</li>
          ))}
        </ul>
      </div>
    </div>
  )
}
