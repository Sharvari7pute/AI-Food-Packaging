"use client"

import type { ReactNode } from "react"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

export function Field({
  label,
  htmlFor,
  hint,
  invalid,
  children,
  className,
}: {
  label: ReactNode
  htmlFor?: string
  hint?: ReactNode
  invalid?: boolean
  children: ReactNode
  className?: string
}) {
  return (
    <div className={cn("space-y-1.5", className)} data-invalid={invalid || undefined}>
      <Label htmlFor={htmlFor} className={cn(invalid && "text-destructive")}>
        {label}
      </Label>
      {children}
      {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  )
}

/** Number input that keeps an empty string as null. */
export function NumberInput({
  id,
  value,
  onChange,
  invalid,
  step = "any",
  min,
  max,
  placeholder,
  testId,
}: {
  id: string
  value: number | null | undefined
  onChange: (v: number | null) => void
  invalid?: boolean
  step?: string
  min?: number
  max?: number
  placeholder?: string
  testId?: string
}) {
  return (
    <Input
      id={id}
      type="number"
      inputMode="decimal"
      step={step}
      min={min}
      max={max}
      placeholder={placeholder}
      data-testid={testId}
      value={value ?? ""}
      aria-invalid={invalid || undefined}
      onChange={(e) => onChange(e.target.value === "" ? null : Number(e.target.value))}
      className="h-10"
    />
  )
}
