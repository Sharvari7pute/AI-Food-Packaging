"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, ArrowRight, Check, Leaf, Loader2, MapPin, Scale, Snowflake, Sun, ThermometerSnowflake, Truck, Wallet } from "lucide-react"
import { toast } from "sonner"
import { api, ApiError } from "@/lib/api"
import { useApi } from "@/lib/hooks"
import { LANGS, useI18n } from "@/lib/i18n"
import type { CityDto, CommodityDto, CustomCommodity, Lang, Level, ParseResponse, Priority, RecommendRequest, StorageType, Transport } from "@/lib/types"
import { Container, PageHeader } from "@/components/common/page-header"
import { Field, NumberInput } from "@/components/common/field"
import { Segmented } from "@/components/common/segmented"
import { ApproxBadge } from "@/components/common/badges"
import { ErrorState } from "@/components/common/states"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { FoodCombobox } from "./food-combobox"
import { NlBox } from "./nl-box"
import { EstimateFoodDialog } from "./estimate-food-dialog"
import { cn } from "@/lib/utils"
import { humanize, tempRange } from "@/lib/format"

type Season = "summer" | "monsoon" | "winter"
type Props = { moisturePct: number | null; waterActivity: number | null; fatPct: number | null; respirationRate: number | null }

/** Typical temperatures pre-filled when the storage type changes (the user can edit them). */
const TYPICAL_TEMP: Record<StorageType, number> = { AMBIENT: 30, CHILLED: 4, FROZEN: -18 }
/** A recommended storage maximum at or below these means frozen / chilled storage. */
const FROZEN_MAX_C = -10
const CHILLED_MAX_C = 15

function currentSeason(): Season {
  const m = new Date().getMonth() + 1
  if (m >= 3 && m <= 5) return "summer"
  if (m >= 6 && m <= 9) return "monsoon"
  return "winter"
}

function climate(c: CityDto, s: Season): { temp: number | null; rh: number | null } {
  if (s === "summer") return { temp: c.summerTempC, rh: c.summerRhPct }
  if (s === "monsoon") return { temp: c.monsoonTempC, rh: c.monsoonRhPct }
  return { temp: c.winterTempC, rh: c.winterRhPct }
}

