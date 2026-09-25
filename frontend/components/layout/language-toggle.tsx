"use client"

import { LANGS, useI18n } from "@/lib/i18n"
import { cn } from "@/lib/utils"

export function LanguageToggle() {
  const { lang, setLang } = useI18n()
  return (
    <div className="flex items-center rounded-lg border bg-muted/50 p-0.5" role="group" aria-label="Language">
      {LANGS.map((l) => (
        <button
          key={l.code}
          type="button"
          onClick={() => setLang(l.code)}
          aria-pressed={lang === l.code}
          title={l.label}
          className={cn(
            "rounded-md px-2 py-1 text-xs font-medium text-muted-foreground transition-colors",
            lang === l.code && "bg-background text-foreground shadow-sm",
          )}
        >
          {l.short}
        </button>
      ))}
    </div>
  )
}
