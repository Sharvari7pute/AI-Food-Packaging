import Link from "next/link"

export function Logo({ className = "" }: { className?: string }) {
  return (
    <Link href="/" className={`flex items-center gap-2 font-semibold tracking-tight ${className}`} aria-label="PackSmart home">
      <span className="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-primary to-brand text-primary-foreground shadow-sm">
        <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
          <path d="M3 7.5 12 3l9 4.5v9L12 21l-9-4.5v-9Z" strokeLinejoin="round" />
          <path d="M3 7.5 12 12l9-4.5M12 12v9" strokeLinejoin="round" />
        </svg>
      </span>
      <span className="text-lg">
        Pack<span className="text-primary">Smart</span>
      </span>
    </Link>
  )
}