export function RecommendWizard() {
  const { t, lang } = useI18n()
  const router = useRouter()
  const foods = useApi(() => api.commodities())
  const cities = useApi(() => api.cities())

  const [step, setStep] = useState(0)
  const [missing, setMissing] = useState<Set<string>>(new Set())
  const [submitting, setSubmitting] = useState(false)

  const [commodityId, setCommodityId] = useState<number | null>(null)
  const [commodity, setCommodity] = useState<CommodityDto | null>(null)
  const [props, setProps] = useState<Props>({ moisturePct: null, waterActivity: null, fatPct: null, respirationRate: null })
  const [custom, setCustom] = useState<CustomCommodity | null>(null)
  const [estimateOpen, setEstimateOpen] = useState(false)

  const [packWeightG, setPackWeightG] = useState<number | null>(100)
  const [shelfLifeDays, setShelfLifeDays] = useState<number | null>(90)
  const [storageType, setStorageType] = useState<StorageType | null>("AMBIENT")
  const [cityId, setCityId] = useState<number | null>(null)
  const [season, setSeason] = useState<Season>(currentSeason())
  const [storageTempC, setStorageTempC] = useState<number | null>(30)
  const [rh, setRh] = useState<number | null>(65)
  const [transport, setTransport] = useState<Transport>("LOCAL")
  const [priority, setPriority] = useState<Priority>("DEFAULT")
  const [language, setLanguage] = useState<Lang>(lang)

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- follow the UI language by default
    setLanguage(lang)
  }, [lang])

  // Load full commodity to auto-fill editable properties.
  useEffect(() => {
    if (commodityId == null) return
    let cancelled = false
    api
      .commodity(commodityId)
      .then((c) => {
        if (cancelled) return
        setCommodity(c)
        setProps({ moisturePct: c.moisturePct, waterActivity: c.waterActivity, fatPct: c.fatPct, respirationRate: c.respirationRate })
        if (c.defaultShelfLifeDays && c.respiring) setShelfLifeDays((d) => d ?? c.defaultShelfLifeDays)
        // Pre-fill storage from the food's recommended range (user can still change it).
        if (c.storageTempMinC != null && c.storageTempMaxC != null) {
          const max = c.storageTempMaxC
          const type: StorageType = max <= FROZEN_MAX_C ? "FROZEN" : max <= CHILLED_MAX_C ? "CHILLED" : "AMBIENT"
          setStorageType(type)
          if (type !== "AMBIENT") setStorageTempC((c.storageTempMinC + c.storageTempMaxC) / 2)
        }
      })
      .catch((e) => toast.error((e as Error).message))
    return () => {
      cancelled = true
    }
  }, [commodityId])

  // City + season → temperature & RH (ambient storage only for temperature).
  const city = useMemo(() => cities.data?.find((c) => c.id === cityId) ?? null, [cities.data, cityId])
  function applyClimate(c: CityDto | null, s: Season, type: StorageType | null) {
    if (!c) return
    const cl = climate(c, s)
    if (cl.rh != null) setRh(cl.rh)
    if (cl.temp != null && (type ?? "AMBIENT") === "AMBIENT") setStorageTempC(cl.temp)
    clearMissing("storageTempC", "relativeHumidityPct")
  }

  function clearMissing(...fields: string[]) {
    setMissing((m) => {
      if (!fields.some((f) => m.has(f))) return m
      const n = new Set(m)
      fields.forEach((f) => n.delete(f))
      return n
    })
  }

  function selectFood(id: number) {
    setCustom(null)
    setCommodityId(id)
    clearMissing("commodityId")
  }

  function onParsed(p: ParseResponse) {
    const d = p.draft
    if (d.commodityId) selectFood(d.commodityId)
    setPackWeightG(d.packWeightG ?? null)
    setShelfLifeDays(d.shelfLifeDays ?? null)
    if (d.storageType) setStorageType(d.storageType)
    setStorageTempC(d.storageTempC ?? (d.storageType ? TYPICAL_TEMP[d.storageType] : null))
    setRh(d.relativeHumidityPct ?? null)
    if (d.transport) setTransport(d.transport)
    const miss = new Set(p.missingFields)
    if (!d.storageTempC && d.storageType) miss.delete("storageTempC")
    setMissing(miss)
    setStep(miss.has("commodityId") ? 0 : 1)
  }

  function buildRequest(): RecommendRequest {
    const overrides =
      commodity && !custom
        ? {
            moisturePct: props.moisturePct !== commodity.moisturePct ? props.moisturePct : null,
            waterActivity: props.waterActivity !== commodity.waterActivity ? props.waterActivity : null,
            fatPct: props.fatPct !== commodity.fatPct ? props.fatPct : null,
            respirationRate: props.respirationRate !== commodity.respirationRate ? props.respirationRate : null,
          }
        : null
    return {
      commodityId: custom ? null : commodityId,
      customCommodity: custom,
      overrides,
      packWeightG,
      shelfLifeDays,
      storageType,
      storageTempC,
      relativeHumidityPct: rh,
      transport,
      priority,
      language,
    }
  }

  function validate(upTo: number): Set<string> {
    const m = new Set<string>()
    if (upTo >= 0 && !custom && commodityId == null) m.add("commodityId")
    if (upTo >= 1) {
      if (!packWeightG || packWeightG < 1 || packWeightG > 50000) m.add("packWeightG")
      if (!shelfLifeDays || shelfLifeDays < 1 || shelfLifeDays > 730) m.add("shelfLifeDays")
      if (!storageType) m.add("storageType")
      if (storageTempC == null || storageTempC < -40 || storageTempC > 60) m.add("storageTempC")
      if (rh == null || rh < 0 || rh > 100) m.add("relativeHumidityPct")
    }
    return m
  }

  function next() {
    const m = validate(step)
    setMissing(m)
    if (m.size) {
      toast.error(t("wizard.missing"))
      if (m.has("commodityId")) setStep(0)
      return
    }
    setStep((s) => Math.min(s + 1, 2))
  }

  async function submit() {
    const m = validate(2)
    setMissing(m)
    if (m.size) {
      toast.error(t("wizard.missing"))
      setStep(m.has("commodityId") ? 0 : 1)
      return
    }
    setSubmitting(true)
    try {
      const res = await api.recommend(buildRequest())
      router.push(`/results/${res.id}`)
    } catch (e) {
      const err = e as ApiError
      toast.error(err.message, { description: err.details?.join(", ") })
      setSubmitting(false)
    }
  }

  const steps = [t("wizard.step.food"), t("wizard.step.storage"), t("wizard.step.priority")]
  const respiring = custom ? !!custom.respiring : !!commodity?.respiring

  if (foods.error) {
    return (
      <Container>
        <ErrorState message={foods.error} onRetry={foods.retry} />
      </Container>
    )
  }

  return (
    <Container className="max-w-4xl">
      <PageHeader title={t("wizard.title")} description="Two ways in: describe it in your own words, or fill three quick steps." />

      <section className="mb-8 rounded-2xl border bg-accent/50 p-5 shadow-sm">
        <h2 className="mb-3 flex items-center gap-2 font-semibold">
          <Wand /> {t("wizard.describe")}
        </h2>
        <NlBox onParsed={onParsed} />
      </section>

      <div className="rounded-2xl border bg-card shadow-sm">
        <ol className="flex border-b" aria-label="Steps">
          {steps.map((label, i) => (
            <li key={label} className="flex-1">
              <button
                type="button"
                onClick={() => (i <= step ? setStep(i) : next())}
                className={cn(
                  "flex w-full items-center justify-center gap-2 px-2 py-4 text-sm font-medium text-muted-foreground",
                  i === step && "text-foreground",
                )}
                aria-current={i === step ? "step" : undefined}
              >
                <span
                  className={cn(
                    "grid size-6 place-items-center rounded-full border text-xs",
                    i < step && "border-primary bg-primary text-primary-foreground",
                    i === step && "border-primary text-primary",
                  )}
                >
                  {i < step ? <Check className="size-3.5" /> : i + 1}
                </span>
                <span className="hidden sm:inline">{label}</span>
              </button>
              <div className={cn("h-0.5", i <= step ? "bg-primary" : "bg-transparent")} />
            </li>
          ))}
        </ol>

        <div className="space-y-6 p-5 sm:p-6">
          {step === 0 && (
            <>
              <Field label={t("wizard.food")} invalid={missing.has("commodityId")}>
                {foods.loading ? (
                  <Skeleton className="h-11" />
                ) : (
                  <FoodCombobox
                    foods={foods.data ?? []}
                    value={custom ? null : commodityId}
                    onChange={selectFood}
                    placeholder={t("wizard.foodSearch")}
                    invalid={missing.has("commodityId")}
                  />
                )}
              </Field>
              <button type="button" onClick={() => setEstimateOpen(true)} className="text-sm font-medium text-primary hover:underline">
                {t("wizard.notInList")}
              </button>
              <EstimateFoodDialog
                open={estimateOpen}
                onOpenChange={setEstimateOpen}
                onExisting={selectFood}
                onEstimated={(f) => {
                  setCustom(f)
                  setCommodityId(null)
                  setCommodity(null)
                  clearMissing("commodityId")
                }}
              />

              {custom && <CustomFoodEditor food={custom} onChange={setCustom} />}

              {!custom && commodity && (
                <div className="rounded-xl border bg-muted/30 p-4">
                  <div className="mb-3 flex flex-wrap items-center gap-2">
                    <h3 className="text-sm font-semibold">{t("wizard.properties")}</h3>
                    {commodity.approx && <ApproxBadge />}
                    <span className="text-xs text-muted-foreground">
                      O₂ sensitivity {commodity.o2Sensitive} · light {commodity.lightSensitive}
                      {commodity.respiring ? " · fresh produce (breathes)" : ""}
                    </span>
                  </div>
                  <div className="mb-3 flex flex-wrap gap-2 text-xs">
                    {commodity.mainDeteriorationFactor && (
                      <span className="rounded-full bg-warning/20 px-2.5 py-1">Spoils mainly by: {humanize(commodity.mainDeteriorationFactor)}</span>
                    )}
                    {tempRange(commodity.storageTempMinC, commodity.storageTempMaxC) && (
                      <span className="rounded-full bg-brand/10 px-2.5 py-1 text-brand">
                        Recommended storage: {tempRange(commodity.storageTempMinC, commodity.storageTempMaxC)}
                      </span>
                    )}
                  </div>
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <Field label={t("wizard.moisture")} htmlFor="moisture">
                      <NumberInput id="moisture" value={props.moisturePct} onChange={(v) => setProps({ ...props, moisturePct: v })} min={0} max={100} />
                    </Field>
                    <Field label={t("wizard.aw")} htmlFor="aw">
                      <NumberInput id="aw" value={props.waterActivity} onChange={(v) => setProps({ ...props, waterActivity: v })} step="0.01" min={0} max={1} />
                    </Field>
                    <Field label={t("wizard.fat")} htmlFor="fat">
                      <NumberInput id="fat" value={props.fatPct} onChange={(v) => setProps({ ...props, fatPct: v })} min={0} max={100} />
                    </Field>
                    {respiring && (
                      <Field label={t("wizard.respiration")} htmlFor="rr">
                        <NumberInput id="rr" value={props.respirationRate} onChange={(v) => setProps({ ...props, respirationRate: v })} min={0} />
                      </Field>
                    )}
                  </div>
                </div>
              )}
            </>
          )}

          {step === 1 && (
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label={t("wizard.packWeight")} htmlFor="weight" invalid={missing.has("packWeightG")}>
                <NumberInput
                  id="weight"
                  testId="pack-weight"
                  value={packWeightG}
                  onChange={(v) => {
                    setPackWeightG(v)
                    clearMissing("packWeightG")
                  }}
                  min={1}
                  max={50000}
                  invalid={missing.has("packWeightG")}
                />
              </Field>
              <Field label={t("wizard.shelfLife")} htmlFor="days" invalid={missing.has("shelfLifeDays")}>
                <NumberInput
                  id="days"
                  testId="shelf-life"
                  value={shelfLifeDays}
                  onChange={(v) => {
                    setShelfLifeDays(v)
                    clearMissing("shelfLifeDays")
                  }}
                  step="1"
                  min={1}
                  max={730}
                  invalid={missing.has("shelfLifeDays")}
                />
              </Field>
              <Field label={t("wizard.storageType")} className="sm:col-span-2" invalid={missing.has("storageType")}>
                <Segmented<StorageType>
                  ariaLabel={t("wizard.storageType")}
                  value={storageType}
                  invalid={missing.has("storageType")}
                  onChange={(v) => {
                    setStorageType(v)
                    setStorageTempC(v === "AMBIENT" && city ? climate(city, season).temp : TYPICAL_TEMP[v])
                    clearMissing("storageType", "storageTempC")
                  }}
                  options={[
                    { value: "AMBIENT", label: t("wizard.ambient"), icon: <Sun className="size-4" /> },
                    { value: "CHILLED", label: t("wizard.chilled"), icon: <ThermometerSnowflake className="size-4" /> },
                    { value: "FROZEN", label: t("wizard.frozen"), icon: <Snowflake className="size-4" /> },
                  ]}
                />
              </Field>
              <Field label={t("wizard.city")} htmlFor="city" hint="Fills temperature and humidity for the season (you can still edit them).">
                <div className="relative">
                  <MapPin className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                  <select
                    id="city"
                    value={cityId ?? ""}
                    onChange={(e) => {
                      const id = e.target.value ? Number(e.target.value) : null
                      setCityId(id)
                      applyClimate(cities.data?.find((c) => c.id === id) ?? null, season, storageType)
                    }}
                    className="h-10 w-full appearance-none rounded-lg border bg-background pr-3 pl-9 text-sm dark:bg-input/30"
                  >
                    <option value="">Choose a city…</option>
                    {(cities.data ?? []).map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}, {c.state}
                      </option>
                    ))}
                  </select>
                </div>
              </Field>
              <Field label={t("wizard.season")}>
                <Segmented<Season>
                  ariaLabel={t("wizard.season")}
                  value={season}
                  onChange={(s) => {
                    setSeason(s)
                    applyClimate(city, s, storageType)
                  }}
                  options={[
                    { value: "summer", label: t("wizard.summer") },
                    { value: "monsoon", label: t("wizard.monsoon") },
                    { value: "winter", label: t("wizard.winter") },
                  ]}
                />
              </Field>
              <Field label={t("wizard.temp")} htmlFor="temp" invalid={missing.has("storageTempC")}>
                <NumberInput
                  id="temp"
                  value={storageTempC}
                  onChange={(v) => {
                    setStorageTempC(v)
                    clearMissing("storageTempC")
                  }}
                  min={-40}
                  max={60}
                  invalid={missing.has("storageTempC")}
                />
              </Field>
              <Field label={t("wizard.rh")} htmlFor="rh" invalid={missing.has("relativeHumidityPct")}>
                <NumberInput
                  id="rh"
                  value={rh}
                  onChange={(v) => {
                    setRh(v)
                    clearMissing("relativeHumidityPct")
                  }}
                  min={0}
                  max={100}
                  invalid={missing.has("relativeHumidityPct")}
                />
              </Field>
              <Field label={t("wizard.transport")} className="sm:col-span-2">
                <Segmented<Transport>
                  ariaLabel={t("wizard.transport")}
                  value={transport}
                  onChange={setTransport}
                  options={[
                    { value: "LOCAL", label: t("wizard.local") },
                    { value: "LONG_DISTANCE", label: t("wizard.long"), icon: <Truck className="size-4" /> },
                  ]}
                />
              </Field>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-6">
              <Field label={t("wizard.priority")}>
                <Segmented<Priority>
                  ariaLabel={t("wizard.priority")}
                  value={priority}
                  onChange={setPriority}
                  className="h-auto"
                  options={[
                    { value: "DEFAULT", label: t("wizard.default"), icon: <Scale className="size-4" /> },
                    { value: "ECO", label: t("wizard.eco"), icon: <Leaf className="size-4" /> },
                    { value: "BUDGET", label: t("wizard.budget"), icon: <Wallet className="size-4" /> },
                  ]}
                />
                <p className="text-xs text-muted-foreground">
                  {priority === "ECO"
                    ? "Recyclable and biodegradable packs rank higher (eco weight 40%)."
                    : priority === "BUDGET"
                      ? "Cheaper packs rank higher (cost weight 45%)."
                      : "Balanced: barrier 40%, cost 25%, eco 20%, strength 15%."}
                </p>
              </Field>
              <Field label={t("wizard.language")}>
                <Segmented<Lang>
                  ariaLabel={t("wizard.language")}
                  value={language}
                  onChange={setLanguage}
                  options={LANGS.map((l) => ({ value: l.code, label: l.label }))}
                />
              </Field>
              <ReviewSummary
                food={custom ? `${custom.name} (AI-estimated)` : commodity?.name ?? "-"}
                lines={[
                  `${packWeightG ?? "-"} g · ${shelfLifeDays ?? "-"} days`,
                  `${storageType ?? "-"} · ${storageTempC ?? "-"} °C · ${rh ?? "-"}% RH${city ? ` · ${city.name} ${season}` : ""}`,
                  transport === "LONG_DISTANCE" ? "Long-distance transport" : "Local transport",
                ]}
              />
            </div>
          )}
        </div>

        <div className="flex items-center justify-between gap-2 border-t p-4">
          <Button variant="ghost" onClick={() => setStep((s) => Math.max(0, s - 1))} disabled={step === 0}>
            <ArrowLeft /> {t("wizard.back")}
          </Button>
          {step < 2 ? (
            <Button onClick={next} data-testid="wizard-next" size="lg">
              {t("wizard.next")} <ArrowRight />
            </Button>
          ) : (
            <Button onClick={submit} disabled={submitting} data-testid="wizard-submit" size="lg">
              {submitting ? <Loader2 className="animate-spin" /> : <Check />} {t("wizard.submit")}
            </Button>
          )}
        </div>
      </div>
    </Container>
  )
}

