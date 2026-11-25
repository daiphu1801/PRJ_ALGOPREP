/**
 * Cells copied from Excel or Google Sheets: tab between cells, newline between rows, and a cell that holds
 * a tab, a newline or a quote is wrapped in double quotes. Unlike CSV (`parseCsv`), a quote only opens a
 * quoted cell at the cell start, so a raw one-line cell such as `print("a")` keeps its quotes.
 * Blank rows are dropped.
 */
export function parseTsv(text: string): string[][] {
  const input = text.replace(/^﻿/, "");
  const rows: string[][] = [];
  let cells: string[] = [];
  let cell = "";
  let quoted = false;

  const endRow = () => {
    cells.push(cell);
    if (cells.some((value) => value.trim() !== "")) rows.push(cells);
    cells = [];
    cell = "";
  };

  for (let i = 0; i < input.length; i += 1) {
    const char = input[i]!;
    if (quoted) {
      if (char === '"' && input[i + 1] === '"') {
        cell += '"';
        i += 1;
      } else if (char === '"') quoted = false;
      else cell += char;
    } else if (char === '"' && cell === "") quoted = true;
    else if (char === "\t") {
      cells.push(cell);
      cell = "";
    } else if (char === "\n" || char === "\r") {
      if (char === "\r" && input[i + 1] === "\n") i += 1;
      endRow();
    } else cell += char;
  }
  if (cell !== "" || cells.length > 0) endRow();
  return rows;
}
