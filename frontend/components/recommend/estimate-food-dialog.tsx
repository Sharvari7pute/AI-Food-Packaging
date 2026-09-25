"use client"

import { useState } from "react"
import { Loader2, Sparkles } from "lucide-react"
import { toast } from "sonner"
import { api } from "@/lib/api"
import type { CustomCommodity, EstimateFoodResponse } from "@/lib/types"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"

/** "Food not in list?" → AI estimate (clearly labelled) or a pointer to an existing food. */
export function EstimateFoodDialog({
  open,
  onOpenChange,
  onExisting,
  onEstimated,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  onExisting: (id: number) => void
  onEstimated: (food: CustomCommodity) => void
}) {
  const [name, setName] = useState("")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function run() {
    if (!name.trim()) return
    setBusy(true)
    setError(null)
    try {
      const res: EstimateFoodResponse = await api.estimateFood(name.trim())
      if (res.existingCommodityId) {
        toast.success(`"${res.food.name}" is already in our list — selected it.`)
        onExisting(res.existingCommodityId)
      } else {
        onEstimated(res.food)
      }
      onOpenChange(false)
      setName("")
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Food not in the list?</DialogTitle>
          <DialogDescription>
            Gemini can estimate typical properties (water activity, fat, sensitivity). You&apos;ll see an
            &quot;AI-estimated — verify&quot; banner and can edit every value before running the engine. The engine still
            makes every packaging decision.
          </DialogDescription>
        </DialogHeader>
        <form
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault()
            run()
          }}
        >
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Peanut chikki, Ghee, Khakhra" className="h-10" />
          <Button type="submit" disabled={busy || !name.trim()} className="h-10">
            {busy ? <Loader2 className="animate-spin" /> : <Sparkles />} Estimate
          </Button>
        </form>
        {error && <p className="rounded-lg bg-muted p-3 text-sm text-muted-foreground">{error}</p>}
      </DialogContent>
    </Dialog>
  )
}