function Wand() {
  return <span className="grid size-6 place-items-center rounded-md bg-primary/15 text-primary">✎</span>
}

function ReviewSummary({ food, lines }: { food: string; lines: string[] }) {
  return (
    <div className="rounded-xl border bg-muted/30 p-4 text-sm">
      <p className="font-semibold">{food}</p>
      {lines.map((l) => (
        <p key={l} className="text-muted-foreground">
          {l}
        </p>
      ))}
    </div>
  )
}

const LEVELS: Level[] = ["low", "medium", "high"]

function CustomFoodEditor({ food, onChange }: { food: CustomCommodity; onChange: (f: CustomCommodity) => void }) {
  const set = (patch: Partial<CustomCommodity>) => onChange({ ...food, ...patch })
  return (
    <div className="space-y-4 rounded-xl border border-warning/60 bg-warning/10 p-4">
      <div className="flex flex-wrap items-center gap-2">
        <span className="rounded-md bg-warning px-2 py-0.5 text-xs font-semibold text-black">AI-estimated — verify</span>
        <span className="text-sm text-muted-foreground">
          Values below were estimated by AI for <b className="text-foreground">{food.name}</b>. Check and edit them — they are
          not saved to our database.
        </span>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Field label="Name" htmlFor="c-name">
          <Input id="c-name" value={food.name} onChange={(e) => set({ name: e.target.value })} className="h-10" />
        </Field>
        <Field label="Water activity (aw)" htmlFor="c-aw">
          <NumberInput id="c-aw" value={food.waterActivity} onChange={(v) => set({ waterActivity: v ?? 0 })} step="0.01" min={0} max={1} />
        </Field>
        <Field label="Fat %" htmlFor="c-fat">
          <NumberInput id="c-fat" value={food.fatPct} onChange={(v) => set({ fatPct: v ?? 0 })} min={0} max={100} />
        </Field>
        <Field label="Moisture %" htmlFor="c-m">
          <NumberInput id="c-m" value={food.moisturePct} onChange={(v) => set({ moisturePct: v })} min={0} max={100} />
        </Field>
        <Field label="Oxygen sensitivity">
          <Segmented<Level> ariaLabel="Oxygen sensitivity" value={food.o2Sensitive ?? "medium"} onChange={(v) => set({ o2Sensitive: v })} options={LEVELS.map((l) => ({ value: l, label: l }))} />
        </Field>
        <Field label="Light sensitivity">
          <Segmented<Level> ariaLabel="Light sensitivity" value={food.lightSensitive ?? "medium"} onChange={(v) => set({ lightSensitive: v })} options={LEVELS.map((l) => ({ value: l, label: l }))} />
        </Field>
        <Field label="Fresh produce (breathes)?">
          <Segmented<"yes" | "no">
            ariaLabel="Respiring"
            value={food.respiring ? "yes" : "no"}
            onChange={(v) => set({ respiring: v === "yes" })}
            options={[
              { value: "no", label: "No" },
              { value: "yes", label: "Yes" },
            ]}
          />
        </Field>
        {food.respiring && (
          <Field label="Respiration (mL O₂/kg·h)" htmlFor="c-rr">
            <NumberInput id="c-rr" value={food.respirationRate} onChange={(v) => set({ respirationRate: v })} min={0} />
          </Field>
        )}
      </div>
    </div>
  )
}
