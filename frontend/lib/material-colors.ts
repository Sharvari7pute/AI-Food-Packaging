// Display colours for layer stacks (styling only - no material data lives here).
const PALETTE = ["#38bdf8", "#34d399", "#a78bfa", "#f472b6", "#fbbf24", "#60a5fa", "#2dd4bf", "#fb7185", "#c084fc"]

function hash(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return h
}

export function materialColor(name: string, type?: string | null): string {
  const n = name.toLowerCase()
  if (type === "metal" || n.includes("alumin")) return "#94a3b8"
  if (n.includes("metal")) return "#64748b"
  if (type === "paper" || n.includes("paper") || n.includes("kraft")) return "#d6a86c"
  if (type === "bio") return "#86efac"
  return PALETTE[hash(name) % PALETTE.length]
}
