// F2-14 settings the ADMIN edits (BD ADM0301 Khu vực H, ADM0501 Khu vực F). `useT` returns the key.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { aiGenerationSettings } from "@/entities/problem";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { toast, useToasts } from "@/shared/lib/toast-store";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useLocale: () => "vi",
}));

import { AdminAiConfigView } from "./ai-config";
import { AdminLanguageConfigView } from "./language-config";

function withQuery(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return <QueryClientProvider client={client}>{ui}</QueryClientProvider>;
}

afterEach(() => {
  aiGenerationSettings.set({ maxPerProblem: 2 });
  act(() => toast.clear());
});

describe("per-problem generation cap on the AI config screen", () => {
  it("starts at 2 and the stepper moves the shared setting within 1 to 10", () => {
    render(<AdminAiConfigView />);
    expect(aiGenerationSettings.get().maxPerProblem).toBe(2);

    fireEvent.click(
      screen.getByRole("button", { name: "testcaseGen.increment" }),
    );
    expect(aiGenerationSettings.get().maxPerProblem).toBe(3);

    fireEvent.click(
      screen.getByRole("button", { name: "testcaseGen.decrement" }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "testcaseGen.decrement" }),
    );
    expect(aiGenerationSettings.get().maxPerProblem).toBe(1);
    expect(
      screen.getByRole("button", { name: "testcaseGen.decrement" }),
    ).toBeDisabled();
  });
});

describe("generator script limits on the language config screen", () => {
  it("rejects a run time outside 1 to 300 seconds and an output size outside 1 to 1024 MB", async () => {
    const toasts = renderHook(() => useToasts());
    render(withQuery(<AdminLanguageConfigView />));
    await screen.findByText("Python");

    fireEvent.change(screen.getByLabelText("generator.runtime.label"), {
      target: { value: "0" },
    });
    fireEvent.click(screen.getByRole("button", { name: "save" }));
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "error",
      message: "errorInvalidGenerator",
    });

    act(() => toast.clear());
    fireEvent.change(screen.getByLabelText("generator.runtime.label"), {
      target: { value: "30" },
    });
    fireEvent.change(screen.getByLabelText("generator.output.label"), {
      target: { value: "2000" },
    });
    fireEvent.click(screen.getByRole("button", { name: "save" }));
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "error",
      message: "errorInvalidGenerator",
    });
  });

  it("shows Python as the fixed script language with the BD default limits", async () => {
    render(withQuery(<AdminLanguageConfigView />));
    await screen.findByText("Python");
    expect(screen.getByText("Python")).toBeInTheDocument();
    expect(screen.getByLabelText("generator.runtime.label")).toHaveValue(30);
    expect(screen.getByLabelText("generator.output.label")).toHaveValue(100);
  });
});
