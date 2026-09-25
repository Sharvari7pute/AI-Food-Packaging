import { Recycle, Sparkles, Sprout, TriangleAlert } from "lucide-react"
import { cn } from "@/lib/utils"

/** Marks every AI-generated text. */
export function AiTag({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border border-violet-300/60 bg-violet-50 px-2 py-0.5 text-[11px] font-medium text-violet-700 dark:border-violet-400/30 dark:bg-violet-500/10 dark:text-violet-300",
        className,
      )}
      title="Written by AI from the engine's numbers"
    >
      <Sparkles className="size-3" /> AI
    </span>
  )
}

/** Marks approximate data (notes contain "approx" or "VERIFY"). */
export function ApproxBadge({ className, label = "approx" }: { className?: string; label?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full bg-warning/25 px-2 py-0.5 text-[11px] font-medium text-warning-foreground",
        className,
      )}
      title="Some values are approximate — verify with supplier datasheets"
    >
      <TriangleAlert className="size-3" /> {label}
    </span>
  )
}

export function EcoBadges({ recyclable, biodegradable }: { recyclable: boolean; biodegradable: boolean }) {
  return (
    <>
      {recyclable && (
        <span className="inline-flex items-center gap-1 rounded-full bg-success/15 px-2 py-0.5 text-[11px] font-medium text-success">
          <Recycle className="size-3" /> Recyclable
        </span>
      )}
      {biodegradable && (
        <span className="inline-flex items-center gap-1 rounded-full bg-success/15 px-2 py-0.5 text-[11px] font-medium text-success">
          <Sprout className="size-3" /> Biodegradable
        </span>
      )}
    </>
  )
}
