"use client"

import { useEffect, useRef, useState } from "react"
import { Bot, Loader2, Send, User } from "lucide-react"
import { toast } from "sonner"
import { api } from "@/lib/api"
import { useI18n } from "@/lib/i18n"
import type { ChatMessage } from "@/lib/types"
import { AiTag } from "@/components/common/badges"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

const SUGGESTIONS = [
  "Which pack keeps chips crispy for 3 months?",
  "What is the difference between OTR and WVTR?",
  "Is BOPP/CPP recyclable?",
  "Paneer ke liye kaunsa packet sahi hai?",
  "Why is aluminium foil never used alone?",
]

interface Msg extends ChatMessage {
  aiUsed?: boolean
}

export function ChatPanel({ recommendationId, compact = false }: { recommendationId?: number | null; compact?: boolean }) {
  const { lang } = useI18n()
  const [messages, setMessages] = useState<Msg[]>([])
  const [input, setInput] = useState("")
  const [busy, setBusy] = useState(false)
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" })
  }, [messages, busy])

  async function send(text: string) {
    const content = text.trim()
    if (!content || busy) return
    const next: Msg[] = [...messages, { role: "user", content }]
    setMessages(next)
    setInput("")
    setBusy(true)
    try {
      const res = await api.chat(
        next.map(({ role, content }) => ({ role, content })),
        lang,
        recommendationId,
      )
      setMessages([...next, { role: "assistant", content: res.reply, aiUsed: res.aiUsed }])
    } catch (e) {
      toast.error((e as Error).message)
      setMessages(messages)
      setInput(content)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className={cn("flex min-h-0 flex-1 flex-col", compact ? "h-full" : "h-[70vh] rounded-xl border bg-card shadow-sm")}>
      <div className="flex-1 space-y-4 overflow-y-auto p-4" aria-live="polite">
        {messages.length === 0 && (
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <div className="grid size-8 shrink-0 place-items-center rounded-full bg-primary/15 text-primary">
                <Bot className="size-4" />
              </div>
              <div className="rounded-2xl rounded-tl-sm bg-muted px-4 py-2.5 text-sm">
                Namaste! I&apos;m Pack-Bot. Ask me about films, laminates, barrier properties or your result. I only answer from
                PackSmart&apos;s database.
                {recommendationId ? " I can see your current recommendation." : ""}
              </div>
            </div>
            <div className="flex flex-wrap gap-2 pl-11">
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => send(s)}
                  className="rounded-full border bg-background px-3 py-1.5 text-left text-xs text-muted-foreground transition-colors hover:border-primary hover:text-foreground"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={cn("flex items-start gap-3", m.role === "user" && "flex-row-reverse")}>
            <div
              className={cn(
                "grid size-8 shrink-0 place-items-center rounded-full",
                m.role === "user" ? "bg-brand/15 text-brand" : "bg-primary/15 text-primary",
              )}
            >
              {m.role === "user" ? <User className="size-4" /> : <Bot className="size-4" />}
            </div>
            <div
              className={cn(
                "max-w-[85%] space-y-1.5 whitespace-pre-wrap rounded-2xl px-4 py-2.5 text-sm",
                m.role === "user" ? "rounded-tr-sm bg-brand text-brand-foreground" : "rounded-tl-sm bg-muted",
              )}
            >
              {m.role === "assistant" && m.aiUsed && <AiTag />}
              <p>{m.content}</p>
            </div>
          </div>
        ))}
        {busy && (
          <div className="flex items-center gap-2 pl-11 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" /> Pack-Bot is thinking…
          </div>
        )}
        <div ref={endRef} />
      </div>
      <form
        className="flex items-end gap-2 border-t p-3"
        onSubmit={(e) => {
          e.preventDefault()
          send(input)
        }}
      >
        <Textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault()
              send(input)
            }
          }}
          placeholder="Ask about packaging…"
          rows={1}
          className="max-h-32 min-h-10 resize-none"
          aria-label="Message"
        />
        <Button type="submit" size="icon-lg" disabled={busy || !input.trim()} aria-label="Send">
          <Send />
        </Button>
      </form>
    </div>
  )
}
