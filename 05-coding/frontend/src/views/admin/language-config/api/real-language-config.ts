import { api, ApiError } from "@/shared/api";
import type { LanguageConfigPage, LanguageKey } from "../model/types";
export type WireLanguageConfig = {
  languages: {
    language: "PYTHON" | "JAVA" | "CPP";
    compilerDisplayName: string;
    timeLimitMultiplier: number;
    memoryLimitMultiplier: number;
    enabled: boolean;
    pendingJobCount: number;
    updatedAt: string;
  }[];
  gradingDefaults: {
    timeLimitMs: number;
    memoryLimitMb: number;
    maxOutputSizeKb: number;
    compileTimeoutSeconds: number;
    updatedAt: string;
  };
  sandbox: {
    networkAccessEnabled: boolean;
    childProcessLimitEnabled: boolean;
    returnStderrToStudent: boolean;
    updatedAt: string;
  };
  generatorSandbox: {
    maxRuntimeSeconds: number;
    maxOutputMb: number;
    updatedAt: string;
  };
};
function toPage(wire: WireLanguageConfig): LanguageConfigPage {
  const keys = { PYTHON: "py", JAVA: "java", CPP: "cpp" } as const;
  const names = { PYTHON: "Python", JAVA: "Java", CPP: "C++" };
  return {
    languages: wire.languages.map((item) => ({
      key: keys[item.language],
      name: names[item.language],
      short: item.language === "PYTHON" ? "PY" : item.language.slice(0, 2),
      compiler: item.compilerDisplayName,
      timeMultiplier: item.timeLimitMultiplier,
      memoryMultiplier: item.memoryLimitMultiplier,
      enabled: item.enabled,
    })),
    defaults: {
      timeLimitMs: wire.gradingDefaults.timeLimitMs,
      memoryLimitMb: wire.gradingDefaults.memoryLimitMb,
      outputLimitKb: wire.gradingDefaults.maxOutputSizeKb,
      compileTimeoutSec: wire.gradingDefaults.compileTimeoutSeconds,
    },
    sandbox: {
      networkAccess: wire.sandbox.networkAccessEnabled,
      limitChildProcesses: wire.sandbox.childProcessLimitEnabled,
      returnStderr: wire.sandbox.returnStderrToStudent,
    },
    generator: {
      maxRuntimeSeconds: wire.generatorSandbox.maxRuntimeSeconds,
      maxOutputMb: wire.generatorSandbox.maxOutputMb,
    },
    versions: {
      languages: Object.fromEntries(
        wire.languages.map((item) => [keys[item.language], item.updatedAt]),
      ) as Record<LanguageKey, string>,
      defaults: wire.gradingDefaults.updatedAt,
      sandbox: wire.sandbox.updatedAt,
      generator: wire.generatorSandbox.updatedAt,
    },
  };
}
export async function loadRealLanguageConfig() {
  return toPage(
    await api.get<WireLanguageConfig>("/judge/admin/language-config"),
  );
}
export async function saveRealLanguageConfig(page: LanguageConfigPage) {
  if (!page.versions)
    throw new ApiError(
      "UNKNOWN_ERROR",
      409,
      "Reload configuration before saving",
    );
  const codes = { py: "PYTHON", java: "JAVA", cpp: "CPP" };
  return toPage(
    await api.put<WireLanguageConfig>("/judge/admin/language-config", {
      languages: page.languages.map((item) => ({
        language: codes[item.key],
        timeLimitMultiplier: item.timeMultiplier,
        memoryLimitMultiplier: item.memoryMultiplier,
        enabled: item.enabled,
        updatedAt: page.versions!.languages[item.key],
      })),
      gradingDefaults: {
        timeLimitMs: page.defaults.timeLimitMs,
        memoryLimitMb: page.defaults.memoryLimitMb,
        maxOutputSizeKb: page.defaults.outputLimitKb,
        compileTimeoutSeconds: page.defaults.compileTimeoutSec,
        updatedAt: page.versions.defaults,
      },
      sandbox: {
        networkAccessEnabled: page.sandbox.networkAccess,
        childProcessLimitEnabled: page.sandbox.limitChildProcesses,
        returnStderrToStudent: page.sandbox.returnStderr,
        updatedAt: page.versions.sandbox,
      },
      generatorSandbox: {
        ...page.generator,
        updatedAt: page.versions.generator,
      },
    }),
  );
}
