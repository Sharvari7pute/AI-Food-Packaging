import Link from "next/link"
import { Logo } from "./logo"

export function SiteFooter() {
  return (
    <footer className="mt-16 border-t bg-secondary/40">
      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 py-10 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <div className="flex items-center gap-4">
          <Logo size="sm" />
          <div>
            <p className="font-semibold text-foreground">AnnKAVACH · PackSmart</p>
            <p>Smart India Hackathon · SIH26236 · Ministry of Food Processing Industries</p>
          </div>
        </div>
        <div className="flex flex-col gap-1 sm:items-end">
          <p>Prototype estimates from literature data — validate with lab shelf-life tests.</p>
          <p>
            <Link href="/methodology" className="text-foreground underline-offset-4 hover:underline">
              Methodology &amp; data sources
            </Link>
            {" · "}The engine decides, AI only explains.
          </p>
        </div>
      </div>
    </footer>
  )
}
