// View-level tests of the Settings actions that talk to the backend (BD USR0503, DD F1 #14, #15,
// #16): saving preferences (two independent calls, BR-07) and exporting data. Deleting the account
// and changing the password have their own tests next to their features. HTTP is faked for the
// preferences form; the export card mocks the entity call because the learner UI only reaches the
// not-yet-built exports.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), refresh: vi.fn() }),
}));

const exportMyData = vi.fn();
const exportMyInterviewTranscripts = vi.fn();
vi.mock("@/entities/user", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  exportMyData: (...args: unknown[]) => exportMyData(...args),
  exportMyInterviewTranscripts: (...args: unknown[]) =>
    exportMyInterviewTranscripts(...args),
}));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { DataExportCard } from "./data-export-card";
import { PreferencesForm } from "./preferences-form";

const s = viMessages.settings;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const SETTINGS = "/identity/me/settings";

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

describe("PreferencesForm save", () => {
  const dto = {
    streakReminderEnabled: true,
    weeklyReportEnabled: false,
    preferredInterviewLevel: "MIDDLE",
    preferredMaxTurns: 12,
    preferredHintAllowed: true,
    defaultLanguage: "PYTHON",
  };

  beforeEach(() => {
    resetApiMock();
    localStorage.clear();
    apiMock.get.mockResolvedValue(dto);
    act(() => toast.clear());
  });

  async function changeBothGroups() {
    render(withTestProviders(<PreferencesForm />));
    fireEvent.click(
      await screen.findByRole("switch", { name: s.notification.weeklyReport }),
    );
    fireEvent.click(
      screen.getByRole("switch", { name: s.interview.hintAllowed }),
    );
    fireEvent.click(screen.getByRole("button", { name: s.saveBar.save }));
  }

  const isInterviewCall = (body: Record<string, unknown>) =>
    "preferredMaxTurns" in body;

  it("saves the settings group and the interview group as two separate PATCH calls", async () => {
    const read = watch();
    apiMock.patch.mockResolvedValue(dto);

    await changeBothGroups();

    await waitFor(() => expect(apiMock.patch).toHaveBeenCalledTimes(2));
    const bodies = apiMock.patch.mock.calls.map(
      (call) => call[1] as Record<string, unknown>,
    );
    expect(apiMock.patch.mock.calls.every((call) => call[0] === SETTINGS)).toBe(
      true,
    );
    expect(bodies.find((b) => !isInterviewCall(b))).toMatchObject({
      weeklyReportEnabled: true,
    });
    expect(bodies.find(isInterviewCall)).toEqual({
      preferredMaxTurns: 12,
      preferredHintAllowed: false,
    });
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", s.saveBar.saved]),
    );
  });

  it.each(["IDT-413", "IDT-503", "IDT-105"])(
    "reports a failed settings group (%s) with the backend's own message while the interview group still saves",
    async (code) => {
      const read = watch();
      apiMock.patch.mockImplementation(
        (_path: string, body: Record<string, unknown>) =>
          isInterviewCall(body)
            ? Promise.resolve(dto)
            : Promise.reject(new ApiError(code, 422)),
      );

      await changeBothGroups();

      await waitFor(() =>
        expect(read()).toContainEqual([
          "error",
          apiErrors[code] ?? apiErrors.UNKNOWN_ERROR,
        ]),
      );
      expect(read()).toContainEqual(["success", s.interview.saved]);
    },
  );

  it("falls back to the save-failed toast when the settings call fails without an API error", async () => {
    const read = watch();
    apiMock.patch.mockImplementation(
      (_path: string, body: Record<string, unknown>) =>
        isInterviewCall(body)
          ? Promise.resolve(dto)
          : Promise.reject(new TypeError("network down")),
    );

    await changeBothGroups();

    await waitFor(() =>
      expect(read()).toContainEqual(["error", s.saveBar.saveFailed]),
    );
  });

  it.each(["IDT-V18", "IDT-503"])(
    "warns when only the interview group fails (%s) and still confirms the settings group",
    async (code) => {
      const read = watch();
      apiMock.patch.mockImplementation(
        (_path: string, body: Record<string, unknown>) =>
          isInterviewCall(body)
            ? Promise.reject(new ApiError(code, 422))
            : Promise.resolve(dto),
      );

      await changeBothGroups();

      await waitFor(() =>
        expect(read()).toContainEqual([
          "warning",
          s.interview.saveFailedWarning,
        ]),
      );
      expect(read()).toContainEqual(["success", s.saveBar.savedSettings]);
    },
  );

  it("shows the load error with a retry when the settings cannot be read", async () => {
    apiMock.get.mockRejectedValue(new ApiError("IDT-503", 503));
    render(withTestProviders(<PreferencesForm />));
    expect(await screen.findByText(s.loadError)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: s.retry })).toBeInTheDocument();
  });
});

describe("DataExportCard", () => {
  beforeEach(() => {
    exportMyData.mockReset();
    exportMyInterviewTranscripts.mockReset();
    act(() => toast.clear());
    URL.createObjectURL = vi.fn(() => "blob:test");
    URL.revokeObjectURL = vi.fn();
  });

  const exportSubmissions = () =>
    fireEvent.click(screen.getByRole("button", { name: /Xuất lượt nộp/ }));

  it("downloads the file and toasts success", async () => {
    const read = watch();
    exportMyData.mockResolvedValue(new Blob(["a,b"]));
    render(withTestProviders(<DataExportCard />));

    exportSubmissions();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", s.dataExport.exported]),
    );
    expect(exportMyData).toHaveBeenCalledWith("submissions");
  });

  it("shows apiErrors.IDT-V06 when the export was requested again within 5 minutes", async () => {
    const read = watch();
    exportMyData.mockRejectedValue(new ApiError("IDT-V06", 429));
    render(withTestProviders(<DataExportCard />));

    exportSubmissions();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors["IDT-V06"]]),
    );
    expect(screen.getByRole("button", { name: /Xuất lượt nộp/ })).toBeEnabled();
  });

  it.each([
    ["not built yet", new ApiError("NOT_IMPLEMENTED", 501)],
    ["an outage", new ApiError("IDT-503", 503)],
    ["a non-API failure", new Error("boom")],
  ])("shows the generic export failure for %s", async (_name, error) => {
    const read = watch();
    exportMyInterviewTranscripts.mockRejectedValue(error);
    render(withTestProviders(<DataExportCard />));

    fireEvent.click(
      screen.getByRole("button", { name: /Xuất hội thoại phỏng vấn/ }),
    );

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", s.dataExport.exportFailed]),
    );
  });
});
