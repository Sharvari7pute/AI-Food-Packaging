import type { Metadata } from "next"
import { MaterialsPage } from "./materials-page"

export const metadata: Metadata = { title: "Material Library" }

export default function Page() {
  return <MaterialsPage />
}
