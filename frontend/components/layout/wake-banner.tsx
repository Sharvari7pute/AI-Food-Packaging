"use client"

import { useEffect, useState } from "react"
import { Loader2 } from "lucide-react"
import { api, WAKING_EVENT } from "@/lib/api"

/**
 * Free hosting puts the server to sleep when idle. We ping it as soon as the app opens and show a calm banner
 * while any request is waiting for it to wake up (requests retry automatically).
 */
export function WakeBanner() {
  const [waking, setWaking] = useState(0)

  useEffect(() => {
    const onWake = (e: Event) => setWaking((n) => Math.max(0, n + ((e as CustomEvent<boolean>).detail ? 1 : -1)))
    window.addEventListener(WAKING_EVENT, onWake)
    api.health().catch(() => {})
    return () => window.removeEventListener(WAKING_EVENT, onWake)
  }, [])

  if (waking <= 0) return null
  return (
    <div role="status" aria-live="polite" className="sticky top-[76px] z-30 border-b border-brand/30 bg-[#fbeedd] text-[#7a4f1d] dark:bg-[#3a2c1c] dark:text-[#e7c79c]">
      <div className="mx-auto flex max-w-7xl items-center gap-2 px-4 py-2 text-sm sm:px-6">
        <Loader2 className="size-4 animate-spin" />
        Waking up the PackSmart server (free hosting sleeps when idle) — this can take up to a minute. Your request will continue automatically.
      </div>
    </div>
  )
}
