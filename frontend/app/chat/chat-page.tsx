"use client"

import { Container, PageHeader } from "@/components/common/page-header"
import { ChatPanel } from "@/components/chat/chat-panel"
import { useI18n } from "@/lib/i18n"

export function ChatPage({ recommendationId }: { recommendationId: number | null }) {
  const { t } = useI18n()
  return (
    <Container className="max-w-3xl">
      <PageHeader
        title={t("nav.chat")}
        description={
          recommendationId
            ? `Ask anything about recommendation #${recommendationId} or packaging in general.`
            : "Your packaging assistant. Answers come only from PackSmart's database — it says so when it doesn't know."
        }
      />
      <ChatPanel recommendationId={recommendationId} />
    </Container>
  )
}
