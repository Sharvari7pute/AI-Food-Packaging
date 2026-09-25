import type { Metadata } from "next"
import { BuilderPage } from "./builder-page"

export const metadata: Metadata = { title: "Laminate Builder" }

export default function Page() {
  return <BuilderPage />
}
