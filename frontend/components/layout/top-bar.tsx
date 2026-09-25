/** Thin announcement bar above the header (as in the AnnKAVACH design). */
export function TopBar() {
  return (
    <div className="bg-forest text-forest-foreground">
      <div className="mx-auto flex h-9 max-w-7xl items-center justify-between gap-4 px-4 text-[11px] font-medium tracking-[0.18em] uppercase sm:px-6">
        <p className="flex items-center gap-3 truncate">
          <span>Smart packaging</span>
          <span className="hidden opacity-60 sm:inline">•</span>
          <span className="hidden sm:inline">Better shelf life</span>
          <span className="hidden opacity-60 sm:inline">•</span>
          <span className="hidden sm:inline">Less waste</span>
        </p>
        <p className="flex shrink-0 items-center gap-4">
          <span className="font-semibold">SIH26236</span>
          <span className="hidden opacity-60 md:inline">Ministry of Food Processing Industries</span>
        </p>
      </div>
    </div>
  )
}
