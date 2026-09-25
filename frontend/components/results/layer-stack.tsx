import type { LayerDto } from "@/lib/types"
import { materialColor } from "@/lib/material-colors"
import { num } from "@/lib/format"

/** Coloured horizontal bars proportional to layer thickness (outside → inside). */
export function LayerStack({ layers, types }: { layers: LayerDto[]; types?: Record<string, string> }) {
  const total = layers.reduce((s, l) => s + l.thicknessUm, 0) || 1
  return (
    <div className="space-y-1.5">
      <div className="flex h-6 overflow-hidden rounded-md ring-1 ring-border" role="img" aria-label="Layer structure">
        {layers.map((l, i) => (
          <div
            key={i}
            className="h-full border-r border-background/60 last:border-r-0"
            style={{ width: `${Math.max((l.thicknessUm / total) * 100, 4)}%`, background: materialColor(l.material, types?.[l.material]) }}
            title={`${l.material} ${l.thicknessUm} µm`}
          />
        ))}
      </div>
      <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
        {layers.map((l, i) => (
          <span key={i} className="flex items-center gap-1">
            <span className="size-2 rounded-sm" style={{ background: materialColor(l.material, types?.[l.material]) }} />
            {l.material} <b className="font-medium text-foreground">{num(l.thicknessUm)} µm</b>
          </span>
        ))}
        <span className="ml-auto text-[11px]">outside → inside</span>
      </div>
    </div>
  )
}
