// Save rules of the language-and-limits screen (BD ADM0501 Sheet 9) and the 2026-10-10 fix: a failed
// save must raise only the error toast. `useT` returns the key, so assertions read i18n keys.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, describe, expect, it, vi } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useLocale: () => "vi",
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

const failingSave = vi.fn();
vi.mock("../api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useSaveLanguageConfigPage: () => failingSave(),
}));

import { AdminLanguageConfigView } from "./admin-language-config-view";

function mount(save: () => Promise<void>) {
  failingSave.mockReturnValue({ mutateAsync: save, isPending: false });
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const toasts = renderHook(() => useToasts());
  render(
    <QueryClientProvider client={client}>
      <AdminLanguageConfigView />
    </QueryClientProvider>,
  );
  const tones = () =>
    toasts.result.current.map((item) => [item.tone, item.message]);
  return { tones };
}

const save = () => screen.getByRole("button", { name: "save" });
const changeTime = async (index: number, value: string) =>
  fireEvent.change(
    (await screen.findAllByLabelText("timeMultiplierFor"))[index]!,
    {
      target: { value },
    },
  );

afterEach(() => act(() => toast.clear()));

describe("language config save", () => {
  it("raises only the error toast when the save call rejects", async () => {
    const { tones } = mount(() => Promise.reject(new Error("down")));
    await changeTime(0, "4");
    fireEvent.click(save());
    await vi.waitFor(() => expect(tones()).toEqual([["error", "saveFailed"]]));
  });

  it("refuses a multiplier outside 0.10 to 9.99", async () => {
    const { tones } = mount(() => Promise.resolve());
    await changeTime(0, "50");
    fireEvent.click(save());
    expect(tones()).toEqual([["error", "errorMultiplier"]]);
  });

  it("refuses an effective time limit below 100 ms", async () => {
    const { tones } = mount(() => Promise.resolve());
    await changeTime(1, "1");
    fireEvent.change(screen.getByLabelText("defaults.timeLimitMs.label"), {
      target: { value: "50" },
    });
    fireEvent.click(save());
    expect(tones()).toEqual([["error", "errorTimeRange"]]);
  });

  it("blocks turning every language off", async () => {
    const { tones } = mount(() => Promise.resolve());
    for (const toggle of await screen.findAllByRole("switch"))
      fireEvent.click(toggle);
    fireEvent.click(save());
    expect(tones()).toEqual([["error", "errorLastLanguage"]]);
  });

  it("asks once before disabling a language, then saves", async () => {
    const saveCall = vi.fn(() => Promise.resolve());
    const { tones } = mount(saveCall);
    fireEvent.click((await screen.findAllByRole("switch"))[0]!);
    fireEvent.click(save());

    expect(saveCall).not.toHaveBeenCalled();
    expect(screen.getByText("confirmDisable.title")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "confirmDisable.yes" }));
    await vi.waitFor(() => expect(tones()).toEqual([["success", "saveDone"]]));
    expect(saveCall).toHaveBeenCalledTimes(1);
  });

  it("saves a plain edit without any confirmation", async () => {
    const saveCall = vi.fn(() => Promise.resolve());
    const { tones } = mount(saveCall);
    await changeTime(0, "4");
    fireEvent.click(save());
    await vi.waitFor(() => expect(tones()).toEqual([["success", "saveDone"]]));
    expect(screen.queryByText("confirmDisable.title")).not.toBeInTheDocument();
  });
});
