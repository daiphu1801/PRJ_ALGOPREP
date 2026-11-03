package com.algoprep.identity.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.application.dto.AuditExport;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CSV encoding of the export: BOM, header, CRLF, quoting and formula-injection guard. */
class AuditCsvWriterTest {

  private static String csv(List<String> headers, List<List<String>> rows) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    AuditCsvWriter.write(new AuditExport(headers, rows.iterator()), out);
    return out.toString(StandardCharsets.UTF_8);
  }

  @Test
  void startsWithABomThenTheHeaderAndEndsEveryLineWithCrlf() throws Exception {
    String text = csv(List.of("Thời gian", "Nội dung"), List.of(List.of("t1", "c1")));

    assertThat(text).isEqualTo("﻿Thời gian,Nội dung\r\nt1,c1\r\n");
  }

  @Test
  void aHeaderOnlyFileIsValidWhenThereAreNoRows() throws Exception {
    assertThat(csv(List.of("id"), List.of())).isEqualTo("﻿id\r\n");
  }

  @Test
  void cellsWithCommasQuotesOrLineBreaksAreQuotedAndQuotesDoubled() {
    assertThat(AuditCsvWriter.escape("a,b")).isEqualTo("\"a,b\"");
    assertThat(AuditCsvWriter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    assertThat(AuditCsvWriter.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
    assertThat(AuditCsvWriter.escape("a\r\nb")).isEqualTo("\"a\r\nb\"");
    assertThat(AuditCsvWriter.escape("plain")).isEqualTo("plain");
  }

  @Test
  void cellsThatASpreadsheetWouldRunAsAFormulaGetAnApostrophe() {
    for (String start : new String[] {"=", "+", "-", "@", "\t"}) {
      assertThat(AuditCsvWriter.escape(start + "cmd")).startsWith("'" + start);
    }
    assertThat(AuditCsvWriter.escape("=1+1")).isEqualTo("'=1+1");
  }

  @Test
  void aFormulaCellWithACommaIsNeutralisedThenQuoted() {
    assertThat(AuditCsvWriter.escape("=SUM(A1,A2)")).isEqualTo("\"'=SUM(A1,A2)\"");
  }

  @Test
  void aSafeFirstCharacterIsLeftAlone() {
    assertThat(AuditCsvWriter.escape("2026-11-01T10:00:00+07:00"))
        .isEqualTo("2026-11-01T10:00:00+07:00");
    assertThat(AuditCsvWriter.escape("a=b")).isEqualTo("a=b");
    assertThat(AuditCsvWriter.escape(null)).isEmpty();
    assertThat(AuditCsvWriter.escape("")).isEmpty();
  }
}
