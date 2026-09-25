"use client"

import { LANGS, useI18n } from "@/lib/i18n"
import { cn } from "@/lib/utils"

export function LanguageToggle() {
  const { lang, setLang } = useI18n()
  return (
    <div className="flex items-center gap-3" role="group" aria-label="Language">
      {LANGS.map((l) => (
        <button
          key={l.code}
          type="button"
          onClick={() => setLang(l.code)}
          aria-pressed={lang === l.code}
          title={l.label}
          className={cn(
            "text-sm text-muted-foreground transition-colors hover:text-foreground",
            lang === l.code && "font-semibold text-foreground",
          )}
        >
          {l.code === "en" ? "EN" : l.code === "hi" ? "हि" : "मराठी"}
        </button>
      ))}
    </div>
  )
}
