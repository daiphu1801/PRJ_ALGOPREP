import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

// i18n không dùng route segment [locale] — ngôn ngữ đổi qua cookie NEXT_LOCALE +
// attribute data-ui-lang trên <html>, giữ URL nguyên theo SYS0102_frontend_architecture.md:34-37.
const withNextIntl = createNextIntlPlugin("./src/shared/i18n/request.ts");

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Off by request — the dev-only floating "N" badge (route/build indicator) was mistaken for an
  // app button during UI review. Dev-mode only; never renders in production either way.
  devIndicators: false,
  // Next gzips proxied responses, which makes the rewrite hold an SSE stream (F5 mock interview turns)
  // until it ends; measured 2026-11-12: 1 chunk at the end with gzip, 6 incremental chunks without.
  compress: false,
  // F2 generation allows 120s for the provider plus sandbox work; keep the API response reachable.
  experimental: { proxyTimeout: 240_000 },
  // The browser talks to the backend through this same-origin proxy, so no CORS is needed and the
  // refresh cookie (Path=/api/v1/identity/auth, SameSite=Strict) is sent as-is. Point
  // NEXT_PUBLIC_API_BASE_URL at "/api/v1" to use it; API_PROXY_TARGET is the backend address as
  // seen from the Next server.
  async rewrites() {
    const target = process.env.API_PROXY_TARGET ?? "http://localhost:8080";
    return [{ source: "/api/:path*", destination: `${target}/api/:path*` }];
  },
};

export default withNextIntl(nextConfig);
