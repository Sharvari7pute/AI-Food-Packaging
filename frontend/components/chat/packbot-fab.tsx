"use client"

import { usePathname } from "next/navigation"
import { useState } from "react"
import { MessageCircle } from "lucide-react"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
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
          <Button
            size="lg"
            className="fixed right-4 bottom-4 z-40 h-12 rounded-full px-4 shadow-lg shadow-primary/30 sm:right-6 sm:bottom-6"
            aria-label="Open Pack-Bot"
          />
        }
      >
        <MessageCircle /> <span className="hidden sm:inline">Pack-Bot</span>
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
