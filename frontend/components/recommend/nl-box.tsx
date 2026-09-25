"use client"

import { useState } from "react"
import { Loader2, Wand2 } from "lucide-react"
import { toast } from "sonner"
import { api } from "@/lib/api"
import type { ParseResponse } from "@/lib/types"
import { useI18n } from "@/lib/i18n"
import { AiTag } from "@/components/common/badges"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"

/** "Describe your product" - natural language (Hindi/Hinglish OK) → pre-filled wizard. */
export function NlBox({ onParsed }: { onParsed: (p: ParseResponse) => void }) {
  const { t } = useI18n()
  const [text, setText] = useState("")
  const [busy, setBusy] = useState(false)
  const [last, setLast] = useState<ParseResponse | null>(null)

  async function run() {
    if (!text.trim()) return
    setBusy(true)
    try {
      const res = await api.parse(text)
      setLast(res)
      onParsed(res)
      toast.success(
        res.missingFields.length ? `Filled what we understood — please check ${res.missingFields.length} highlighted field(s).` : "Form filled from your description.",
      )
    } catch (e) {
      toast.error((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-3">
      <Textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={t("wizard.describeHint")}
        rows={3}
        className="min-h-24 text-base"
        aria-label={t("wizard.describe")}
        onKeyDown={(e) => {
          if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) run()
        }}
      />
      <div className="flex flex-wrap items-center gap-2">
        <Button onClick={run} disabled={busy || !text.trim()}>
          {busy ? <Loader2 className="animate-spin" /> : <Wand2 />} Fill the form for me
        </Button>
        {last && (
          <span className="flex items-center gap-2 text-xs text-muted-foreground">
            {last.aiUsed ? <AiTag /> : <span className="rounded-full bg-muted px-2 py-0.5">keyword match</span>}
            {last.commodityName ? `Understood: ${last.commodityName}` : "Food not recognised"}
            {last.note ? ` · ${last.note}` : ""}
          </span>
        )}
      </div>
      <div className="flex flex-wrap gap-2">
        {["1 kg atta 3 mahine Nagpur mein", "500 gram paneer 10 din fridge", "chips 100g 90 days 30 degree 70% humidity"].map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => setText(s)}
            className="rounded-full border px-3 py-1 text-xs text-muted-foreground hover:border-primary hover:text-foreground"
          >
            {s}
          </button>
        ))}
      </div>
    </div>
  )
}
