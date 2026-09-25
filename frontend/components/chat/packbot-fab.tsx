"use client"

import { usePathname } from "next/navigation"
import { useState } from "react"
import { Bot } from "lucide-react"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { ChatPanel } from "./chat-panel"

/** Floating Pack-Bot button on every page; passes the current result as context on /results/[id]. */
export function PackBotFab() {
  const pathname = usePathname()
  const [open, setOpen] = useState(false)
  if (pathname.startsWith("/chat")) return null
  const match = pathname.match(/^\/results\/(\d+)/)
  const recommendationId = match ? Number(match[1]) : null

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          <button
            type="button"
            className="fixed right-4 bottom-4 z-40 flex items-center gap-3 rounded-2xl bg-forest px-4 py-3 text-forest-foreground shadow-[0_18px_40px_-12px_rgba(27,61,47,0.6)] ring-1 ring-white/10 transition-transform hover:-translate-y-0.5 sm:right-6 sm:bottom-6 dark:bg-card dark:text-foreground dark:ring-border"
            aria-label="Ask Pack-Bot"
          />
        }
      >
        <Bot className="size-5" />
        <span className="text-left leading-tight">
          <span className="block text-[9px] font-semibold tracking-[0.2em] opacity-70">ASK</span>
          <span className="block text-sm font-semibold">Pack-Bot</span>
        </span>
      </SheetTrigger>
      <SheetContent side="right" className="w-full gap-0 p-0 sm:max-w-md">
        <SheetHeader className="border-b">
          <SheetTitle>Pack-Bot</SheetTitle>
          <SheetDescription>Answers from PackSmart&apos;s materials and foods database.</SheetDescription>
        </SheetHeader>
        <ChatPanel recommendationId={recommendationId} compact />
      </SheetContent>
    </Sheet>
  )
}
