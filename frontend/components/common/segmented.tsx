"use client"

import { cn } from "@/lib/utils"
import type { ReactNode } from "react"

export function Segmented<T extends string>({
  value,
  onChange,
  options,
  className,
  ariaLabel,
  invalid,
}: {
  value: T | null
  onChange: (v: T) => void
  options: { value: T; label: ReactNode; icon?: ReactNode; testId?: string }[]
  className?: string
  ariaLabel: string
  invalid?: boolean
}) {
  return (
    <div
      role="radiogroup"
      aria-label={ariaLabel}
      className={cn(
        "grid gap-1 rounded-xl border bg-muted/50 p-1",
        invalid && "border-destructive ring-3 ring-destructive/20",
        className,
      )}
      style={{ gridTemplateColumns: `repeat(${options.length}, minmax(0, 1fr))` }}
    >
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={value === o.value}
          data-testid={o.testId}
          onClick={() => onChange(o.value)}
          className={cn(
            "flex items-center justify-center gap-1.5 rounded-lg px-2 py-2 text-sm font-medium text-muted-foreground transition-all",
            value === o.value ? "bg-background text-foreground shadow-sm ring-1 ring-border" : "hover:text-foreground",
          )}
        >
          {o.icon}
          {o.label}
        </button>
      ))}
    </div>
  )
}
