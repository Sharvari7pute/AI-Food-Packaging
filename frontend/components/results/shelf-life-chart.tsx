"use client"

import { useState } from "react"
import { CartesianGrid, Legend, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts"
import type { OptionDto } from "@/lib/types"
import { cn } from "@/lib/utils"

/** O₂ used % and moisture used % over days against the 100 % spoilage line. */
export function ShelfLifeChart({ options, targetDays }: { options: OptionDto[]; targetDays: number }) {
  const [idx, setIdx] = useState(0)
  const option = options[Math.min(idx, options.length - 1)]
  if (!option) return null
  const hasO2 = option.curve.some((p) => p.o2UsedPct != null)
  const hasH2O = option.curve.some((p) => p.moistureUsedPct != null)
  const data = option.curve.map((p) => ({
    day: p.day,
    o2: p.o2UsedPct == null ? null : Math.min(p.o2UsedPct, 200),
    moisture: p.moistureUsedPct == null ? null : Math.min(p.moistureUsedPct, 200),
  }))

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-1.5" role="tablist" aria-label="Option">
        {options.map((o, i) => (
          <button
            key={o.name + i}
            type="button"
            role="tab"
            aria-selected={i === idx}
            onClick={() => setIdx(i)}
            className={cn(
              "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
              i === idx ? "border-primary bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground",
            )}
          >
            #{o.rank} {o.name}
          </button>
        ))}
      </div>
      {!hasO2 && !hasH2O ? (
        <p className="rounded-lg bg-muted/50 p-6 text-center text-sm text-muted-foreground">
          No oxygen or moisture limit applies to this food (e.g. fresh produce is limited by ripening), so the shelf life
          shown is the typical value for the food.
        </p>
      ) : (
        <div className="h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 10, right: 16, left: -8, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="day" minTickGap={24} tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} label={{ value: "days", position: "insideBottomRight", offset: -2, fontSize: 11 }} />
              <YAxis domain={[0, (max: number) => Math.max(120, Math.ceil(max / 20) * 20)]} tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} unit="%" />
              <Tooltip
                contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 12 }}
                formatter={(v) => `${Number(v).toFixed(1)}%`}
                labelFormatter={(d) => `Day ${d}`}
              />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <ReferenceLine y={100} stroke="var(--destructive)" strokeDasharray="6 4" label={{ value: "spoilage", position: "insideTopLeft", fontSize: 11, fill: "var(--destructive)" }} />
              <ReferenceLine x={nearestDay(data, targetDays)} stroke="var(--muted-foreground)" strokeDasharray="2 4" label={{ value: "target", position: "top", fontSize: 11 }} />
              {hasO2 && <Line type="monotone" dataKey="o2" name="O₂ used" stroke="var(--chart-2)" strokeWidth={2.5} dot={false} connectNulls />}
              {hasH2O && <Line type="monotone" dataKey="moisture" name="Moisture used" stroke="var(--chart-1)" strokeWidth={2.5} dot={false} connectNulls />}
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
      <p className="text-xs text-muted-foreground">
        The pack fails when a line crosses 100 %. {option.name} is estimated to last <b>{option.estimatedShelfLifeDays} days</b>
        {option.limitingFactor !== "NONE" ? `, limited by ${option.limitingFactor.toLowerCase()}` : ""}.
      </p>
    </div>
  )
}

function nearestDay(data: { day: number }[], target: number): number {
  let best = data[0]?.day ?? 0
  for (const p of data) if (Math.abs(p.day - target) < Math.abs(best - target)) best = p.day
  return best
}
