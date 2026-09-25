import Image from "next/image"
import Link from "next/link"
import { cn } from "@/lib/utils"

/** AnnKAVACH logo (image on a white tile so it reads well on cream and in dark mode). */
export function Logo({ className = "", size = "md" }: { className?: string; size?: "sm" | "md" }) {
  const h = size === "sm" ? 44 : 64
  return (
    <Link
      href="/"
      aria-label="AnnKAVACH - home"
      className={cn("inline-flex shrink-0 items-center rounded-md bg-white px-2 py-1 shadow-sm ring-1 ring-black/5", className)}
    >
      <Image
        src="/brand/annkavach-logo.png"
        alt="AnnKAVACH - Right Packaging for Every Food"
        width={Math.round((h * 488) / 418)}
        height={h}
        priority
        className="h-auto"
        style={{ height: h, width: "auto" }}
      />
    </Link>
  )
}
