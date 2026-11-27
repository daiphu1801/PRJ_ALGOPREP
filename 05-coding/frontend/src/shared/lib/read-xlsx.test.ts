import { readFileSync } from "node:fs";
import { deflateRawSync } from "node:zlib";
import { describe, expect, it } from "vitest";
import type { XlsxError } from "./read-xlsx";
import { XLSX_MAX_BYTES, readXlsx } from "./read-xlsx";

/** Minimal ZIP: local headers plus central directory, every entry deflated. */
function zip(files: Record<string, string | Buffer>): Uint8Array<ArrayBuffer> {
  const locals: Buffer[] = [];
  const central: Buffer[] = [];
  let offset = 0;
  for (const [name, content] of Object.entries(files)) {
    const label = Buffer.from(name);
    const data = deflateRawSync(Buffer.from(content));
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(8, 8);
    local.writeUInt16LE(label.length, 26);
    locals.push(local, label, data);
    const entry = Buffer.alloc(46);
    entry.writeUInt32LE(0x02014b50, 0);
    entry.writeUInt16LE(8, 10);
    entry.writeUInt32LE(data.length, 20);
    entry.writeUInt16LE(label.length, 28);
    entry.writeUInt32LE(offset, 42);
    central.push(entry, label);
    offset += 30 + label.length + data.length;
  }
  const directory = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(Object.keys(files).length, 10);
  end.writeUInt32LE(offset, 16);
  return new Uint8Array(Buffer.concat([...locals, directory, end]));
}

const REL =
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

const book = (sheetData: string, more: Record<string, string | Buffer> = {}) =>
  zip({
    "xl/workbook.xml": `<workbook xmlns:r="${REL}"><sheets><sheet name="Bài" r:id="rId1"/></sheets></workbook>`,
    "xl/_rels/workbook.xml.rels": `<Relationships><Relationship Id="rId1" Target="worksheets/sheet1.xml"/></Relationships>`,
    "xl/worksheets/sheet1.xml": `<worksheet><sheetData>${sheetData}</sheetData></worksheet>`,
    ...more,
  });

const codeOf = (bytes: Uint8Array<ArrayBuffer>) =>
  readXlsx(bytes).then(
    () => "no error",
    (error: unknown) => (error as XlsxError).code,
  );

const template = () =>
  new Uint8Array(readFileSync("public/templates/mau-nhap-bai-toan.xlsx"));

describe("readXlsx", () => {
  it("reads the template made by Excel", async () => {
    const sheet = await readXlsx(template());
    expect(sheet.rows).toHaveLength(6);
    expect(sheet.rows[0]!.slice(0, 3)).toEqual(["Tiêu đề", "Độ khó", "Chủ đề"]);
    expect(sheet.rows[1]!.join("|")).toContain("int(input())\n");
    expect([sheet.dateCells, sheet.errorCells]).toEqual([[], []]);
  });

  it("joins rich text, skips phonetic runs and keeps text that looks like a number", async () => {
    const sheet = await readXlsx(
      book(
        '<row><c r="A1" t="s"><v>0</v></c><c r="C1" t="s"><v>1</v></c></row><row><c r="A2" t="inlineStr"><is><t>a_x000D_\nb</t></is></c></row>',
        {
          "xl/sharedStrings.xml":
            "<sst><si><r><t>Tổng </t></r><r><t>chẵn</t></r><rPh><t>x</t></rPh></si><si><t>007</t></si></sst>",
        },
      ),
    );
    expect(sheet.rows).toEqual([["Tổng chẵn", "", "007"], ["a\nb"]]);
  });

  it("takes the stored result of a formula and a plain number", async () => {
    const sheet = await readXlsx(
      book(
        '<row><c r="A1"><f>1+1</f><v>2</v></c><c r="B1"><v>1.5</v></c></row>',
      ),
    );
    expect(sheet.rows).toEqual([["2", "1.5"]]);
  });

  it("blanks dates and failed formulas, and names the cells", async () => {
    const sheet = await readXlsx(
      book(
        '<row><c r="A1" s="1"><v>45000</v></c><c r="B1" s="2"><v>45000</v></c><c r="C1" t="e"><v>#REF!</v></c><c r="D1" s="0"><v>9</v></c></row>',
        {
          "xl/styles.xml":
            '<styleSheet><numFmts><numFmt numFmtId="164" formatCode="dd/mm/yyyy"/></numFmts><cellXfs><xf numFmtId="0"/><xf numFmtId="14"/><xf numFmtId="164"/></cellXfs></styleSheet>',
        },
      ),
    );
    expect(sheet.rows).toEqual([["", "", "", "9"]]);
    expect(sheet.dateCells).toEqual(["A1", "B1"]);
    expect(sheet.errorCells).toEqual(["C1"]);
  });

  it("reads only the first sheet and says how many there are", async () => {
    const sheet = await readXlsx(
      book('<row><c r="A1"><v>1</v></c></row>', {
        "xl/workbook.xml": `<workbook xmlns:r="${REL}"><sheets><sheet name="Một" r:id="rId1"/><sheet name="Hai" r:id="rId2"/></sheets></workbook>`,
      }),
    );
    expect([sheet.sheetName, sheet.sheetCount]).toEqual(["Một", 2]);
  });

  it.each<[string, () => Uint8Array<ArrayBuffer>, string]>([
    [
      "a file that is not a ZIP",
      () => new Uint8Array(new TextEncoder().encode("a,b,c".repeat(20))),
      "notXlsx",
    ],
    ["an empty file", () => new Uint8Array(0), "notXlsx"],
    [
      "a password-protected or old file",
      () => Uint8Array.of(0xd0, 0xcf, 0x11, 0xe0),
      "protectedOrOld",
    ],
    ["a ZIP that is not a workbook", () => zip({ "a.txt": "x" }), "notXlsx"],
    ["a truncated file", () => template().slice(0, 4000), "notXlsx"],
    [
      "a file above the size cap",
      () => new Uint8Array(XLSX_MAX_BYTES + 1),
      "tooLarge",
    ],
    ["a sheet without data", () => book("<row></row>"), "empty"],
    ["broken XML", () => book("<row>"), "corrupt"],
    [
      "a DOCTYPE",
      () =>
        book("", { "xl/workbook.xml": '<!DOCTYPE a [<!ENTITY b "c">]><a/>' }),
      "corrupt",
    ],
    [
      "a sheet that inflates past the cap",
      () =>
        book("", {
          "xl/worksheets/sheet1.xml": Buffer.alloc(61 * 1024 * 1024, 32),
        }),
      "inflatedTooLarge",
    ],
  ])("refuses %s", async (_name, make, code) => {
    expect(await codeOf(make())).toBe(code);
  });

  it("refuses a damaged body with a valid directory", async () => {
    const bytes = template();
    bytes.fill(0xff, 200, 3000);
    expect(await codeOf(bytes)).toBe("corrupt");
  });
});
