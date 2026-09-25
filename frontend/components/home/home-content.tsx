"use client"

import Image from "next/image"
import { ArrowDownRight, ArrowRight, BadgeCheck, CircleAlert, Leaf, MessageSquareText, ShieldCheck, Sparkles, Thermometer, Wind, FileCheck2, Recycle, Timer } from "lucide-react"
import { LinkButton } from "@/components/common/link-button"
import { useI18n } from "@/lib/i18n"

const LAYERS = [
  { name: "PET", um: 12, cls: "bg-[#b9c9ae]" },
  { name: "Aluminium foil", um: 9, cls: "bg-[#c58a47]" },
  { name: "LDPE", um: 50, cls: "bg-[#e6e1d3]" },
]

const FEATURES = [
  { icon: ShieldCheck, title: "Physics-Based Engine", text: "Barrier calculations backed by OTR, WVTR and Q10." },
  { icon: Wind, title: "Fresh Produce (MAP)", text: "Breathable film and target gas mix for fruits and vegetables." },
  { icon: Leaf, title: "Cost & Sustainability", text: "₹ per 1000 packs, CO₂e and recyclability." },
  { icon: BadgeCheck, title: "Verified Specifications", text: "Supplier-ready packaging specifications with QR." },
  { icon: Sparkles, title: "Shelf-Life Focused", text: "Packaging selected around food and storage conditions." },
]

