import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realExportInterviewTranscripts,
  realExportMyData,
} from "./real-export";

// jsdom's Blob has no `text()`.
const readText = (blob: Blob) =>
  new Promise<string>((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.readAsText(blob);
  });

describe("real export", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("downloads the profile data as a JSON file", async () => {
    get.mockResolvedValue({ profile: { email: "a@b.vn" }, settings: {} });

    const blob = await realExportMyData("profile");

    expect(get).toHaveBeenCalledWith("/identity/me/export", {
      scope: "profile",
    });
    expect(blob.type).toBe("application/json");
    expect(JSON.parse(await readText(blob))).toMatchObject({
      profile: { email: "a@b.vn" },
    });
  });

  it("refuses the exports whose data does not exist yet, without calling the server", async () => {
    await expect(realExportMyData("submissions")).rejects.toMatchObject({
      status: 501,
    });
    await expect(realExportInterviewTranscripts()).rejects.toMatchObject({
      status: 501,
    });
    expect(get).not.toHaveBeenCalled();
  });
});
