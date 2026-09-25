import type { Metadata } from "next"
import { ResultsPage } from "./results-page"

export const metadata: Metadata = { title: "Your packaging recommendation" }

export default async function Page({ params }: PageProps<"/results/[id]">) {
  const { id } = await params
  return <ResultsPage id={id} />
}
