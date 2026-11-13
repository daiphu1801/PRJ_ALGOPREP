package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.ExportProblemsQuery;
import com.algoprep.problembank.domain.model.AuthoringListPage;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemCsvExportTest {
  private final ProblemAuthoringListRepository repository =
      mock(ProblemAuthoringListRepository.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final ExportProblemsCsvQueryHandler handler =
      new ExportProblemsCsvQueryHandler(repository, clock);
  private final UUID actor = UUID.randomUUID();
  private final AuthoringScope teacher = new AuthoringScope(actor, false);

  private static AuthoringListPage.Item item(String title, BigDecimal ac) {
    return new AuthoringListPage.Item(
        UUID.randomUUID(),
        "TWO-SUM",
        title,
        List.of("Mảng", "Băm"),
        UUID.randomUUID(),
        "EASY",
        "Dễ",
        "PUBLISHED",
        12,
        1,
        ac,
        8,
        Instant.parse("2026-11-05T00:00:00Z"));
  }

  @Test
  void theFileHasABomTheVietnameseHeaderAndOneRowPerProblem() {
    String csv =
        ProblemCsv.render(List.of(item("Two Sum", new BigDecimal("43.50")), item("Other", null)));

    assertThat(csv)
        .startsWith("﻿Mã,Tiêu đề,Chủ đề,Độ khó,Trạng thái,Lượt nộp,Tỉ lệ AC,Số testcase\r\n");
    var lines = csv.split("\r\n");
    assertThat(lines).hasSize(3); // header + two rows
    assertThat(lines[1]).isEqualTo("TWO-SUM,Two Sum,Mảng; Băm,Dễ,Đã xuất bản,12,43.50,8");
    assertThat(lines[2]).endsWith(",12,,8"); // no statistics: an empty AC cell, not 0
  }

  @Test
  void aCellThatAFormulaCouldRunGetsALeadingQuote() {
    for (String bad : new String[] {"=SUM(1,1)", "+1", "-1", "@cmd", "\tx", "\nx"}) {
      assertThat(ProblemCsv.cell(bad)).as(bad).contains("'" + bad.replace("\"", "\"\""));
    }
    assertThat(ProblemCsv.cell("=SUM(1,1)"))
        .isEqualTo("\"'=SUM(1,1)\""); // quoted too: it has a comma
    assertThat(ProblemCsv.cell("plain")).isEqualTo("plain");
  }

  @Test
  void commasQuotesAndNewlinesAreQuotedAndNullIsEmpty() {
    assertThat(ProblemCsv.cell("a,b")).isEqualTo("\"a,b\"");
    assertThat(ProblemCsv.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    assertThat(ProblemCsv.cell("two\nlines")).isEqualTo("\"two\nlines\"");
    assertThat(ProblemCsv.cell(null)).isEmpty();
  }

  @Test
  void exactlyOneOfIdsAndFilterIsRequired() {
    var filter = new ExportProblemsQuery.Filter(null, null, null);
    for (var query :
        new ExportProblemsQuery[] {
          new ExportProblemsQuery(teacher, null, null),
          new ExportProblemsQuery(teacher, List.of(UUID.randomUUID()), filter),
          new ExportProblemsQuery(teacher, List.of(), null),
        }) {
      assertThatThrownBy(() -> handler.export(query))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-001"));
    }
  }

  @Test
  void selectedIdsAreExportedInsideTheCallersScopeWithTheLocalDateInTheName() {
    when(clock.now()).thenReturn(Instant.parse("2026-11-05T18:00:00Z")); // already the 6th in UTC+7
    UUID id = UUID.randomUUID();
    when(repository.export(any(), any(), any(), any(), any()))
        .thenReturn(List.of(item("Two Sum", null)));

    var csv = handler.export(new ExportProblemsQuery(teacher, List.of(id, id), null));

    assertThat(csv.filename()).isEqualTo("problems-20261106.csv");
    assertThat(new String(csv.content(), StandardCharsets.UTF_8)).contains("Two Sum");
    verify(repository).export(actor, List.of(id), null, null, null); // the repeated id is sent once
  }

  @Test
  void theFilterIsValidatedAndPassedAsIs() {
    when(clock.now()).thenReturn(Instant.parse("2026-11-05T00:00:00Z"));
    UUID level = UUID.randomUUID();
    var admin = new AuthoringScope(actor, true);

    handler.export(
        new ExportProblemsQuery(
            admin, null, new ExportProblemsQuery.Filter(" abc ", level.toString(), "PUBLISHED")));
    verify(repository)
        .export(
            isNull(),
            isNull(),
            org.mockito.ArgumentMatchers.eq("abc"),
            org.mockito.ArgumentMatchers.eq(level),
            org.mockito.ArgumentMatchers.eq("PUBLISHED"));

    for (var bad :
        new ExportProblemsQuery.Filter[] {
          new ExportProblemsQuery.Filter("x".repeat(101), null, null),
          new ExportProblemsQuery.Filter(null, null, "DRAFT"),
          new ExportProblemsQuery.Filter(null, "nope", null)
        }) {
      assertThatThrownBy(() -> handler.export(new ExportProblemsQuery(admin, null, bad)))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-001"));
    }
  }
}
