import type { Metadata, Viewport } from "next"
import { Geist, Geist_Mono, Noto_Sans_Devanagari } from "next/font/google"
import "katex/dist/katex.min.css"
import "./globals.css"
import { Providers } from "@/components/layout/providers"
import { SiteHeader } from "@/components/layout/site-header"
import { SiteFooter } from "@/components/layout/site-footer"
import { PackBotFab } from "@/components/chat/packbot-fab"

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin"] })
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] })
const devanagari = Noto_Sans_Devanagari({
  variable: "--font-devanagari",
  subsets: ["devanagari"],
  weight: ["400", "500", "600", "700"],
})

export const metadata: Metadata = {
  title: { default: "PackSmart — right pack, longer shelf life", template: "%s · PackSmart" },
  description:
    "Science-based food packaging recommendations for Indian MSMEs: barrier specs, thickness, shelf life, MAP, cost and CO₂ — with a QR-verified spec sheet.",
}

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f7fbf9" },
    { media: "(prefers-color-scheme: dark)", color: "#10172a" },
  ],
}

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={`${geistSans.variable} ${geistMono.variable} ${devanagari.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col">
        <Providers>
          <SiteHeader />
          <main className="flex-1">{children}</main>
          <SiteFooter />
          <PackBotFab />
        </Providers>
      </body>
    </html>
  )
}
