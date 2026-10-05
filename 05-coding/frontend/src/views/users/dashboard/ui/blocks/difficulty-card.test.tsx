import { afterEach, describe, expect, it } from "vitest";
import { act, render, renderHook, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import { addProblemLevel, removeProblemLevel, useProblemLevels } from "@/entities/problem";
import messages from "../../../../../../messages/vi.json";
import { DifficultyCard } from "./difficulty-card";

function renderCard() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        <DifficultyCard />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("DifficultyCard", () => {
  afterEach(() => {
    const { result } = renderHook(() => useProblemLevels());
    const extra = result.current.find((l) => l.label === "Rất khó");
    if (extra) act(() => void removeProblemLevel(extra.key));
  });

  it("renders one row per seed level with its counts", async () => {
    renderCard();
    expect(await screen.findByText("88 / 180")).toBeInTheDocument();
    expect(screen.getByText("77 / 320")).toBeInTheDocument();
    expect(screen.getByText("17 / 140")).toBeInTheDocument();
  });

  it("renders a 0 / 0 row for a level without data", async () => {
    act(() => void addProblemLevel("Rất khó"));
    renderCard();
    expect(await screen.findByText("Rất khó")).toBeInTheDocument();
    expect(screen.getByText("0 / 0")).toBeInTheDocument();
  });
});
