"use client"

import { useMemo, useRef, useState } from "react"
import { Check, ChevronsUpDown, Search } from "lucide-react"
import type { CommoditySummary } from "@/lib/types"
import { cn } from "@/lib/utils"

/** Searchable food dropdown (matches English and Hindi names). */
export function FoodCombobox({
  foods,
  value,
  onChange,
  placeholder,
  invalid,
}: {
  foods: CommoditySummary[]
  value: number | null
  onChange: (id: number) => void
  placeholder: string
  invalid?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")
  const [active, setActive] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)
  const selected = foods.find((f) => f.id === value)

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return foods
    return foods.filter(
      (f) => f.name.toLowerCase().includes(q) || (f.nameHi ?? "").includes(query.trim()) || (f.category ?? "").includes(q),
    )
  }, [foods, query])

  function pick(f: CommoditySummary) {
    onChange(f.id)
    setOpen(false)
    setQuery("")
  }

  return (
    <div className="relative">
      <button
        type="button"
        data-testid="food-combobox"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => {
          setOpen((o) => !o)
          setTimeout(() => inputRef.current?.focus(), 0)
        }}
        className={cn(
          "flex h-11 w-full items-center justify-between rounded-lg border bg-background px-3 text-left text-sm shadow-xs transition-colors hover:bg-muted/50 dark:bg-input/30",
          invalid && "border-destructive ring-3 ring-destructive/20",
        )}
      >
        {selected ? (
          <span className="flex items-center gap-2">
            <span className="font-medium">{selected.name}</span>
            {selected.nameHi && <span className="text-muted-foreground">{selected.nameHi}</span>}
          </span>
        ) : (
          <span className="text-muted-foreground">{placeholder}</span>
        )}
        <ChevronsUpDown className="size-4 text-muted-foreground" />
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} aria-hidden />
          <div className="absolute z-50 mt-1 w-full overflow-hidden rounded-lg border bg-popover shadow-lg">
            <div className="flex items-center gap-2 border-b px-3">
              <Search className="size-4 text-muted-foreground" />
              <input
                ref={inputRef}
                value={query}
                data-testid="food-search"
                onChange={(e) => {
                  setQuery(e.target.value)
                  setActive(0)
                }}
                onKeyDown={(e) => {
                  if (e.key === "ArrowDown") {
                    e.preventDefault()
                    setActive((a) => Math.min(a + 1, filtered.length - 1))
                  } else if (e.key === "ArrowUp") {
                    e.preventDefault()
                    setActive((a) => Math.max(a - 1, 0))
                  } else if (e.key === "Enter" && filtered[active]) {
                    e.preventDefault()
                    pick(filtered[active])
                  } else if (e.key === "Escape") {
                    setOpen(false)
                  }
                }}
                placeholder={placeholder}
                className="h-10 w-full bg-transparent text-sm outline-none"
              />
            </div>
            <ul role="listbox" className="max-h-64 overflow-y-auto p-1">
              {filtered.length === 0 && <li className="px-3 py-6 text-center text-sm text-muted-foreground">No match</li>}
              {filtered.map((f, i) => (
                <li
                  key={f.id}
                  role="option"
                  aria-selected={f.id === value}
                  data-testid={`food-option-${f.name}`}
                  onMouseEnter={() => setActive(i)}
                  onClick={() => pick(f)}
                  className={cn(
                    "flex cursor-pointer items-center justify-between rounded-md px-3 py-2 text-sm",
                    i === active && "bg-muted",
                  )}
                >
                  <span className="flex items-center gap-2">
                    <span className="font-medium">{f.name}</span>
                    {f.nameHi && <span className="text-muted-foreground">{f.nameHi}</span>}
                  </span>
                  <span className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground">{f.category?.replace("_", " ")}</span>
                    {f.id === value && <Check className="size-4 text-primary" />}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </div>
  )
}
