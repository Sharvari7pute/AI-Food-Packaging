"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import { Loader2, RotateCcw } from "lucide-react"
import { api, requestFromResult } from "@/lib/api"
import { useDebounced } from "@/lib/hooks"
import type { RecommendResponse } from "@/lib/types"
import { Button } from "@/components/ui/button"
import { Slider } from "@/components/ui/slider"

interface Knobs {
  temp: number
  rh: number
  days: number
  weight: number
}

const DEBOUNCE_MS = 350

/** Sliders → debounced /simulate (nothing saved). Calls onResult with the simulated result, or null when reset. */
export function WhatIf({ base, onResult }: { base: RecommendResponse; onResult: (r: RecommendResponse | null) => void }) {
  const initial: Knobs = useMemo(
    () => ({
      temp: base.inputs.storageTempC,
      rh: base.inputs.relativeHumidityPct,
      days: base.inputs.shelfLifeDays,
      weight: base.inputs.packWeightG,
    }),
    [base],
  )
  const [knobs, setKnobs] = useState<Knobs>(initial)
  const [sim, setSim] = useState<RecommendResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const debounced = useDebounced(knobs, DEBOUNCE_MS)
  const onResultRef = useRef(onResult)
  useEffect(() => {
    onResultRef.current = onResult
  })

  const changed = JSON.stringify(debounced) !== JSON.stringify(initial)

  useEffect(() => {
    if (!changed) {
      onResultRef.current(null)
      return
    }
    const ctrl = new AbortController()
    // eslint-disable-next-line react-hooks/set-state-in-effect -- request status for the new slider values
    setBusy(true)
    setError(null)
    const req = {
      ...requestFromResult(base),
      storageTempC: debounced.temp,
      relativeHumidityPct: debounced.rh,
      shelfLifeDays: debounced.days,
      packWeightG: debounced.weight,
      packAreaM2: null,
    }
    api
      .simulate(req, ctrl.signal)
      .then((r) => {
        setSim(r)
        onResultRef.current(r)
      })
      .catch((e: Error) => e.name !== "AbortError" && setError(e.message))
      .finally(() => !ctrl.signal.aborted && setBusy(false))
    return () => ctrl.abort()
  }, [debounced, changed, base])

  const baseTop = base.options[0]
  const shownSim = changed ? sim : null
  const simTop = shownSim?.options[0]
  let sentence: string | null = null
  if (shownSim && baseTop) {
    if (!simTop) sentence = "Under these conditions no pack meets every requirement — see near misses."
    else if (simTop.name !== baseTop.name)
      sentence = `The best pack changes to ${simTop.name} (${simTop.estimatedShelfLifeDays} days).`
    else if (simTop.estimatedShelfLifeDays !== baseTop.estimatedShelfLifeDays)
      sentence = `${simTop.name}: shelf life ${simTop.estimatedShelfLifeDays < baseTop.estimatedShelfLifeDays ? "drops" : "rises"} from ${baseTop.estimatedShelfLifeDays} to ${simTop.estimatedShelfLifeDays} days.`
    else sentence = `${simTop.name} still wins with ${simTop.estimatedShelfLifeDays} days.`
  }

  const set = (k: keyof Knobs) => (v: number | readonly number[]) => setKnobs((s) => ({ ...s, [k]: Array.isArray(v) ? v[0] : (v as number) }))

  return (
    <div className="space-y-5">
      <div className="grid gap-5 sm:grid-cols-2">
        <Knob label="Temperature" unit="°C" value={knobs.temp} min={-30} max={50} step={1} onChange={set("temp")} />
        <Knob label="Relative humidity" unit="%" value={knobs.rh} min={0} max={100} step={1} onChange={set("rh")} />
        <Knob label="Shelf life wanted" unit="days" value={knobs.days} min={1} max={730} step={1} onChange={set("days")} />
        <Knob label="Pack weight" unit="g" value={knobs.weight} min={10} max={5000} step={10} onChange={set("weight")} />
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <div className="min-h-9 flex-1 rounded-lg bg-muted/60 px-3 py-2 text-sm" aria-live="polite">
          {busy && changed ? (
            <span className="flex items-center gap-2 text-muted-foreground">
              <Loader2 className="size-4 animate-spin" /> Re-running the engine…
            </span>
          ) : error ? (
            <span className="text-destructive">{error}</span>
          ) : sentence ? (
            <span>
              At {knobs.temp} °C / {knobs.rh}% RH: <b>{sentence}</b>
            </span>
          ) : (
            <span className="text-muted-foreground">Move a slider — the cards and chart above update live (nothing is saved).</span>
          )}
        </div>
        <Button variant="outline" onClick={() => setKnobs(initial)} disabled={!changed}>
          <RotateCcw /> Reset
        </Button>
      </div>
    </div>
  )
}

function Knob({
  label,
  unit,
  value,
  min,
  max,
  step,
  onChange,
}: {
  label: string
  unit: string
  value: number
  min: number
  max: number
  step: number
  onChange: (v: number | readonly number[]) => void
}) {
  return (
    <div className="space-y-2.5">
      <div className="flex items-center justify-between text-sm">
        <span className="text-muted-foreground">{label}</span>
        <span className="font-semibold tabular-nums">
          {value} {unit}
        </span>
      </div>
      <Slider value={[value]} min={min} max={max} step={step} onValueChange={(v) => onChange(v)} aria-label={label} />
    </div>
  )
}
