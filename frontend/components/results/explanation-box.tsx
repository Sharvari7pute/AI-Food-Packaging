"use client"

import { useEffect, useState } from "react"
import { Loader2 } from "lucide-react"
import { api } from "@/lib/api"
import { LANGS, useI18n } from "@/lib/i18n"
import type { Lang } from "@/lib/types"
import { AiTag } from "@/components/common/badges"
import { cn } from "@/lib/utils"

/** Plain-language explanation of the engine result, via /ai/explain (falls back to a template without AI). */
export function ExplanationBox({ id, shareId, initialLang }: { id: number | null; shareId: string | null; initialLang: Lang }) {
  const { lang: uiLang } = useI18n()
  const [lang, setLang] = useState<Lang>(initialLang ?? uiLang)
  const [text, setText] = useState<string | null>(null)
  const [aiUsed, setAiUsed] = useState(false)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    // eslint-disable-next-line react-hooks/set-state-in-effect -- loading state for a new request
    setBusy(true)
    setError(null)
    api
      .explain(id, shareId, lang)
      .then((r) => {
        if (cancelled) return
        setText(r.text)
        setAiUsed(r.aiUsed)
      })
      .catch((e: Error) => !cancelled && setError(e.message))
      .finally(() => !cancelled && setBusy(false))
    return () => {
      cancelled = true
    }
  }, [id, shareId, lang])

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          {aiUsed ? (
            <AiTag />
          ) : (
            <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground" title="AI unavailable — built from the engine's reasons">
              template
            </span>
          )}
        </div>
        <div className="flex rounded-lg border bg-muted/50 p-0.5" role="group" aria-label="Explanation language">
          {LANGS.map((l) => (
            <button
              key={l.code}
              type="button"
              onClick={() => setLang(l.code)}
              aria-pressed={lang === l.code}
              className={cn(
                "rounded-md px-2.5 py-1 text-xs font-medium text-muted-foreground",
                lang === l.code && "bg-background text-foreground shadow-sm",
              )}
            >
              {l.short}
            </button>
          ))}
        </div>
      </div>
      {busy ? (
        <div className="flex items-center gap-2 py-4 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" /> Writing a simple explanation…
        </div>
      ) : error ? (
        <p className="text-sm text-destructive">{error}</p>
      ) : (
        <p className="leading-relaxed">{text}</p>
      )}
      <p className="text-[11px] text-muted-foreground">Gemini explains the engine&apos;s numbers — it never picks materials or invents values.</p>
    </div>
  )
}
