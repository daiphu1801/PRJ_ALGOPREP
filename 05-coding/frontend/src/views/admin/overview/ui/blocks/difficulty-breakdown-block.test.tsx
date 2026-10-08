import { afterEach, describe, expect, it } from "vitest";
import { act, render, renderHook, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import {
  addProblemLevel,
  removeProblemLevel,
  useProblemLevels,
} from "@/entities/problem";
import messages from "../../../../../../messages/vi.json";
import { DifficultyBreakdownBlock } from "./difficulty-breakdown-block";

function renderBlock() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        <DifficultyBreakdownBlock />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("DifficultyBreakdownBlock", () => {
  afterEach(() => {
    const { result } = renderHook(() => useProblemLevels());
    const extra = result.current.find((l) => l.label === "Rất khó");
    if (extra) act(() => void removeProblemLevel(extra.key));
  });

  it("draws one group per level, extra levels included", async () => {
    act(() => void addProblemLevel("Rất khó"));
    renderBlock();
    for (const label of ["Dễ", "Trung bình", "Khó", "Rất khó"]) {
      expect(await screen.findByText(label)).toBeInTheDocument();
    }
  });
});
