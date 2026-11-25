/**
 * Reads the first sheet of an .xlsx file as text cells. An .xlsx is a ZIP of XML files, so the platform's
 * `DecompressionStream` and `DOMParser` are enough and no library is needed.
 *
 * The file comes from outside, so sizes are capped before and while inflating, and a document with a DOCTYPE
 * is refused (entity expansion). ponytail: no ZIP64 and no encrypted files; Excel writes neither for a file
 * this small unless a password is set.
 */
export const XLSX_MAX_BYTES = 10 * 1024 * 1024;
const INFLATE_MAX_BYTES = 60 * 1024 * 1024;
const MAX_COLUMNS = 200;

export type XlsxErrorCode =
  | "tooLarge"
  | "protectedOrOld"
  | "notXlsx"
  | "unsupported"
  | "inflatedTooLarge"
  | "corrupt"
  | "empty";

export class XlsxError extends Error {
  constructor(readonly code: XlsxErrorCode) {
    super(code);
    this.name = "XlsxError";
  }
}

export type XlsxSheet = {
  /** Non-blank rows, blank cells as "". */
  rows: string[][];
  /** Cells left blank because Excel turned the value into a date; the typed text is gone from the file. */
  dateCells: string[];
  /** Cells left blank because their formula failed. */
  errorCells: string[];
  sheetName: string;
  sheetCount: number;
};

const OFFICE_REL =
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

type ZipEntry = { method: number; size: number; offset: number };

/** Entries of the central directory, which is what every ZIP writer fills in correctly. */
function zipEntries(bytes: Uint8Array, view: DataView): Map<string, ZipEntry> {
  let end = bytes.length - 22;
  while (end >= 0 && view.getUint32(end, true) !== 0x06054b50) end -= 1;
  if (end < 0) throw new XlsxError("notXlsx");
  const entries = new Map<string, ZipEntry>();
  const decoder = new TextDecoder();
  let at = view.getUint32(end + 16, true);
  for (let left = view.getUint16(end + 10, true); left > 0; left -= 1) {
    if (view.getUint32(at, true) !== 0x02014b50) throw new XlsxError("corrupt");
    const nameLength = view.getUint16(at + 28, true);
    entries.set(decoder.decode(bytes.subarray(at + 46, at + 46 + nameLength)), {
      method: view.getUint16(at + 10, true),
      size: view.getUint32(at + 20, true),
      offset: view.getUint32(at + 42, true),
    });
    at +=
      46 +
      nameLength +
      view.getUint16(at + 30, true) +
      view.getUint16(at + 32, true);
  }
  return entries;
}

const inflate = (raw: Uint8Array<ArrayBuffer>) =>
  new Response(raw).body!.pipeThrough(new DecompressionStream("deflate-raw"));

function xmlReader(bytes: Uint8Array<ArrayBuffer>) {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const entries = zipEntries(bytes, view);
  let budget = INFLATE_MAX_BYTES;

  return async (name: string): Promise<Document | null> => {
    const entry = entries.get(name);
    if (!entry) return null;
    const start =
      entry.offset +
      30 +
      view.getUint16(entry.offset + 26, true) +
      view.getUint16(entry.offset + 28, true);
    const raw = bytes.subarray(start, start + entry.size);
    if (entry.method !== 0 && entry.method !== 8)
      throw new XlsxError("unsupported");

    const decoder = new TextDecoder();
    let text = "";
    const reader = (
      entry.method === 8 ? inflate(raw) : new Response(raw).body!
    ).getReader();
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      budget -= value.length;
      if (budget < 0) {
        void reader.cancel();
        throw new XlsxError("inflatedTooLarge");
      }
      text += decoder.decode(value, { stream: true });
    }
    text += decoder.decode();
    if (text.includes("<!DOCTYPE")) throw new XlsxError("corrupt");
    const doc = new DOMParser().parseFromString(text, "application/xml");
    if (doc.getElementsByTagName("parsererror").length > 0)
      throw new XlsxError("corrupt");
    return doc;
  };
}

const tags = (node: Document | Element, name: string) =>
  Array.from(node.getElementsByTagNameNS("*", name));

/** Text of a shared or inline string: all rich-text runs, without phonetic hints, control codes decoded. */
const textOf = (node: Element) =>
  tags(node, "t")
    .filter((t) => t.parentElement?.localName !== "rPh")
    .map((t) => t.textContent)
    .join("")
    .replace(/_x([0-9A-Fa-f]{4})_/g, (_, hex: string) =>
      String.fromCharCode(parseInt(hex, 16)),
    )
    .replace(/\r\n?/g, "\n");

