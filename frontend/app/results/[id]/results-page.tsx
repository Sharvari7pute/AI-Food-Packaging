"use client"

import { api } from "@/lib/api"
import { useApi } from "@/lib/hooks"
import { Container } from "@/components/common/page-header"
import { ErrorState } from "@/components/common/states"
import { ResultsView } from "@/components/results/results-view"
import { Skeleton } from "@/components/ui/skeleton"

export function ResultsPage({ id }: { id: string }) {
  const { data, error, loading, retry } = useApi(() => api.recommendation(id), [id])
  return (
    <Container>
      {loading && <ResultsSkeleton />}
      {error && <ErrorState message={error} onRetry={retry} />}
      {data && <ResultsView result={data} />}
    </Container>
  )
}

export function ResultsSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-44 rounded-2xl" />
      <div className="grid gap-6 lg:grid-cols-2">
        <Skeleton className="h-56 rounded-2xl" />
        <Skeleton className="h-56 rounded-2xl" />
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        <Skeleton className="h-80 rounded-2xl" />
        <Skeleton className="h-80 rounded-2xl" />
        <Skeleton className="h-80 rounded-2xl" />
      </div>
    </div>
  )
}
