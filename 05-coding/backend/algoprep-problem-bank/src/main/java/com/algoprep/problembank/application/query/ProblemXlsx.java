package com.algoprep.problembank.application.query;

import com.algoprep.problembank.domain.model.problem.AuthoringListPage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The management table as an Excel workbook: the eight table columns under a fixed Vietnamese
 * header, written straight to the OOXML parts with {@code java.util.zip} so no spreadsheet library
 * is pulled in for one flat sheet.
 *
 * <p>Text goes in as inline strings and counts as numbers, so a title typed by an author can never
 * run as a formula (it did in the CSV, which needed a leading quote; DD F2 section 28.3, ponytail
 * rung 6). Characters XML 1.0 forbids are dropped.
 */
final class ProblemXlsx {
  static final List<String> HEADER =
      List.of(
          "Mã", "Tiêu đề", "Chủ đề", "Độ khó", "Trạng thái", "Lượt nộp", "Tỉ lệ AC", "Số testcase");
  static final String SHEET = "Bài tập";

  /** Character widths: the code, then a roomy title and topic column, then short ones. */
  private static final int[] WIDTH = {16, 46, 26, 13, 14, 11, 11, 13};

  // Cell styles, in the order of cellXfs in STYLES.
  private static final int HEAD = 1;
  private static final int TEXT = 2;
  private static final int CENTER = 3;
  private static final int PUBLISHED = 4;
  private static final int DRAFT = 5;
  private static final int COUNT = 6;
  private static final int PERCENT = 7;

  private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
  private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
  private static final String REL =
      "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

  private static final String CONTENT_TYPES =
      XML
          + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
          + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
          + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
          + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
          + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
          + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
          + "</Types>";

  private static final String ROOT_RELS =
      XML
          + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
          + "<Relationship Id=\"rId1\" Type=\""
          + REL
          + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
          + "</Relationships>";

  private static final String WORKBOOK =
      XML
          + "<workbook xmlns=\""
          + MAIN
          + "\" xmlns:r=\""
          + REL
          + "\"><sheets><sheet name=\""
          + SHEET
          + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";

  private static final String WORKBOOK_RELS =
      XML
          + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
          + "<Relationship Id=\"rId1\" Type=\""
          + REL
          + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
          + "<Relationship Id=\"rId2\" Type=\""
          + REL
          + "/styles\" Target=\"styles.xml\"/>"
          + "</Relationships>";

  /**
   * Fonts: 0 body, 1 white bold (header), 2 green, 3 amber (status). Fill 2 is the header blue.
   * Format 164 shows the stored percentage with a percent sign.
   */
  private static final String STYLES =
      XML
          + "<styleSheet xmlns=\""
          + MAIN
          + "\">"
          + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"0.00&quot;%&quot;\"/></numFmts>"
          + "<fonts count=\"4\">"
          + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
          + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
          + "<font><b/><sz val=\"11\"/><color rgb=\"FF1E7B34\"/><name val=\"Calibri\"/></font>"
          + "<font><b/><sz val=\"11\"/><color rgb=\"FF9C5700\"/><name val=\"Calibri\"/></font>"
          + "</fonts>"
          + "<fills count=\"3\">"
          + "<fill><patternFill patternType=\"none\"/></fill>"
          + "<fill><patternFill patternType=\"gray125\"/></fill>"
          + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1F4E79\"/><bgColor indexed=\"64\"/></patternFill></fill>"
          + "</fills>"
          + "<borders count=\"2\">"
          + "<border><left/><right/><top/><bottom/><diagonal/></border>"
          + "<border>"
          + "<left style=\"thin\"><color rgb=\"FFBFBFBF\"/></left><right style=\"thin\"><color rgb=\"FFBFBFBF\"/></right>"
          + "<top style=\"thin\"><color rgb=\"FFBFBFBF\"/></top><bottom style=\"thin\"><color rgb=\"FFBFBFBF\"/></bottom>"
          + "<diagonal/></border>"
          + "</borders>"
          + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
          + "<cellXfs count=\"8\">"
          + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
          + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
          + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\" wrapText=\"1\"/></xf>"
          + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
          + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
          + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
          + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>"
          + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>"
          + "</cellXfs>"
          + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
          + "</styleSheet>";

  private ProblemXlsx() {}

