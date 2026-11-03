package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.AuditExport;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * Writes an {@link AuditExport} as RFC 4180 CSV: UTF-8 with a byte order mark (so Excel opens the
 * Vietnamese text correctly), CRLF line ends, quoting where needed, and a guard against formula
 * injection. Rows are written and flushed as they come, never collected.
 */
final class AuditCsvWriter {
  private static final int FLUSH_EVERY = 200;
  private static final String FORMULA_STARTS = "=+-@\t\r";

  private AuditCsvWriter() {}

  static void write(AuditExport export, OutputStream out) throws IOException {
    Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    writer.write('﻿');
    writeLine(writer, export.headers());
    Iterator<List<String>> rows = export.rows();
    int count = 0;
    while (rows.hasNext()) {
      writeLine(writer, rows.next());
      if (++count % FLUSH_EVERY == 0) {
        writer.flush();
      }
    }
    writer.flush();
  }

  private static void writeLine(Writer writer, List<String> cells) throws IOException {
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        writer.write(',');
      }
      writer.write(escape(cells.get(i)));
    }
    writer.write("\r\n");
  }

  /**
   * Neutralises a cell that a spreadsheet would run as a formula by prefixing an apostrophe (the
   * OWASP recommendation; the DD is silent), then quotes it when it holds a comma, quote or line
   * break. {@code [SoT: Suy luan]}
   */
  static String escape(String cell) {
    String text = cell == null ? "" : cell;
    if (!text.isEmpty() && FORMULA_STARTS.indexOf(text.charAt(0)) >= 0) {
      text = "'" + text;
    }
    if (text.indexOf(',') >= 0
        || text.indexOf('"') >= 0
        || text.indexOf('\n') >= 0
        || text.indexOf('\r') >= 0) {
      return '"' + text.replace("\"", "\"\"") + '"';
    }
    return text;
  }
}
