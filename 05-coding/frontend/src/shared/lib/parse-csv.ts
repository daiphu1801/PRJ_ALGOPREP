/**
 * Minimal RFC 4180 CSV reader: comma separator, double-quoted fields, "" as an escaped quote, and
 * newlines inside quotes. Returns one string[] per record; blank lines are dropped and a leading BOM
 * (Excel's "CSV UTF-8") is stripped. Each record carries the 1-based physical line it started on.
 */
export type CsvRecord = { line: number; cells: string[] };

export function parseCsv(text: string): CsvRecord[] {
  const input = text.replace(/^\uFEFF/, "");
  const records: CsvRecord[] = [];
  let cells: string[] = [];
  let cell = "";
  let quoted = false;
  let line = 1;
  let startLine = 1;

  const endRecord = () => {
    cells.push(cell);
    if (cells.some((value) => value.trim() !== ""))
      records.push({ line: startLine, cells });
    cells = [];
    cell = "";
  };

  for (let i = 0; i < input.length; i += 1) {
    const char = input[i]!;
    if (quoted) {
      if (char === '"' && input[i + 1] === '"') {
        cell += '"';
        i += 1;
      } else if (char === '"') {
        quoted = false;
      } else {
        if (char === "\n") line += 1;
        cell += char;
      }
    } else if (char === '"') {
      quoted = true;
    } else if (char === ",") {
      cells.push(cell);
      cell = "";
    } else if (char === "\n" || char === "\r") {
      if (char === "\r" && input[i + 1] === "\n") i += 1;
      endRecord();
      line += 1;
      startLine = line;
    } else {
      cell += char;
    }
  }
  if (cell !== "" || cells.length > 0) endRecord();
  return records;
}

/** Wraps a value for CSV output when it holds a comma, quote or newline. */
export const csvCell = (value: string) =>
  /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
