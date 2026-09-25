/** Display helpers - numbers already come rounded from the engine; these only format. */
export function num(v: number | null | undefined, maxFrac = 2): string {
  if (v == null || Number.isNaN(v)) return "-"
  const abs = Math.abs(v)
  const digits = abs !== 0 && abs < 0.1 ? 4 : maxFrac
  return v.toLocaleString("en-IN", { maximumFractionDigits: digits })
}

export function inr(v: number | null | undefined): string {
  if (v == null) return "-"
  return "₹" + v.toLocaleString("en-IN", { maximumFractionDigits: 0 })
}

export function pct(v: number): string {
  return `${Math.round(v * 100)}%`
}

export function dateTime(iso: string | null | undefined): string {
  if (!iso) return ""
  return new Date(iso).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })
}

export function titleCase(s: string): string {
  return s
    .toLowerCase()
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ")
}

/** "oxidative_rancidity_and_moisture_uptake" → "Oxidative rancidity and moisture uptake" */
export function humanize(s: string | null | undefined): string {
  if (!s) return "-"
  const t = s.replace(/_/g, " ").trim()
  return t.charAt(0).toUpperCase() + t.slice(1)
}

export function tempRange(min: number | null | undefined, max: number | null | undefined): string | null {
  if (min == null || max == null) return null
  return min === max ? `${min} °C` : `${min} to ${max} °C`
}
