import type { Metadata } from "next"
import { ComparePage } from "./compare-page"

export const metadata: Metadata = { title: "Compare" }

export default async function Page({ searchParams }: PageProps<"/compare">) {
  const sp = await searchParams
  const rec = typeof sp.rec === "string" && /^\d+$/.test(sp.rec) ? Number(sp.rec) : null
  return <ComparePage recommendationId={rec} />
}
