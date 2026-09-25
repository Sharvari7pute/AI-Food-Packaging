"use client"

import { ArrowRight, Bot, Calculator, Database, ExternalLink, FileText, User } from "lucide-react"
import { api } from "@/lib/api"
import { useApi } from "@/lib/hooks"
import { useI18n } from "@/lib/i18n"
import { num } from "@/lib/format"
import { Container, PageHeader } from "@/components/common/page-header"
import { ApproxBadge } from "@/components/common/badges"
import { Tex } from "@/components/common/tex"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { ReactNode } from "react"

const STEPS: { title: string; text: ReactNode; tex: string[] }[] = [
  {
    title: "A · Requirements from the food",
    text: "Fat > 10 % (in dry foods) or high O₂ sensitivity → HIGH oxygen barrier. Water activity decides the moisture mode: aw < 0.70 keep moisture out, aw > 0.90 keep moisture in, respiring produce → breathable film. Light-sensitive → opaque. Frozen → film must stay flexible at −25 °C. Long distance → strength ≥ 3/5.",
    tex: [],
  },
  {
    title: "B · Temperature and required barrier",
    text: "The Q10 rule scales transmission with temperature. The oxygen the food can tolerate and the water it may gain or lose set the maximum OTR and WVTR.",
    tex: [
      String.raw`tf(T, T_{ref}) = Q_{10}^{\,(T - T_{ref})/10}, \quad Q_{10} = 2`,
      String.raw`OTR_{req} = \dfrac{O_{2,tol}\,[\text{mL/kg}] \times m_{kg}}{A \times t_{days} \times 0.21 \times tf(T, 23)}`,
      String.raw`WVTR_{req} = \dfrac{0.02 \times m_{g}}{A \times t_{days} \times \frac{|RH - 100\,a_w|}{90} \times tf(T, 38)}`,
      String.raw`A \approx 0.02 + 0.0004 \times m_g \;\; [\text{m}^2]`,
    ],
  },
  {
    title: "C · Laminates (layers in series)",
    text: "Each layer's value is scaled from its 25 µm reference; layers add like resistors in series. A laminate is recyclable only if every layer is from the same polymer family.",
    tex: [String.raw`OTR_{layer}(t) = OTR_{25} \times \frac{25}{t}, \qquad \frac{1}{OTR_{total}} = \sum_i \frac{1}{OTR_i}`],
  },
  {
    title: "D · Thickness",
    text: "For single films the thinnest standard gauge (20–100 µm) that meets both limits is chosen. If even 100 µm is not enough, the film fails with the reason.",
    tex: [],
  },
  {
    title: "E · Cost and carbon",
    text: "Grams of each layer per pack give cost and CO₂e per 1000 packs.",
    tex: [
      String.raw`g_{pack} = A \times t_{\mu m} \times \rho_{g/cm^3}`,
      String.raw`\text{INR}_{1000} = \sum g \times \frac{\text{INR/kg}}{1000} \times 1000, \qquad CO_2e_{1000} = \sum g \times EF_{kg/kg}`,
    ],
  },
  {
    title: "F · Filters and scores",
    text: "Hard filters remove packs that fail barrier, temperature, opacity, strength or heat-sealing. The rest are scored 0–1 on barrier, cost, eco and strength and weighted by your priority (Balanced 40/25/20/15, Eco 30/15/40/15, Budget 30/45/10/15).",
    tex: [],
  },
  {
    title: "G · Shelf life",
    text: "The same physics run backwards gives the days until the food absorbs its tolerable oxygen or water; the smaller one is the limiting factor (capped at 730 days).",
    tex: [
      String.raw`SL_{O_2} = \dfrac{O_{2,tol}}{OTR \times A \times 0.21 \times tf(T,23)}, \quad SL_{H_2O} = \dfrac{0.02\,m_g}{WVTR \times A \times \frac{|RH-100a_w|}{90} \times tf(T,38)}`,
    ],
  },
  {
    title: "H · Modified atmosphere for fresh produce",
    text: "Fruits and vegetables keep breathing. The film must let in exactly enough oxygen to hold the target O₂ level; the film closest above the requirement wins, otherwise micro-perforated LDPE.",
    tex: [
      String.raw`RR_{day} = r \times m_{kg} \times 24 \times tf(T, 20), \qquad OTR_{req,MAP} = \dfrac{RR_{day}}{A \times (0.21 - O_{2,target}/100)}`,
    ],
  },
]

