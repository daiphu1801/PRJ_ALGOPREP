// Pairing rule of the bulk testcase upload (SHR0202 EVT-15, owner decision 2026-11-07): `<name>.in` is
// the input and `<name>.out` the expected output of ONE testcase. The limits are the server's
// (03-dd/api/F2_problem-bank.md #45, #46); the server still enforces them, this only reports early.

export const BULK_LIMITS = {
  maxPairs: 100,
  maxSideBytes: 64 * 1024 * 1024,
  maxTotalBytes: 256 * 1024 * 1024,
  maxTestcases: 500,
} as const;

export type FileInfo = { name: string; size: number };

export type BulkPairRow<F extends FileInfo = FileInfo> = {
  /** Wire key sent to the server; maps a PRB-045 item back to `base`. */
  key: string;
  base: string;
  input: F;
  expected: F;
};

export type BulkIssue =
  | {
      code: "extension" | "duplicate" | "unmatched" | "empty" | "sideTooLarge";
      file: string;
    }
  | { code: "tooManyPairs"; count: number; limit: number }
  | { code: "totalTooLarge"; limit: number }
  | { code: "tooManyTestcases"; count: number; limit: number };

export type BulkPairing<F extends FileInfo = FileInfo> = {
  pairs: BulkPairRow<F>[];
  issues: BulkIssue[];
};

function splitName(name: string): { base: string; ext: string } {
  const dot = name.lastIndexOf(".");
  return dot <= 0
    ? { base: name, ext: "" }
    : { base: name.slice(0, dot), ext: name.slice(dot + 1).toLowerCase() };
}

/** Pairs files by base name; every file that cannot be part of a clean pair becomes an issue. */
export function pairTestcaseFiles<F extends FileInfo>(
  files: readonly F[],
  existingTestcases: number,
): BulkPairing<F> {
  const issues: BulkIssue[] = [];
  const inputs = new Map<string, F>();
  const outputs = new Map<string, F>();

  for (const file of files) {
    const { base, ext } = splitName(file.name);
    if (ext !== "in" && ext !== "out") {
      issues.push({ code: "extension", file: file.name });
      continue;
    }
    const side = ext === "in" ? inputs : outputs;
    if (side.has(base)) {
      issues.push({ code: "duplicate", file: file.name });
      continue;
    }
    side.set(base, file);
  }

  const pairs: BulkPairRow<F>[] = [];
  for (const [base, input] of inputs) {
    const expected = outputs.get(base);
    if (!expected) {
      issues.push({ code: "unmatched", file: input.name });
      continue;
    }
    pairs.push({ key: "", base, input, expected });
  }
  for (const [base, output] of outputs) {
    if (!inputs.has(base))
      issues.push({ code: "unmatched", file: output.name });
  }

  pairs.sort((a, b) =>
    a.base.localeCompare(b.base, undefined, { numeric: true }),
  );
  pairs.forEach((pair, index) => {
    pair.key = `p${index + 1}`;
  });

  let total = 0;
  for (const pair of pairs) {
    for (const side of [pair.input, pair.expected]) {
      total += side.size;
      if (side.size <= 0) issues.push({ code: "empty", file: side.name });
      else if (side.size > BULK_LIMITS.maxSideBytes)
        issues.push({ code: "sideTooLarge", file: side.name });
    }
  }
  if (pairs.length > BULK_LIMITS.maxPairs)
    issues.push({
      code: "tooManyPairs",
      count: pairs.length,
      limit: BULK_LIMITS.maxPairs,
    });
  if (total > BULK_LIMITS.maxTotalBytes)
    issues.push({ code: "totalTooLarge", limit: BULK_LIMITS.maxTotalBytes });
  if (existingTestcases + pairs.length > BULK_LIMITS.maxTestcases)
    issues.push({
      code: "tooManyTestcases",
      count: existingTestcases + pairs.length,
      limit: BULK_LIMITS.maxTestcases,
    });

  return { pairs, issues };
}
