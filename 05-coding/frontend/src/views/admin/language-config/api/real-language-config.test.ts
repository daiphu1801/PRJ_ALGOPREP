import { beforeEach, expect, it, vi } from "vitest";
const get = vi.fn();
const put = vi.fn();
vi.mock("@/shared/api", async (original) => ({
  ...(await original<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    put: (...args: unknown[]) => put(...args),
  },
}));
import {
  loadRealLanguageConfig,
  saveRealLanguageConfig,
} from "./real-language-config";
const wire = {
  languages: ["PYTHON", "JAVA", "CPP"].map((language) => ({
    language,
    compilerDisplayName: "compiler",
    timeLimitMultiplier: 2,
    memoryLimitMultiplier: 1,
    enabled: true,
    pendingJobCount: 2,
    updatedAt: `version-${language}`,
  })),
  gradingDefaults: {
    timeLimitMs: 1000,
    memoryLimitMb: 256,
    maxOutputSizeKb: 64,
    compileTimeoutSeconds: 30,
    updatedAt: "defaults-v1",
  },
  sandbox: {
    networkAccessEnabled: false,
    childProcessLimitEnabled: true,
    returnStderrToStudent: false,
    updatedAt: "sandbox-v1",
  },
  generatorSandbox: {
    maxRuntimeSeconds: 30,
    maxOutputMb: 64,
    updatedAt: "generator-v1",
  },
};
beforeEach(() => {
  get.mockReset().mockResolvedValue(wire);
  put.mockReset().mockResolvedValue(wire);
});
it("round-trips every row version and excludes read-only fields when saving", async () => {
  const page = await loadRealLanguageConfig();
  page.languages[0]!.timeMultiplier = 3;
  await saveRealLanguageConfig(page);
  const body = put.mock.calls[0]?.[1];
  expect(body.languages[0]).toEqual({
    language: "PYTHON",
    timeLimitMultiplier: 3,
    memoryLimitMultiplier: 1,
    enabled: true,
    updatedAt: "version-PYTHON",
  });
  expect(body.gradingDefaults).toEqual(wire.gradingDefaults);
  expect(body.sandbox).toEqual(wire.sandbox);
  expect(body.generatorSandbox).toEqual(wire.generatorSandbox);
});
it("preserves a concurrent-edit conflict instead of retrying with a newly read version", async () => {
  const page = await loadRealLanguageConfig();
  const error = new Error("conflict");
  put.mockRejectedValue(error);
  await expect(saveRealLanguageConfig(page)).rejects.toBe(error);
  expect(put).toHaveBeenCalledTimes(1);
  expect(get).toHaveBeenCalledTimes(1);
});
