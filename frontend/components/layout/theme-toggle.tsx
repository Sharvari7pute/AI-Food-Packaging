"use client"

import { useTheme } from "next-themes"

/** Round toggle: a filled dot (light) / ring (dark), like the design. */
export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme()
  return (
    <button
      type="button"
      aria-label="Toggle dark mode"
      onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
      className="ml-2 grid size-9 place-items-center rounded-full border border-border bg-card shadow-sm transition-colors hover:border-foreground/30"
    >
      <span className="size-3.5 rounded-full bg-foreground transition-all dark:bg-transparent dark:ring-2 dark:ring-foreground" />
    </button>
  )
}
