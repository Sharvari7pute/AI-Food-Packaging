# PackSmart frontend

Next.js (App Router) + TypeScript + Tailwind + shadcn/ui (Base UI) + Recharts.

```bash
cp .env.example .env.local   # NEXT_PUBLIC_API_URL=http://localhost:8080
npm install
npm run dev                  # http://localhost:3000
npm run lint && npm run build
npm run test:e2e             # Playwright smoke test (API mocked with fixtures; E2E_REAL_API=1 for a live backend)
```

See the root [README](../README.md) for the full setup and deployment.