  static byte[] render(List<AuthoringListPage.Item> items) {
    var out = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(out)) {
      part(zip, "[Content_Types].xml", CONTENT_TYPES);
      part(zip, "_rels/.rels", ROOT_RELS);
      part(zip, "xl/workbook.xml", WORKBOOK);
      part(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
      part(zip, "xl/styles.xml", STYLES);
      part(zip, "xl/worksheets/sheet1.xml", sheet(items));
    } catch (IOException e) {
      throw new UncheckedIOException(e); // an in-memory stream: not reachable
    }
    return out.toByteArray();
  }

  private static void part(ZipOutputStream zip, String name, String xml) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(xml.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private static String headerRow() {
    var row = new StringBuilder("<row r=\"1\" ht=\"30\" customHeight=\"1\">");
    for (int i = 0; i < HEADER.size(); i++) {
      row.append(text(i, 1, HEAD, HEADER.get(i)));
    }
    return row.append("</row>").toString();
  }

  private static String dataRow(AuthoringListPage.Item item, int r) {
    boolean published = "PUBLISHED".equals(item.status());
    return "<row r=\""
        + r
        + "\">"
        + text(0, r, TEXT, item.code())
        + text(1, r, TEXT, item.title())
        + text(2, r, TEXT, item.topicNames().stream().collect(Collectors.joining("; ")))
        + text(3, r, CENTER, item.levelName())
        + text(4, r, published ? PUBLISHED : DRAFT, published ? "Đã xuất bản" : "Nháp")
        + number(5, r, COUNT, Integer.toString(item.submissionCount()))
        + (item.acRate() == null
            ? "<c r=\"%s%d\" s=\"%d\"/>".formatted(column(6), r, PERCENT)
            : number(6, r, PERCENT, item.acRate().toPlainString()))
        + number(7, r, COUNT, Integer.toString(item.testcaseCount()))
        + "</row>";
  }

  private static String sheet(List<AuthoringListPage.Item> items) {
    var cols = new StringBuilder("<cols>");
    for (int i = 0; i < WIDTH.length; i++) {
      cols.append(
          "<col min=\"%d\" max=\"%d\" width=\"%d\" customWidth=\"1\"/>"
              .formatted(i + 1, i + 1, WIDTH[i]));
    }
    cols.append("</cols>");

    var rows = new StringBuilder(headerRow());
    int r = 1;
    for (var item : items) {
      r++;
      rows.append(dataRow(item, r));
    }

    return XML
        + "<worksheet xmlns=\""
        + MAIN
        + "\">"
        + "<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>"
        + "<dimension ref=\"A1:"
        + column(HEADER.size() - 1)
        + r
        + "\"/>"
        + "<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\">"
        + "<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>"
        + "</sheetView></sheetViews>"
        + "<sheetFormatPr defaultRowHeight=\"18\"/>"
        + cols
        + "<sheetData>"
        + rows
        + "</sheetData>"
        + "<autoFilter ref=\"A1:"
        + column(HEADER.size() - 1)
        + r
        + "\"/>"
        // One landscape page wide when printed, however long the list is.
        + "<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>"
        + "<pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>"
        + "</worksheet>";
  }

  private static String column(int index) {
    return String.valueOf((char) ('A' + index));
  }

  private static String number(int col, int row, int style, String value) {
    return "<c r=\"%s%d\" s=\"%d\"><v>%s</v></c>".formatted(column(col), row, style, value);
  }

  private static String text(int col, int row, int style, String raw) {
    return "<c r=\"%s%d\" s=\"%d\" t=\"inlineStr\"><is><t xml:space=\"preserve\">%s</t></is></c>"
        .formatted(column(col), row, style, escape(raw));
  }

  /** Escapes the markup characters and drops what XML 1.0 cannot carry. */
  static String escape(String raw) {
    if (raw == null) {
      return "";
    }
    var out = new StringBuilder(raw.length());
    raw.codePoints()
        .forEach(
            c -> {
              if (c == '&') {
                out.append("&amp;");
              } else if (c == '<') {
                out.append("&lt;");
              } else if (c == '>') {
                out.append("&gt;");
              } else if (c == '"') {
                out.append("&quot;");
              } else if (c == '\t' || c == '\n' || c == '\r' || xmlChar(c)) {
                out.appendCodePoint(c);
              }
            });
    return out.toString();
  }

  private static boolean xmlChar(int c) {
    return (c >= 0x20 && c <= 0xD7FF)
        || (c >= 0xE000 && c <= 0xFFFD)
        || (c >= 0x10000 && c <= 0x10FFFF);
  }
}
