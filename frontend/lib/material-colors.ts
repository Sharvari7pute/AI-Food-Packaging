// Display colours for layer stacks (styling only - no material data lives here).
// Earthy tones that match the AnnKAVACH palette (sage, forest, sand, terracotta, olive, mist).
const PALETTE = ["#8fae86", "#4f7a62", "#d6c29a", "#c9785d", "#9aa65b", "#9fbcc0", "#b69cb0", "#6d8c8f", "#e0a458"]

function hash(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return h
}

export function materialColor(name: string, type?: string | null): string {
  const n = name.toLowerCase()
  if (type === "metal" || n.includes("alumin")) return "#a7adaa"
  if (n.includes("metal")) return "#7d8580"
  if (type === "paper" || n.includes("paper") || n.includes("kraft")) return "#c9a36b"
  if (type === "bio") return "#a9c79b"
  return PALETTE[hash(name) % PALETTE.length]
}
