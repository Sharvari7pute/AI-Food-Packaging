"use client"

import { Bot, Download, GitCompare, QrCode, Share2 } from "lucide-react"
import { toast } from "sonner"
import { pdfUrl, qrUrl } from "@/lib/api"
import { useI18n } from "@/lib/i18n"
import { Button, buttonVariants } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { LinkButton } from "@/components/common/link-button"
import { cn } from "@/lib/utils"

export function ResultActions({ id, shareId }: { id: number; shareId: string }) {
  const { t } = useI18n()
  const verifyUrl = typeof window !== "undefined" ? `${window.location.origin}/report/${shareId}` : `/report/${shareId}`

  async function share() {
    try {
      if (navigator.share) {
        await navigator.share({ title: "PackSmart packaging spec", url: verifyUrl })
        return
      }
      await navigator.clipboard.writeText(verifyUrl)
      toast.success("Link copied", { description: verifyUrl })
    } catch {
      /* user cancelled */
    }
  }

  return (
    <div className="flex flex-wrap gap-2">
      <a href={pdfUrl(shareId)} className={cn(buttonVariants({ size: "lg" }))} data-testid="download-pdf">
        <Download /> {t("results.pdf")}
      </a>
      <Dialog>
        <DialogTrigger render={<Button variant="outline" size="lg" />}>
          <QrCode /> {t("results.qr")}
        </DialogTrigger>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Scan to verify</DialogTitle>
            <DialogDescription>Opens the read-only spec, so your supplier can check it.</DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @next/next/no-img-element -- PNG served by the backend */}
          <img src={qrUrl(shareId)} alt="QR code linking to the verified spec" className="mx-auto size-60 rounded-lg bg-white p-2" />
          <p className="break-all text-center text-xs text-muted-foreground">{verifyUrl}</p>
        </DialogContent>
      </Dialog>
      <LinkButton href={`/compare?rec=${id}`} variant="outline" size="lg">
        <GitCompare /> {t("results.compare")}
      </LinkButton>
      <LinkButton href={`/chat?rec=${id}`} variant="outline" size="lg">
        <Bot /> {t("results.ask")}
      </LinkButton>
      <Button variant="outline" size="lg" onClick={share}>
        <Share2 /> {t("results.share")}
      </Button>
    </div>
  )
}
