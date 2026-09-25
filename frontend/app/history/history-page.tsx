"use client"

import { useState } from "react"
import { ChevronLeft, ChevronRight, Download, ExternalLink } from "lucide-react"
import { api, pdfUrl } from "@/lib/api"
import { useApi } from "@/lib/hooks"
import { useI18n } from "@/lib/i18n"
import { dateTime } from "@/lib/format"
import { Container, PageHeader } from "@/components/common/page-header"
import { EmptyState, ErrorState } from "@/components/common/states"
import { LinkButton } from "@/components/common/link-button"
import { Button, buttonVariants } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

const PAGE_SIZE = 15

export function HistoryPage() {
  const { t } = useI18n()
  const [page, setPage] = useState(0)
  const { data, error, loading, retry } = useApi(() => api.history(page, PAGE_SIZE), [page])

  return (
    <Container>
      <PageHeader title={t("nav.history")} description="Past recommendations (shared by everyone using this PackSmart instance), newest first.">
        <LinkButton href="/recommend">New recommendation</LinkButton>
      </PageHeader>
      {loading && <Skeleton className="h-96 rounded-2xl" />}
      {error && <ErrorState message={error} onRetry={retry} />}
      {data && data.content.length === 0 && (
        <EmptyState title="No recommendations yet">
          <LinkButton href="/recommend">Run your first one</LinkButton>
        </EmptyState>
      )}
      {data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-2xl border bg-card shadow-sm">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>#</TableHead>
                <TableHead>Food</TableHead>
                <TableHead>Top option</TableHead>
                <TableHead>Shelf life</TableHead>
                <TableHead>Created</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="text-muted-foreground">{r.id}</TableCell>
                  <TableCell className="font-medium">{r.commodity}</TableCell>
                  <TableCell>{r.topOption ?? <span className="text-muted-foreground">none passed</span>}</TableCell>
                  <TableCell>{r.estimatedShelfLifeDays != null ? `${r.estimatedShelfLifeDays} days` : "-"}</TableCell>
                  <TableCell className="text-muted-foreground">{dateTime(r.createdAt)}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <LinkButton href={`/results/${r.id}`} variant="outline" size="sm">
                        <ExternalLink /> Open
                      </LinkButton>
                      <a href={pdfUrl(r.shareId)} className={buttonVariants({ variant: "ghost", size: "sm" })} aria-label="Download PDF">
                        <Download /> PDF
                      </a>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <div className="flex items-center justify-between border-t p-3 text-sm text-muted-foreground">
            <span>
              Page {data.page + 1} of {Math.max(1, data.totalPages)} · {data.totalElements} total
            </span>
            <div className="flex gap-1">
              <Button variant="outline" size="sm" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>
                <ChevronLeft /> Prev
              </Button>
              <Button variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= data.totalPages}>
                Next <ChevronRight />
              </Button>
            </div>
          </div>
        </div>
      )}
    </Container>
  )
}
