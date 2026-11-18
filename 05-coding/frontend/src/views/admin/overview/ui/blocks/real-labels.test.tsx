// Real mode: the server sends codes, the blocks write the words (vi and en) from `adminOverview.labels`.
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import enMessages from "../../../../../../messages/en.json";
import viMessages from "../../../../../../messages/vi.json";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { SubmissionsByLanguageBlock } from "./submissions-by-language-block";
import { VerdictDistributionBlock } from "./verdict-distribution-block";

function renderBlock(
  node: React.ReactNode,
  locale: "vi" | "en",
  messages: typeof viMessages,
) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <NextIntlClientProvider locale={locale} messages={messages}>
        {node}
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("ADM0101 labels in real mode", () => {
  beforeEach(() => {
    resetApiMock();
    apiMock.get.mockImplementation(async (path: string) =>
      path.endsWith("/verdicts")
        ? {
            slices: [
              { verdict: "ACCEPTED", count: 3, percent: 75 },
              { verdict: "WRONG_ANSWER", count: 1, percent: 25 },
              { verdict: "MEMORY_LIMIT_EXCEEDED", count: 0, percent: 0 },
            ],
          }
        : {
            pointLabels: ["01/11"],
            series: [{ languageCode: "CPP", values: [4] }],
          },
    );
  });

  it("writes the verdict names in Vietnamese, and an unknown code as the code itself", async () => {
    renderBlock(<VerdictDistributionBlock />, "vi", viMessages);
    expect(
      (
        await screen.findAllByText(
          viMessages.adminOverview.labels.verdict.WRONG_ANSWER,
          { exact: false },
        )
      ).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("MEMORY_LIMIT_EXCEEDED", { exact: false }).length,
    ).toBeGreaterThan(0);
  });

  it("writes the same verdicts in English", async () => {
    renderBlock(
      <VerdictDistributionBlock />,
      "en",
      enMessages as typeof viMessages,
    );
    expect(
      (
        await screen.findAllByText(
          enMessages.adminOverview.labels.verdict.WRONG_ANSWER,
          { exact: false },
        )
      ).length,
    ).toBeGreaterThan(0);
  });

  it("names the language from its code", async () => {
    renderBlock(
      <SubmissionsByLanguageBlock />,
      "en",
      enMessages as typeof viMessages,
    );
    expect(
      (await screen.findAllByText("C++", { exact: false })).length,
    ).toBeGreaterThan(0);
  });
});
