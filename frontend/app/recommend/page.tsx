import type { Metadata } from "next"
import { RecommendWizard } from "@/components/recommend/recommend-wizard"

export const metadata: Metadata = { title: "Find packaging" }

export default function Page() {
  return <RecommendWizard />
}
