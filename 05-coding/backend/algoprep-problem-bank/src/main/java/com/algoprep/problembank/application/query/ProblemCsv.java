package com.algoprep.problembank.application.query;

import com.algoprep.problembank.domain.model.AuthoringListPage;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The eight table columns with a fixed Vietnamese header and a UTF-8 BOM so Excel reads the
 * accents. Titles are typed by authors, so a cell that a spreadsheet would run as a formula gets a
 * leading quote (DD F2 section 28.3, ponytail rung 6).
 */
final class ProblemCsv {
  static final String HEADER = "Mã,Tiêu đề,Chủ đề,Độ khó,Trạng thái,Lượt nộp,Tỉ lệ AC,Số testcase";
  private static final String BOM =
      String.valueOf((char) 0xFEFF); // UTF-8 byte order mark: Excel reads the accents
  private static final String EOL = "\r\n";

  private ProblemCsv() {}

  static String render(List<AuthoringListPage.Item> items) {
    var out = new StringBuilder(BOM).append(HEADER).append(EOL);
    for (var item : items) {
      out.append(
              String.join(
                  ",",
                  cell(item.code()),
                  cell(item.title()),
                  cell(item.topicNames().stream().collect(Collectors.joining("; "))),
                  cell(item.levelName()),
                  cell("PUBLISHED".equals(item.status()) ? "Đã xuất bản" : "Nháp"),
                  Integer.toString(item.submissionCount()),
                  item.acRate() == null ? "" : item.acRate().toPlainString(),
                  Integer.toString(item.testcaseCount())))
          .append(EOL);
    }
    return out.toString();
  }

  /** Quote when needed and defuse a leading formula character. */
  static String cell(String raw) {
    String value = raw == null ? "" : raw;
    if (!value.isEmpty() && "=+-@\t\r\n".indexOf(value.charAt(0)) >= 0) {
      value = "'" + value;
    }
    boolean quote =
        value.indexOf(',') >= 0
            || value.indexOf('"') >= 0
            || value.indexOf('\n') >= 0
            || value.indexOf('\r') >= 0;
    return quote ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
  }
}
