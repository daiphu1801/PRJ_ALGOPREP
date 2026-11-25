// How long opening a session really took on this browser (the last few times), so the waiting screen can say
// "usually about N seconds". Real measurements only: with no history it says nothing.
const KEY = "algoprep-mock-interview-open-seconds";
const KEEP = 5;

function read(): number[] {
  try {
    const raw = JSON.parse(localStorage.getItem(KEY) ?? "[]") as unknown;
    return Array.isArray(raw)
      ? raw.filter((n): n is number => typeof n === "number" && n > 0)
      : [];
  } catch {
    return [];
  }
}

export function recordOpenSeconds(seconds: number): void {
  if (!(seconds > 0)) return;
  try {
    localStorage.setItem(
      KEY,
      JSON.stringify([...read(), seconds].slice(-KEEP)),
    );
  } catch {
    // Storage full or blocked: the hint is optional.
  }
}

/** Median of the remembered durations, in whole seconds, or null when nothing was measured yet. */
export function usualOpenSeconds(): number | null {
  const values = [...read()].sort((a, b) => a - b);
  if (values.length === 0) return null;
  return Math.round(values[Math.floor(values.length / 2)]!);
}
