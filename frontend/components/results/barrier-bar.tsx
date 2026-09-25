import { num } from "@/lib/format"
import { cn } from "@/lib/utils"

const DECADES_BELOW = 3
const DECADES_ABOVE = 1

/**
 * Log-scale bar: the dashed line is the requirement, the fill is the pack's value.
 * mode "max": value must be ≤ required (barrier). mode "min": value must be ≥ required (MAP breathability).
 */
export function BarrierBar({
  label,
  unit,
  value,
  required,
  mode = "max",
}: {
  label: string
  unit: string
  value: number
  required: number | null
  mode?: "max" | "min"
}) {
  if (required == null) {
    return (
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label}</span>
        <span>
          <b className="font-medium">{num(value)}</b> <span className="text-muted-foreground">· no limit</span>
        </span>
      </div>
    )
  }
  const lo = Math.log10(required) - DECADES_BELOW
  const hi = Math.log10(required) + DECADES_ABOVE
  const pos = (x: number) => Math.min(100, Math.max(1, ((Math.log10(Math.max(x, 1e-9)) - lo) / (hi - lo)) * 100))
  const ok = mode === "max" ? value <= required : value >= required
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label}</span>
        <span>
          <b className={cn("font-medium", ok ? "text-success" : "text-destructive")}>{num(value)}</b>
          <span className="text-muted-foreground">
            {" "}
            {mode === "max" ? "≤" : "≥"} {num(required)} {unit}
          </span>
        </span>
      </div>
      <div className="relative h-2 rounded-full bg-muted">
        <div className={cn("h-full rounded-full", ok ? "bg-success/80" : "bg-destructive/80")} style={{ width: `${pos(value)}%` }} />
        <div className="absolute -top-1 h-4 border-l-2 border-dashed border-foreground/60" style={{ left: `${pos(required)}%` }} title="required" />
      </div>
    </div>
  )
}
