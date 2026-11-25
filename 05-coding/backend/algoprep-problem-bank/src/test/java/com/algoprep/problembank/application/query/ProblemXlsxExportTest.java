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
import com.algoprep.problembank.application.dto.bulk.ExportProblemsQuery;
import com.algoprep.problembank.domain.model.problem.AuthoringListPage;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemXlsxExportTest {
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
  void theWorkbookHasTheVietnameseHeaderAndOneTypedRowPerProblem() throws Exception {
    var sheet =
        Workbook.read(
            ProblemXlsx.render(
                List.of(item("Two Sum", new BigDecimal("43.50")), item("Other", null))));

    assertThat(sheet.parts())
        .containsExactlyInAnyOrder(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml");
    assertThat(sheet.name()).isEqualTo("Bài tập");
    assertThat(sheet.row(1))
        .containsExactly(
            "Mã",
            "Tiêu đề",
            "Chủ đề",
            "Độ khó",
            "Trạng thái",
            "Lượt nộp",
            "Tỉ lệ AC",
            "Số testcase");
    assertThat(sheet.row(2))
        .containsExactly(
            "TWO-SUM", "Two Sum", "Mảng; Băm", "Dễ", "Đã xuất bản", "12", "43.50", "8");
    // No statistics: an empty AC cell, not 0.
    assertThat(sheet.row(3).get(6)).isEmpty();
    assertThat(sheet.numbers("F2", "G2", "H2")).containsExactly(true, true, true);
    assertThat(sheet.numbers("A2", "E2")).containsExactly(false, false);
    assertThat(sheet.xml()).contains("<autoFilter ref=\"A1:H3\"/>").contains("state=\"frozen\"");
  }

  @Test
  void textThatAFormulaCouldRunStaysTextAndNeedsNoQuote() throws Exception {
    var titles = List.of("=SUM(1,1)", "+1", "-1", "@cmd", "\tx");
    var sheet =
        Workbook.read(ProblemXlsx.render(titles.stream().map(title -> item(title, null)).toList()));

    for (int i = 0; i < titles.size(); i++) {
      assertThat(sheet.row(i + 2).get(1)).as(titles.get(i)).isEqualTo(titles.get(i));
    }
    assertThat(sheet.xml()).doesNotContain("<f>").contains("t=\"inlineStr\"");
  }

  @Test
  void markupIsEscapedAndCharactersXmlCannotCarryAreDropped() throws Exception {
    assertThat(ProblemXlsx.escape("a<b>&\"c\"")).isEqualTo("a&lt;b&gt;&amp;&quot;c&quot;");
    assertThat(ProblemXlsx.escape("x\u0000y\u0007z\u001Fw")).isEqualTo("xyzw");
    assertThat(ProblemXlsx.escape("two\nlines\ttab")).isEqualTo("two\nlines\ttab");
    assertThat(ProblemXlsx.escape("Σ 😀")).isEqualTo("Σ 😀");
    assertThat(ProblemXlsx.escape(null)).isEmpty();

    var sheet = Workbook.read(ProblemXlsx.render(List.of(item("A<B> & \u0000C", null))));
    assertThat(sheet.row(2).get(1)).isEqualTo("A<B> & C");
  }

  @Test
  void aDraftIsLabelledNhapAndAnEmptyListStillHasTheHeader() throws Exception {
    var draft =
        new AuthoringListPage.Item(
            UUID.randomUUID(),
            "D",
            "Draft",
            List.of(),
            UUID.randomUUID(),
            "EASY",
            "Dễ",
            "UNPUBLISHED",
            0,
            0,
            null,
            0,
            Instant.parse("2026-11-05T00:00:00Z"));

    assertThat(Workbook.read(ProblemXlsx.render(List.of(draft))).row(2).get(4)).isEqualTo("Nháp");
    var empty = Workbook.read(ProblemXlsx.render(List.of()));
    assertThat(empty.row(1)).hasSize(8);
    assertThat(empty.xml()).contains("<autoFilter ref=\"A1:H1\"/>");
  }

  /** The reader side of the tests: unzip the parts and read cells out of sheet1.xml. */
  private record Workbook(java.util.Map<String, String> files) {
    static Workbook read(byte[] bytes) throws Exception {
      var files = new java.util.LinkedHashMap<String, String>();
      try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
        for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
          files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
      }
      return new Workbook(files);
    }

    java.util.Set<String> parts() {
      return files.keySet();
    }

    String xml() {
      return files.get("xl/worksheets/sheet1.xml");
    }

    String name() throws Exception {
      return dom(files.get("xl/workbook.xml"))
          .getElementsByTagName("sheet")
          .item(0)
          .getAttributes()
          .getNamedItem("name")
          .getNodeValue();
    }

    private static org.w3c.dom.Document dom(String xml) throws Exception {
      var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory
          .newDocumentBuilder()
          .parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Every column of one row as text; an empty cell is "". */
    List<String> row(int number) throws Exception {
      var cells = dom(xml()).getElementsByTagName("c");
      var out = new java.util.ArrayList<String>();
      for (int i = 0; i < cells.getLength(); i++) {
        var cell = (org.w3c.dom.Element) cells.item(i);
        if (cell.getAttribute("r").endsWith(Integer.toString(number))
            && cell.getAttribute("r").matches("[A-Z]" + number)) {
          out.add(cell.getTextContent());
        }
      }
      return out;
    }

    /** True for a cell stored as a number (no string type). */
    List<Boolean> numbers(String... refs) throws Exception {
      var cells = dom(xml()).getElementsByTagName("c");
      var out = new java.util.ArrayList<Boolean>();
      for (String ref : refs) {
        for (int i = 0; i < cells.getLength(); i++) {
          var cell = (org.w3c.dom.Element) cells.item(i);
          if (cell.getAttribute("r").equals(ref)) {
            out.add(
                cell.getAttribute("t").isEmpty()
                    && cell.getElementsByTagName("v").getLength() == 1);
          }
        }
      }
      return out;
    }
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
  void selectedIdsAreExportedInsideTheCallersScopeWithTheLocalDateInTheName() throws Exception {
    when(clock.now()).thenReturn(Instant.parse("2026-11-05T18:00:00Z")); // already the 6th in UTC+7
    UUID id = UUID.randomUUID();
    when(repository.export(any(), any(), any(), any(), any()))
        .thenReturn(List.of(item("Two Sum", null)));

    var csv = handler.export(new ExportProblemsQuery(teacher, List.of(id, id), null));

    assertThat(csv.filename()).isEqualTo("problems-20261106.xlsx");
    assertThat(Workbook.read(csv.content()).xml()).contains("Two Sum");
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