export function MethodologyPage() {
  const { t } = useI18n()
  const materials = useApi(() => api.materials())

  return (
    <Container className="max-w-5xl">
      <PageHeader
        title={t("nav.methodology")}
        description="For judges and food technologists: exactly how PackSmart decides, the data behind it, and what it cannot do."
      />

      <section className="mb-10 rounded-2xl border bg-card p-6 shadow-sm">
        <h2 className="mb-5 text-lg font-semibold">Gemini explains, the engine decides</h2>
        <div className="grid items-stretch gap-3 md:grid-cols-[1fr_auto_1.2fr_auto_1fr]">
          <FlowBox icon={<User className="size-5" />} title="You" text="Food + storage conditions (form or plain language)" />
          <FlowArrow />
          <div className="rounded-xl border-2 border-primary bg-primary/10 p-4">
            <div className="flex items-center gap-2 font-semibold text-primary">
              <Calculator className="size-5" /> Deterministic engine
            </div>
            <p className="mt-1 text-sm">Rules + physics formulas (A–H) on our database. Picks every material and computes every number.</p>
            <p className="mt-2 flex items-center gap-1 text-xs text-muted-foreground">
              <Database className="size-3.5" /> materials, laminates, foods, MAP targets, cities
            </p>
          </div>
          <FlowArrow />
          <FlowBox icon={<FileText className="size-5" />} title="Result + spec PDF" text="Options, shelf life, cost, CO₂, QR-verified spec sheet" />
        </div>
        <div className="mt-4 rounded-xl border border-dashed border-violet-400/60 bg-violet-50/60 p-4 text-sm dark:bg-violet-500/5">
          <div className="flex items-center gap-2 font-semibold text-violet-700 dark:text-violet-300">
            <Bot className="size-5" /> Gemini (optional helper)
          </div>
          <ul className="mt-2 grid gap-1 text-muted-foreground sm:grid-cols-2">
            <li>• Turns plain-language text into form fields</li>
            <li>• Explains the engine&apos;s result in EN / हिंदी / मराठी</li>
            <li>• Pack-Bot answers only from our database</li>
            <li>• Estimates unknown foods — labelled &quot;AI-estimated — verify&quot;</li>
          </ul>
          <p className="mt-2 text-xs">Gemini never picks materials and never invents numbers. Without an AI key everything still works using fallbacks.</p>
        </div>
      </section>

      <section className="mb-10 space-y-4">
        <h2 className="text-lg font-semibold">The engine, step by step</h2>
        {STEPS.map((s) => (
          <div key={s.title} className="rounded-2xl border bg-card p-5 shadow-sm">
            <h3 className="font-semibold">{s.title}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{s.text}</p>
            {s.tex.map((f) => (
              <Tex key={f}>{f}</Tex>
            ))}
          </div>
        ))}
        <p className="text-xs text-muted-foreground">
          OTR in cc/(m²·day·atm) at 23 °C, WVTR in g/(m²·day) at 38 °C / 90 % RH, both normalised to 25 µm. O₂ tolerance: high 1,
          medium 10, low 50 mL O₂ per kg food. All constants live in the backend configuration, not in code.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="mb-3 text-lg font-semibold">Data sources (materials)</h2>
        {materials.loading ? (
          <Skeleton className="h-64 rounded-2xl" />
        ) : (
          <div className="overflow-x-auto rounded-2xl border bg-card shadow-sm">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Material</TableHead>
                  <TableHead>OTR @25µm</TableHead>
                  <TableHead>WVTR @25µm</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Source</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(materials.data ?? []).map((m) => (
                  <TableRow key={m.id}>
                    <TableCell className="font-medium">{m.name}</TableCell>
                    <TableCell>{num(m.otr25um)}</TableCell>
                    <TableCell>{num(m.wvtr25um)}</TableCell>
                    <TableCell>{m.approx ? <ApproxBadge /> : <span className="text-xs text-success">sourced</span>}</TableCell>
                    <TableCell className="max-w-64 truncate">
                      {m.sourceUrl?.startsWith("http") ? (
                        <a href={m.sourceUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-primary hover:underline">
                          <ExternalLink className="size-3" /> {new URL(m.sourceUrl).hostname.replace("www.", "")}
                        </a>
                      ) : (
                        <span className="text-muted-foreground">{m.sourceUrl ?? "-"}</span>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
        <p className="mt-2 text-xs text-muted-foreground">
          Material data, food properties (IFCT 2017, USDA FoodData Central, FDA) and MAP targets (UC Davis Postharvest) were
          researched by our team; approximate values are flagged in the data notes. Laminate structures, CO₂ factors and city
          climate are still placeholder values. The engine agrees with the literature packaging direction for 22 of 25 foods
          (validation report in the repository).
        </p>
      </section>

      <section className="rounded-2xl border bg-card p-6 shadow-sm">
        <h2 className="mb-3 text-lg font-semibold">Assumptions &amp; limitations</h2>
        <ul className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
          <li>• Estimates from literature values — validate with lab shelf-life tests before production.</li>
          <li>• Flat-pouch area estimate unless you enter the real pack area.</li>
          <li>• Barrier values of real films vary by grade and supplier; EVOH loses barrier when humid.</li>
          <li>• Seals, pinholes, flex-cracking and headspace are not modelled.</li>
          <li>• Q10 = 2 is a typical value; real foods differ.</li>
          <li>• Microbial spoilage is not modelled directly — only oxygen and moisture limits.</li>
          <li>• MAP uses the film&apos;s 23 °C OTR; real films breathe less in the cold.</li>
          <li>• CO₂e factors are approximate cradle-to-gate values.</li>
        </ul>
      </section>
    </Container>
  )
}

function FlowBox({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return (
    <div className="rounded-xl border bg-muted/40 p-4">
      <div className="flex items-center gap-2 font-semibold">
        {icon} {title}
      </div>
      <p className="mt-1 text-sm text-muted-foreground">{text}</p>
    </div>
  )
}

function FlowArrow() {
  return (
    <div className="flex items-center justify-center text-muted-foreground">
      <ArrowRight className="size-5 rotate-90 md:rotate-0" />
    </div>
  )
}
