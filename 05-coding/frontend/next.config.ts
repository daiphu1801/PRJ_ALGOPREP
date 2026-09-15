import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

// i18n không dùng route segment [locale] — ngôn ngữ đổi qua cookie NEXT_LOCALE +
// attribute data-ui-lang trên <html>, giữ URL nguyên theo frontend_architecture.md:34-37.
const withNextIntl = createNextIntlPlugin("./src/shared/i18n/request.ts");

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Off by request — the dev-only floating "N" badge (route/build indicator) was mistaken for an
  // app button during UI review. Dev-mode only; never renders in production either way.
  devIndicators: false,
};

export default withNextIntl(nextConfig);
