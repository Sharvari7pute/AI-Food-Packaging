"use client"

import { ArrowRight, Atom, Coins, FileCheck2, Leaf, MessageSquareText, Snowflake, Thermometer, Wind } from "lucide-react"
import { LinkButton } from "@/components/common/link-button"
import { useI18n } from "@/lib/i18n"

const LAYERS = [
  { name: "PET", um: 12, cls: "bg-sky-400" },
  { name: "Aluminium foil", um: 9, cls: "bg-slate-400" },
  { name: "LDPE", um: 50, cls: "bg-emerald-400" },
]

export function HomeContent() {
  const { t } = useI18n()
  const steps = [
    { icon: MessageSquareText, title: t("home.step1"), text: t("home.step1d") },
    { icon: Thermometer, title: t("home.step2"), text: t("home.step2d") },
    { icon: FileCheck2, title: t("home.step3"), text: t("home.step3d") },
  ]
  const features = [
    { icon: Atom, title: t("home.f1"), text: t("home.f1d") },
    { icon: Wind, title: t("home.f2"), text: t("home.f2d") },
    { icon: Coins, title: t("home.f3"), text: t("home.f3d") },
  ]
  const stats = [
    { value: "~30%", label: "of packaged snacks lose crispness early in humid months (example)" },
    { value: "2–3×", label: "longer shelf life when the barrier matches the food (example)" },
    { value: "₹0", label: "to use — no lab needed to get a first spec (example)" },
  ]

  return (
    <div>
      <section className="relative overflow-hidden border-b">
        <div className="bg-grid absolute inset-0 [mask-image:radial-gradient(ellipse_at_top,black_30%,transparent_75%)]" />
        <div className="absolute -top-24 left-1/2 size-[36rem] -translate-x-1/2 rounded-full bg-primary/15 blur-3xl" />
        <div className="relative mx-auto grid max-w-7xl gap-12 px-4 py-16 sm:px-6 lg:grid-cols-[1.15fr_1fr] lg:py-24">
          <div className="flex flex-col justify-center gap-6">
            <span className="w-fit rounded-full border bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground">
              {t("home.badge")}
            </span>
            <h1 className="text-balance text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
              {t("home.title")}
            </h1>
            <p className="max-w-xl text-lg text-muted-foreground">{t("home.subtitle")}</p>
            <div className="flex flex-wrap gap-3">
              <LinkButton href="/recommend" size="lg" className="h-11 px-5 text-base">
                {t("home.cta")} <ArrowRight />
              </LinkButton>
              <LinkButton href="/methodology" variant="outline" size="lg" className="h-11 px-5 text-base">
                {t("home.cta2")}
              </LinkButton>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-md" aria-hidden>
            <div className="rotate-1 rounded-2xl border bg-card p-5 shadow-xl shadow-brand/10">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-muted-foreground">Chips · 100 g · 90 days · 30 °C</p>
                  <p className="text-lg font-semibold">PET / AL / PE</p>
                </div>
                <span className="rounded-full bg-primary px-2.5 py-1 text-xs font-semibold text-primary-foreground">#1</span>
              </div>
              <div className="mt-4 flex h-5 overflow-hidden rounded-md">
                {LAYERS.map((l) => (
                  <div key={l.name} className={l.cls} style={{ width: `${(l.um / 71) * 100}%` }} title={l.name} />
                ))}
              </div>
              <div className="mt-1 flex justify-between text-[10px] text-muted-foreground">
                {LAYERS.map((l) => (
                  <span key={l.name}>
                    {l.name} {l.um} µm
                  </span>
                ))}
              </div>
              <div className="mt-5 grid grid-cols-3 gap-3 text-center">
                <div className="rounded-lg bg-muted p-2">
                  <p className="text-lg font-semibold">350+</p>
                  <p className="text-[11px] text-muted-foreground">days shelf life</p>
                </div>
                <div className="rounded-lg bg-muted p-2">
                  <p className="text-lg font-semibold">₹1.1k</p>
                  <p className="text-[11px] text-muted-foreground">per 1000 packs</p>
                </div>
                <div className="rounded-lg bg-muted p-2">
                  <p className="text-lg font-semibold">0.014</p>
                  <p className="text-[11px] text-muted-foreground">OTR vs 0.054 req.</p>
                </div>
              </div>
              <div className="mt-4 rounded-lg border border-destructive/30 bg-destructive/5 p-2.5 text-xs text-destructive">
                Avoid plain LDPE 50 µm — oxygen gets in 70,000× faster than allowed.
              </div>
            </div>
            <div className="absolute -bottom-6 -left-6 hidden -rotate-3 rounded-xl border bg-card p-3 shadow-lg sm:block">
              <div className="flex items-center gap-2 text-xs">
                <Snowflake className="size-4 text-brand" /> MAP for tomatoes: 4% O₂ · 4% CO₂
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
        <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">{t("home.how")}</h2>
        <div className="mt-8 grid gap-6 md:grid-cols-3">
          {steps.map((s, i) => (
            <div key={s.title} className="relative rounded-2xl border bg-card p-6 shadow-sm">
              <span className="absolute top-5 right-5 text-4xl font-bold text-muted/80 dark:text-muted">{i + 1}</span>
              <s.icon className="size-8 text-primary" />
              <h3 className="mt-4 font-semibold">{s.title}</h3>
              <p className="mt-1.5 text-sm text-muted-foreground">{s.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="border-y bg-muted/30">
        <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
          <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">{t("home.features")}</h2>
          <div className="mt-8 grid gap-6 md:grid-cols-3">
            {features.map((f) => (
              <div key={f.title} className="rounded-2xl border bg-card p-6 shadow-sm transition-shadow hover:shadow-md">
                <div className="grid size-11 place-items-center rounded-xl bg-gradient-to-br from-primary/20 to-brand/20">
                  <f.icon className="size-5 text-primary" />
                </div>
                <h3 className="mt-4 font-semibold">{f.title}</h3>
                <p className="mt-1.5 text-sm text-muted-foreground">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
        <div className="flex items-center gap-2">
          <Leaf className="size-5 text-primary" />
          <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">{t("home.impact")}</h2>
        </div>
        <div className="mt-8 grid gap-6 sm:grid-cols-3">
          {stats.map((s) => (
            <div key={s.value} className="rounded-2xl border bg-gradient-to-br from-card to-accent/40 p-6">
              <p className="text-4xl font-bold text-brand">{s.value}</p>
              <p className="mt-2 text-sm text-muted-foreground">{s.label}</p>
            </div>
          ))}
        </div>
        <p className="mt-4 text-xs text-muted-foreground">{t("home.impactNote")}</p>
        <div className="mt-10 flex flex-col items-start gap-4 rounded-2xl bg-gradient-to-r from-brand to-primary p-8 text-white sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xl font-semibold">Ready in under a minute.</p>
            <p className="text-sm text-white/80">No login. Works in English, हिंदी and मराठी.</p>
          </div>
          <LinkButton href="/recommend" size="lg" variant="secondary" className="h-11 px-5">
            {t("home.cta")} <ArrowRight />
          </LinkButton>
        </div>
      </section>
    </div>
  )
}
