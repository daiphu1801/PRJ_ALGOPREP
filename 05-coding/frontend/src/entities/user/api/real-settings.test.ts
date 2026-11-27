import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();
const patch = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    patch: (...args: unknown[]) => patch(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realGetInterviewPreferences,
  realGetMySettings,
  realUpdateInterviewPreferences,
  realUpdateMySettings,
} from "./real-settings";

const dto = {
  streakReminderEnabled: true,
  weeklyReportEnabled: false,
  preferredInterviewLevel: "MIDDLE",
  preferredMaxTurns: 12,
  preferredHintAllowed: true,
  defaultLanguage: "PYTHON",
};

describe("real settings API", () => {
  beforeEach(() => {
    get.mockReset();
    patch.mockReset();
    localStorage.clear();
  });

  it("reads the server settings and fills the editor options from this device", async () => {
    get.mockResolvedValue(dto);

    expect(await realGetMySettings()).toEqual({
      workspace: {
        defaultLanguage: "PYTHON",
        editorFontSize: "14",
        autosaveDraft: true,
        vimMode: false,
      },
      preferredInterviewLevel: "MIDDLE",
      notifications: {
        streakReminderEnabled: true,
        weeklyReportEnabled: false,
      },
    });
    expect(get).toHaveBeenCalledWith("/identity/me/settings");
  });

  it("sends only the server fields and keeps the editor options on the device", async () => {
    patch.mockResolvedValue({ ...dto, weeklyReportEnabled: true });

    const saved = await realUpdateMySettings({
      workspace: {
        defaultLanguage: "JAVA",
        editorFontSize: "16",
        autosaveDraft: false,
        vimMode: true,
      },
      preferredInterviewLevel: "SENIOR",
      notifications: {
        streakReminderEnabled: false,
        weeklyReportEnabled: true,
      },
    });

    expect(patch).toHaveBeenCalledWith("/identity/me/settings", {
      streakReminderEnabled: false,
      weeklyReportEnabled: true,
      preferredInterviewLevel: "SENIOR",
      defaultLanguage: "JAVA",
    });
    expect(saved.workspace).toMatchObject({
      editorFontSize: "16",
      autosaveDraft: false,
      vimMode: true,
    });
  });

  it("reads the interview parameters from the same settings and sends them as numbers", async () => {
    get.mockResolvedValue({
      ...dto,
      preferredMaxTurns: 16,
      preferredHintAllowed: false,
    });
    expect(await realGetInterviewPreferences()).toEqual({
      maxTurnsPerSession: "16",
      hintAllowed: false,
    });

    patch.mockResolvedValue({ ...dto, preferredMaxTurns: 8 });
    expect(
      await realUpdateInterviewPreferences({
        maxTurnsPerSession: "8",
        hintAllowed: true,
      }),
    ).toEqual({ maxTurnsPerSession: "8", hintAllowed: true });
    expect(patch).toHaveBeenCalledWith("/identity/me/settings", {
      preferredMaxTurns: 8,
      preferredHintAllowed: true,
    });
  });
});
