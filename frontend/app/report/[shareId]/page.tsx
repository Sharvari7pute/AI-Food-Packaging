import type { Metadata } from "next"
import { VerifyPage } from "./verify-page"

export const metadata: Metadata = { title: "Verified packaging spec" }

export default async function Page({ params }: PageProps<"/report/[shareId]">) {
  const { shareId } = await params
  return <VerifyPage shareId={shareId} />
}
