// PROTOTYPE mock — no backend endpoint exists yet (05-coding/backend has no Controller).
// Values copied verbatim from 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:402-428 so the
// screen can be reviewed against the mockup without inventing numbers.
import type {
  GeneratorSandbox,
  JudgeDefaults,
  LanguageConfig,
  LanguageConfigPage,
  SandboxConfig,
} from "../../model/types";

// dc.html:402-405. `factor` there is a display string ("x3,0"); stored here as the number it means.
const LANGUAGES: LanguageConfig[] = [
  {
    key: "py",
    name: "Python 3",
    short: "PY",
    compiler: "CPython 3.11.6",
    judgeId: "go-judge #71",
    timeMultiplier: 3,
    memoryMultiplier: 1,
    enabled: true,
  },
  {
    key: "cpp",
    name: "C++ 17",
    short: "C+",
    compiler: "GCC 13.2",
    judgeId: "go-judge #54",
    timeMultiplier: 1,
    memoryMultiplier: 1,
    enabled: true,
  },
  {
    key: "java",
    name: "Java 21",
    short: "JV",
    compiler: "OpenJDK 21.0.2",
    judgeId: "go-judge #62",
    timeMultiplier: 2,
    memoryMultiplier: 2,
    enabled: true,
  },
];

// dc.html:417-420.
const DEFAULTS: JudgeDefaults = {
  timeLimitMs: 1000,
  memoryLimitMb: 256,
  outputLimitKb: 64,
  compileTimeoutSec: 10,
};

// dc.html:272 (`state.sb`) — network off, the other two on.
const SANDBOX: SandboxConfig = {
  networkAccess: false,
  limitChildProcesses: true,
  returnStderr: true,
};

// BD ADM0501 Khu vực F: defaults are the BD's own guess, not an RD figure (Q10).
const GENERATOR: GeneratorSandbox = { maxRuntimeSeconds: 30, maxOutputMb: 100 };

// The last save, kept for the session so reopening the screen shows it; the real call reads the
// server. Not persisted: a page reload goes back to the seed values above.
let current: LanguageConfigPage | null = null;

function seed(): LanguageConfigPage {
  return {
    languages: LANGUAGES.map((language) => ({ ...language })),
    defaults: { ...DEFAULTS },
    sandbox: { ...SANDBOX },
    generator: { ...GENERATOR },
  };
}

/** `GetLanguageConfigs`. */
export async function loadLanguageConfigPage(): Promise<LanguageConfigPage> {
  await new Promise((resolve) => setTimeout(resolve, 150));
  return structuredClone(current ?? seed());
}

/** `UpdateLanguageConfigs`. The delay stands in for the round trip so the saving state is reviewable. */
export async function saveLanguageConfigPage(page: LanguageConfigPage): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 600));
  current = structuredClone(page);
}
