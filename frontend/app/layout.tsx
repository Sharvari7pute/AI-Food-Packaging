import type { Metadata, Viewport } from "next"
import { Geist_Mono, Instrument_Serif, Inter_Tight, Noto_Sans_Devanagari } from "next/font/google"
import "katex/dist/katex.min.css"
import "./globals.css"
import { Providers } from "@/components/layout/providers"
import { SiteHeader } from "@/components/layout/site-header"
import { TopBar } from "@/components/layout/top-bar"
import { WakeBanner } from "@/components/layout/wake-banner"
import { SiteFooter } from "@/components/layout/site-footer"
import { PackBotFab } from "@/components/chat/packbot-fab"

const interTight = Inter_Tight({ variable: "--font-inter-tight", subsets: ["latin"] })
const instrumentSerif = Instrument_Serif({ variable: "--font-instrument-serif", subsets: ["latin"], weight: "400", style: ["normal", "italic"] })
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] })
const devanagari = Noto_Sans_Devanagari({
  variable: "--font-devanagari",
  subsets: ["devanagari"],
  weight: ["400", "500", "600", "700"],
})

export const metadata: Metadata = {
  title: { default: "AnnKAVACH PackSmart — right pack, longer shelf life", template: "%s · AnnKAVACH PackSmart" },
  description:
    "Science-based food packaging recommendations for Indian MSMEs: barrier specs, thickness, shelf life, MAP, cost and CO₂ — with a QR-verified spec sheet.",
}

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#1b3d2f" },
    { media: "(prefers-color-scheme: dark)", color: "#0f1512" },
  ],
}

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={`${interTight.variable} ${instrumentSerif.variable} ${geistMono.variable} ${devanagari.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col">
        <Providers>
          <TopBar />
          <SiteHeader />
          <WakeBanner />
          <main className="flex-1">{children}</main>
          <SiteFooter />
          <PackBotFab />
        </Providers>
      </body>
    </html>
  )
}