export function HomeContent() {
  const { t } = useI18n()
  const steps = [
    { icon: MessageSquareText, title: t("home.step1"), text: t("home.step1d") },
    { icon: Thermometer, title: t("home.step2"), text: t("home.step2d") },
    { icon: FileCheck2, title: t("home.step3"), text: t("home.step3d") },
  ]
  const stats = [
    { icon: Timer, value: "~30%", label: "of packaged snacks lose crispness early in humid months" },
    { icon: Recycle, value: "2–3×", label: "longer shelf life when the barrier matches the food" },
    { icon: BadgeCheck, value: "₹0", label: "to get a first, lab-ready packaging spec" },
  ]

  return (
    <div>
      {/* ---------------------------------------------------------------- hero */}
      <section className="relative overflow-hidden">
        <div className="mx-auto grid max-w-7xl items-center gap-14 px-4 pt-14 pb-20 sm:px-6 lg:grid-cols-[1.05fr_1fr] lg:pt-24 lg:pb-28">
          <div className="flex flex-col gap-7">
            <p className="eyebrow flex items-center gap-3">
              <span className="size-2 rounded-full bg-brand" /> {t("home.eyebrow")}
            </p>
            <h1 className="text-[3.2rem] leading-[0.95] font-semibold tracking-[-0.045em] text-foreground sm:text-7xl lg:text-[5.4rem]">
              <span className="block">{t("home.t1")}</span>
              <span className="block font-serif text-[1.08em] font-normal tracking-[-0.02em] text-sage italic">
                {t("home.t2")}
              </span>
              <span className="block">{t("home.t3")}</span>
            </h1>
            <p className="max-w-xl text-lg leading-relaxed text-muted-foreground">{t("home.subtitle")}</p>
            <div className="flex flex-wrap gap-4">
              <LinkButton href="/recommend" size="lg" className="h-14 gap-3 rounded-md px-6 text-base">
                {t("home.cta")} <ArrowRight className="size-4" />
              </LinkButton>
              <LinkButton href="/methodology" variant="outline" size="lg" className="h-14 gap-3 rounded-md border-foreground/25 bg-transparent px-6 text-base">
                {t("home.cta2")} <ArrowDownRight className="size-4" />
              </LinkButton>
            </div>
            <p className="flex items-center gap-2 text-sm text-muted-foreground">
              <ShieldCheck className="size-4 text-sage" /> {t("home.builtFor")}
            </p>
          </div>

          {/* photo + spec card */}
          <div className="relative mx-auto w-full max-w-[560px]" aria-hidden>
            <div className="relative ml-auto w-[72%] sm:w-[68%]">
              <div className="relative aspect-[420/700] overflow-hidden rounded-tl-[42%_26%] rounded-br-[42%_30%] shadow-[0_30px_60px_-30px_rgba(27,61,47,0.45)]">
                <Image src="/brand/hero-chips-v2.jpg" alt="" fill priority sizes="(max-width: 1024px) 70vw, 380px" className="object-cover" />
              </div>
              {/* round badge covers the photo's top-right corner */}
              <div className="absolute top-[6%] left-[87%] grid aspect-square w-[38%] -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-[#dce8d5] text-[#1b3d2f] shadow-lg ring-4 ring-background">
                <BadgeText />
              </div>
            </div>
            <div className="relative z-10 -mt-24 w-[92%] rounded-xl border bg-card p-5 shadow-[0_24px_50px_-20px_rgba(27,61,47,0.35)] sm:absolute sm:top-[18%] sm:left-0 sm:mt-0 sm:w-[64%] sm:max-w-[340px] sm:p-6">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="eyebrow text-[10px]">PackSmart spec</p>
                  <p className="mt-1 text-sm font-semibold">Chips / 100 g · 90 days · 30°C</p>
                </div>
                <span className="grid size-9 place-items-center rounded-full bg-accent text-xs font-bold">#1</span>
              </div>
              <div className="my-4 border-t" />
              <div className="flex items-end justify-between gap-3">
                <div>
                  <p className="eyebrow text-[10px]">Recommended laminate</p>
                  <p className="mt-1 text-xl font-semibold tracking-tight">PET / AL / PE</p>
                </div>
                <div className="text-right">
                  <p className="text-2xl font-semibold">350+</p>
                  <p className="text-[11px] tracking-wide text-muted-foreground">days shelf life</p>
                </div>
              </div>
              <div className="my-4 border-t" />
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <p className="eyebrow text-[10px]">Pack cost</p>
                  <p className="mt-1 text-sm font-semibold">
                    ₹1.1k <span className="text-[10px] font-normal text-muted-foreground">per 1000 packs</span>
                  </p>
                </div>
                <div>
                  <p className="eyebrow text-[10px]">Oxygen barrier</p>
                  <p className="mt-1 text-sm font-semibold">
                    0.014 <span className="text-[10px] font-normal text-muted-foreground">OTR vs 0.054 req.</span>
                  </p>
                </div>
              </div>
              <div className="my-4 border-t" />
              <ul className="space-y-2 text-sm">
                {LAYERS.map((l) => (
                  <li key={l.name} className="flex items-center gap-2.5">
                    <span className={`size-3 rounded-sm ${l.cls}`} /> {l.name} {l.um} µm
                  </li>
                ))}
              </ul>
              <p className="mt-4 flex gap-2 rounded-md bg-[#fbeedd] p-3 text-xs text-[#8a5a22] dark:bg-[#3a2c1c] dark:text-[#e7c79c]">
                <CircleAlert className="size-4 shrink-0" /> Avoid plain LDPE 50 µm — oxygen gets in 70,000× faster than allowed.
              </p>
              <p className="mt-3 text-xs text-muted-foreground">
                MAP for tomatoes: <b className="text-foreground">4% O₂ · 4% CO₂</b>
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ---------------------------------------------------------------- feature band */}
      <section className="bg-forest text-forest-foreground">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:grid-cols-2 sm:px-6 lg:grid-cols-5">
          {FEATURES.map((f) => (
            <div key={f.title} className="flex gap-3">
              <span className="grid size-9 shrink-0 place-items-center rounded-full bg-white/12">
                <f.icon className="size-4" />
              </span>
              <div>
                <p className="text-sm font-semibold">{f.title}</p>
                <p className="mt-1 text-[13px] leading-snug opacity-75">{f.text}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ---------------------------------------------------------------- how it works */}
      <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <p className="eyebrow flex items-center gap-3">
          <span className="size-2 rounded-full bg-brand" /> {t("home.how")}
        </p>
        <h2 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">
          Three steps to a <span className="font-serif font-normal text-sage italic">verified</span> pack
        </h2>
        <div className="mt-10 grid gap-6 md:grid-cols-3">
          {steps.map((s, i) => (
            <div key={s.title} className="relative rounded-xl border bg-card p-7 shadow-sm">
              <span className="font-serif text-5xl text-brand italic">0{i + 1}</span>
              <s.icon className="absolute top-7 right-7 size-6 text-sage" />
              <h3 className="mt-4 text-lg font-semibold">{s.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{s.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ---------------------------------------------------------------- impact */}
      <section className="border-y bg-secondary/40">
        <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6">
          <p className="eyebrow flex items-center gap-3">
            <span className="size-2 rounded-full bg-brand" /> {t("home.impact")}
          </p>
          <div className="mt-8 grid gap-6 sm:grid-cols-3">
            {stats.map((s) => (
              <div key={s.value} className="rounded-xl border bg-card p-7">
                <s.icon className="size-5 text-sage" />
                <p className="mt-4 font-serif text-5xl text-foreground">{s.value}</p>
                <p className="mt-2 text-sm text-muted-foreground">{s.label}</p>
              </div>
            ))}
          </div>
          <p className="mt-4 text-xs text-muted-foreground">{t("home.impactNote")}</p>
        </div>
      </section>

      {/* ---------------------------------------------------------------- CTA */}
      <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <div className="flex flex-col items-start gap-6 rounded-2xl bg-forest px-8 py-12 text-forest-foreground sm:flex-row sm:items-center sm:justify-between sm:px-12">
          <div>
            <p className="text-3xl font-semibold tracking-tight">
              Ready in <span className="font-serif font-normal italic opacity-90">under a minute.</span>
            </p>
            <p className="mt-2 text-sm opacity-75">No login. Works in English, हिंदी and मराठी.</p>
          </div>
          <LinkButton href="/recommend" size="lg" variant="secondary" className="h-14 gap-3 rounded-md px-6 text-base">
            {t("home.cta")} <ArrowRight className="size-4" />
          </LinkButton>
        </div>
      </section>
    </div>
  )
}

/** "Barrier physics" written around the round badge. */
function BadgeText() {
  return (
    <svg viewBox="0 0 120 120" className="size-full">
      <defs>
        <path id="arc-top" d="M 22 62 A 38 38 0 0 1 98 62" />
        <path id="arc-bottom" d="M 18 58 A 42 42 0 0 0 102 58" />
      </defs>
      <text className="fill-current" fontSize="12.5" letterSpacing="0.5">
        <textPath href="#arc-top" startOffset="50%" textAnchor="middle">
          Barrier
        </textPath>
      </text>
      <text className="fill-current" fontSize="14" fontWeight="600" letterSpacing="0.5">
        <textPath href="#arc-bottom" startOffset="50%" textAnchor="middle">
          physics
        </textPath>
      </text>
      <g transform="translate(51 30)" className="stroke-current" fill="none" strokeWidth="1.6" strokeLinejoin="round">
        <path d="M9 1 L11 7 L17 9 L11 11 L9 17 L7 11 L1 9 L7 7 Z" />
      </g>
    </svg>
  )
}
