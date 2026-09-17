/**
 * Judge language configuration. Exactly three languages, locked by README section 6 and
 * PROTOTYPE_DEBT 1.1 (option A) — `language_configs` is seeded, A3 edits values only and can
 * neither add nor remove a row (02-bd/screens/admin/admin_language_config.md section 1).
 */
export type LanguageKey = "py" | "cpp" | "java";

export type LanguageConfig = {
  key: LanguageKey;
  /** Display name, e.g. "Python 3". */
  name: string;
  /** Two-character chip label, e.g. "PY". */
  short: string;
  /** Compiler/runtime string, e.g. "CPython 3.11.6". */
  compiler: string;
  /** Internal go-judge environment id, e.g. "go-judge #71". */
  judgeId: string;
  /** Multiplies the system-wide default time limit for this language (F2-10). */
  timeMultiplier: number;
  /**
   * Multiplies the default memory limit. Derived from the mockup rather than stated: it shows
   * 256 MB for Python and C++ but 512 MB for Java against a 256 MB default
   * (dc.html:402-404, :418), which only works if memory scales per language the way time does —
   * `02-bd/screens/admin/admin_language_config.md` section 3.1 names `memory_limit_multiplier`.
   */
  memoryMultiplier: number;
  enabled: boolean;
};

/** System-wide fallbacks, applied when a problem declares no limit of its own (F4-11). */
export type JudgeDefaults = {
  timeLimitMs: number;
  memoryLimitMb: number;
  outputLimitKb: number;
  compileTimeoutSec: number;
};

/**
 * The three go-judge sandbox parameters spelled out by DEC-2026-0831-judge-orchestration-ops-details.
 *
 * Modelled as ONE global set, not per-language, following the static prototype
 * (09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:271 `state.sb` is a flat object with no
 * language key). This contradicts 02-bd/database/judge-orchestration.md section 1.5, which puts the
 * three columns on each `language_configs` row. BD section 3.3 raises it as open question Q1 and
 * needs a DEC before DD — see the debt row in 06-plan/PROTOTYPE_DEBT.md section 9.
 */
export type SandboxConfig = {
  networkAccess: boolean;
  limitChildProcesses: boolean;
  returnStderr: boolean;
};

export type LanguageConfigPage = {
  languages: LanguageConfig[];
  defaults: JudgeDefaults;
  sandbox: SandboxConfig;
};

/** Effective limits shown read-only in the table: defaults scaled by the language multipliers. */
export function effectiveTimeLimitMs(defaults: JudgeDefaults, language: LanguageConfig): number {
  return Math.round(defaults.timeLimitMs * language.timeMultiplier);
}

export function effectiveMemoryLimitMb(
  defaults: JudgeDefaults,
  language: LanguageConfig,
): number {
  return Math.round(defaults.memoryLimitMb * language.memoryMultiplier);
}
