import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const downloadFile = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    downloadFile: (...args: unknown[]) => downloadFile(...args),
  },
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
    downloadFile.mockReset();
  });

  it("downloads the profile data as a JSON file", async () => {
    get.mockResolvedValue({ profile: { email: "a@b.vn" }, settings: {} });

    const { blob, truncated } = await realExportMyData("profile");
    expect(truncated).toBe(false);

    expect(get).toHaveBeenCalledWith("/identity/me/export", {
      scope: "profile",
    });
    expect(blob.type).toBe("application/json");
    expect(JSON.parse(await readText(blob))).toMatchObject({
      profile: { email: "a@b.vn" },
    });
  });

  it("downloads the submission history as the CSV file the server sends", async () => {
    const csv = new Blob(["submission_id,status"], { type: "text/csv" });
    downloadFile.mockResolvedValue({ blob: csv, header: () => null });

    const { blob, truncated } = await realExportMyData("submissions");

    expect(truncated).toBe(false);
    expect(downloadFile).toHaveBeenCalledWith("/identity/me/export", {
      params: { scope: "submissions" },
    });
    expect(get).not.toHaveBeenCalled();
    expect(blob).toBe(csv);
  });

  it("lets the rate limit and the judge failure of the submission export reach the card", async () => {
    downloadFile.mockRejectedValueOnce(
      new ApiError("IDT-V06", 429, "slow down"),
    );
    await expect(realExportMyData("submissions")).rejects.toMatchObject({
      code: "IDT-V06",
      status: 429,
    });
    downloadFile.mockRejectedValueOnce(new ApiError("IDT-508", 503, "down"));
    await expect(realExportMyData("submissions")).rejects.toMatchObject({
      code: "IDT-508",
      status: 503,
    });
  });

  it("downloads the caller's transcript JSON file without substituting mock data", async () => {
    const file = new Blob(['[{"sessionId":"mine","messages":[]}]'], {
      type: "application/json",
    });
    downloadFile.mockResolvedValue({ blob: file, header: () => null });
    expect(await realExportInterviewTranscripts()).toBe(file);
    expect(downloadFile).toHaveBeenCalledExactlyOnceWith(
      "/identity/me/export/interview-transcripts",
    );
    expect(get).not.toHaveBeenCalled();
  });

  it.each([
    ["IDT-V06", 429],
    ["IDT-509", 503],
    ["IDT-401", 401],
  ])(
    "preserves transcript export error %s instead of returning an empty file",
    async (code, status) => {
      const error = new ApiError(String(code), Number(status), "unavailable");
      downloadFile.mockRejectedValue(error);
      await expect(realExportInterviewTranscripts()).rejects.toBe(error);
    },
  );

  it.each([
    ["true", true],
    ["false", false],
    [null, false],
  ])("reads X-Export-Truncated=%s as truncated=%s", async (value, expected) => {
    const seen: string[] = [];
    downloadFile.mockResolvedValue({
      blob: new Blob(["x"]),
      header: (name: string) => {
        seen.push(name);
        return value;
      },
    });
    expect((await realExportMyData("submissions")).truncated).toBe(expected);
    expect(seen).toEqual(["x-export-truncated"]);
  });
});
