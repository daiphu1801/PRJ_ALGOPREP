// Test-only helper: wraps a view under test with the same providers app/providers/index.tsx wires
// in the real app (NextIntlClientProvider for useT, QueryClientProvider for TanStack Query), so a
// component smoke test does not need to hand-mock next-intl or react-query per file. Uses the vi.json
// messages verbatim rather than a fixture, so a missing i18n key fails the test the same way it
// would fail a real screen — a fixture with `{}` messages would swallow that class of bug.
import type { ReactElement, ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../messages/vi.json";

export function withTestProviders(children: ReactNode): ReactElement {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return (
    <QueryClientProvider client={queryClient}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        {children}
      </NextIntlClientProvider>
    </QueryClientProvider>
  );
}
