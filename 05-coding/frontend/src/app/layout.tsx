import type { ReactNode } from "react";
import type { Metadata } from "next";
import { IntlProvider, getLocale } from "@/shared/i18n/server";
import { Providers } from "./providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "AlgoPrep",
  description: "Nền tảng luyện thuật toán và ôn phỏng vấn kỹ thuật tích hợp AI",
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  const locale = await getLocale();

  return (
    <html lang={locale} data-ui-lang={locale} suppressHydrationWarning>
      <body>
        <IntlProvider>
          <Providers>{children}</Providers>
        </IntlProvider>
      </body>
    </html>
  );
}