/** "AB12" -> 27. */
const columnOf = (ref: string) =>
  [...ref.replace(/\d/g, "")].reduce(
    (total, char) => total * 26 + char.charCodeAt(0) - 64,
    0,
  ) - 1;

/** True for a cell style whose number format shows a date or a time. */
function dateStyles(
  styles: Document | null,
): (style: string | null) => boolean {
  if (!styles) return () => false;
  const custom = new Map(
    tags(styles, "numFmt").map((format) => [
      Number(format.getAttribute("numFmtId")),
      (format.getAttribute("formatCode") ?? "").replace(
        /"[^"]*"|\[[^\]]*\]/g,
        "",
      ),
    ]),
  );
  const cellFormats = tags(styles, "cellXfs")[0];
  const ids = cellFormats
    ? tags(cellFormats, "xf").map((xf) => Number(xf.getAttribute("numFmtId")))
    : [];
  return (style) => {
    const id = ids[Number(style)];
    if (id === undefined) return false;
    return (
      (id >= 14 && id <= 22) ||
      (id >= 45 && id <= 47) ||
      /[ymdh]/i.test(custom.get(id) ?? "")
    );
  };
}

export async function readXlsx(
  bytes: Uint8Array<ArrayBuffer>,
): Promise<XlsxSheet> {
  if (bytes.length > XLSX_MAX_BYTES) throw new XlsxError("tooLarge");
  // OLE container: an encrypted .xlsx or the old binary .xls.
  if (bytes[0] === 0xd0 && bytes[1] === 0xcf)
    throw new XlsxError("protectedOrOld");
  try {
    return await readSheet(bytes);
  } catch (error) {
    if (error instanceof XlsxError) throw error;
    // Out-of-range offsets and broken streams of a truncated file end here.
    throw new XlsxError("corrupt");
  }
}

async function readSheet(bytes: Uint8Array<ArrayBuffer>): Promise<XlsxSheet> {
  const open = xmlReader(bytes);
  const workbook = await open("xl/workbook.xml");
  const rels = await open("xl/_rels/workbook.xml.rels");
  if (!workbook || !rels) throw new XlsxError("notXlsx");
  const sheets = tags(workbook, "sheet");
  const first = sheets[0];
  const relId =
    first?.getAttributeNS(OFFICE_REL, "id") ?? first?.getAttribute("r:id");
  const target = tags(rels, "Relationship")
    .find((rel) => rel.getAttribute("Id") === relId)
    ?.getAttribute("Target");
  if (!first || !target) throw new XlsxError("corrupt");

  const sheet = await open(
    target.startsWith("/") ? target.slice(1) : `xl/${target}`,
  );
  if (!sheet) throw new XlsxError("corrupt");
  const strings = await open("xl/sharedStrings.xml");
  const shared = strings ? tags(strings, "si").map(textOf) : [];
  const isDate = dateStyles(await open("xl/styles.xml"));

  const rows: string[][] = [];
  const dateCells: string[] = [];
  const errorCells: string[] = [];
  for (const row of tags(sheet, "row")) {
    const cells: string[] = [];
    for (const cell of tags(row, "c")) {
      const ref = cell.getAttribute("r") ?? "";
      const column = ref ? columnOf(ref) : cells.length;
      if (column < 0 || column >= MAX_COLUMNS) continue;
      while (cells.length <= column) cells.push("");
      const type = cell.getAttribute("t") ?? "n";
      const raw = tags(cell, "v")[0]?.textContent ?? "";
      if (type === "s") cells[column] = shared[Number(raw)] ?? "";
      else if (type === "inlineStr") cells[column] = textOf(cell);
      else if (type === "e") errorCells.push(ref);
      else if (type === "n" && raw && isDate(cell.getAttribute("s")))
        dateCells.push(ref);
      else cells[column] = raw.replace(/\r\n?/g, "\n");
    }
    if (cells.some((value) => value.trim() !== "")) rows.push(cells);
  }
  if (rows.length === 0) throw new XlsxError("empty");
  return {
    rows,
    dateCells,
    errorCells,
    sheetName: first.getAttribute("name") ?? "",
    sheetCount: sheets.length,
  };
}
