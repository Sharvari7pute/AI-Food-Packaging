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
