"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { Menu } from "lucide-react"
import { Logo } from "./logo"
import { ThemeToggle } from "./theme-toggle"
import { LanguageToggle } from "./language-toggle"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { useI18n, type DictKey } from "@/lib/i18n"
import { cn } from "@/lib/utils"

const NAV: { href: string; key: DictKey }[] = [
  { href: "/recommend", key: "nav.recommend" },
  { href: "/compare", key: "nav.compare" },
  { href: "/builder", key: "nav.builder" },
  { href: "/materials", key: "nav.materials" },
  { href: "/chat", key: "nav.chat" },
  { href: "/history", key: "nav.history" },
  { href: "/methodology", key: "nav.methodology" },
]

export function SiteHeader() {
  const { t } = useI18n()
  const pathname = usePathname()
  const [open, setOpen] = useState(false)
  const active = (href: string) => pathname.startsWith(href) || (href === "/recommend" && pathname === "/")

  return (
    <header className="sticky top-0 z-40 border-b bg-background/90 backdrop-blur supports-[backdrop-filter]:bg-background/80">
      <div className="mx-auto flex h-[76px] max-w-7xl items-center gap-4 px-4 sm:px-6">
        <Logo size="sm" className="sm:hidden" />
        <Logo className="hidden sm:inline-flex" />
        <nav className="ml-auto hidden items-center gap-6 xl:flex" aria-label="Main">
          {NAV.map((n) => (
            <Link
              key={n.href}
              href={n.href}
              className={cn(
                "relative py-2 text-sm text-muted-foreground transition-colors hover:text-foreground",
                active(n.href) &&
                  "text-foreground after:absolute after:inset-x-0 after:-bottom-0.5 after:h-0.5 after:rounded-full after:bg-brand",
              )}
            >
              {t(n.key)}
            </Link>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-2 xl:ml-4 xl:border-l xl:pl-5">
          <LanguageToggle />
          <ThemeToggle />
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger render={<Button variant="ghost" size="icon" className="xl:hidden" aria-label="Open menu" />}>
              <Menu />
            </SheetTrigger>
            <SheetContent side="right" className="w-72">
              <SheetHeader>
                <SheetTitle>
                  <Logo size="sm" />
                </SheetTitle>
              </SheetHeader>
              <nav className="flex flex-col gap-1 px-4" aria-label="Mobile">
                {NAV.map((n) => (
                  <Link
                    key={n.href}
                    href={n.href}
                    onClick={() => setOpen(false)}
                    className={cn(
                      "rounded-md px-3 py-2.5 text-sm font-medium hover:bg-muted",
                      active(n.href) && "bg-accent text-accent-foreground",
                    )}
                  >
                    {t(n.key)}
                  </Link>
                ))}
              </nav>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  )
}
