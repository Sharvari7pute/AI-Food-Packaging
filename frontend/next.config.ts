import type { NextConfig } from "next"

// Optional same-origin mode: set API_PROXY_TARGET (e.g. http://localhost:8080) and NEXT_PUBLIC_API_URL=/
// to serve the backend API through the frontend's own address (one public URL, no CORS).
const proxyTarget = process.env.API_PROXY_TARGET?.replace(/\/$/, "")

const nextConfig: NextConfig = {
  async rewrites() {
    return proxyTarget ? [{ source: "/api/:path*", destination: `${proxyTarget}/api/:path*` }] : []
  },
}

export default nextConfig
