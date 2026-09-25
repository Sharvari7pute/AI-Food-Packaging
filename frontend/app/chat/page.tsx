import type { Metadata } from "next"
import { ChatPage } from "./chat-page"

export const metadata: Metadata = { title: "Pack-Bot" }

export default async function Page({ searchParams }: PageProps<"/chat">) {
  const sp = await searchParams
  const rec = typeof sp.rec === "string" && /^\d+$/.test(sp.rec) ? Number(sp.rec) : null
  return <ChatPage recommendationId={rec} />
}
