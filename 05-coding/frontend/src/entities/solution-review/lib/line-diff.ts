export type DiffLineKind = "same" | "add" | "del";

export type DiffLine = { kind: DiffLineKind; text: string };

/**
 * Line diff of original vs suggested code (LCS). The DB stores only the suggested code (1.3a,
 * `diffText` is not persisted), so the view derives the diff here; the real app lets Monaco's diff
 * editor do the same job. ponytail: O(n*m) table, fine for the 50000-char cap.
 */
export function lineDiff(original: string[], suggested: string[]): DiffLine[] {
  const n = original.length;
  const m = suggested.length;
  // Flat table, row stride m + 1; cells default to 0 so no index can be undefined.
  const lcs = new Array<number>((n + 1) * (m + 1)).fill(0);
  const at = (i: number, j: number) => lcs[i * (m + 1) + j] ?? 0;
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      lcs[i * (m + 1) + j] =
        original[i] === suggested[j]
          ? at(i + 1, j + 1) + 1
          : Math.max(at(i + 1, j), at(i, j + 1));
    }
  }
  const out: DiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (original[i] === suggested[j]) {
      out.push({ kind: "same", text: original[i] ?? "" });
      i++;
      j++;
    } else if (at(i + 1, j) >= at(i, j + 1)) {
      out.push({ kind: "del", text: original[i++] ?? "" });
    } else {
      out.push({ kind: "add", text: suggested[j++] ?? "" });
    }
  }
  while (i < n) out.push({ kind: "del", text: original[i++] ?? "" });
  while (j < m) out.push({ kind: "add", text: suggested[j++] ?? "" });
  return out;
}
