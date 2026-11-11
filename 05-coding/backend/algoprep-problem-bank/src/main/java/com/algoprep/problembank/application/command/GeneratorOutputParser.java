package com.algoprep.problembank.application.command;

import com.algoprep.problembank.domain.exception.TestcaseGenerationRejectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Splits the stdout of a generator script into cases (BR-16 step 5). A case starts at a line {@code
 * ###CASE <label>}; the lines after it, up to the next header, are the input. Output with no header
 * at all is one unlabelled case. Anything else, such as text before the first header or an empty
 * case, is INVALID_FORMAT. [SoT: Suy luan] the DD names the step but not the format.
 */
final class GeneratorOutputParser {
  record GeneratedCase(String category, String input) {}

  private static final String MARK = "###CASE";
  private static final Set<String> CATEGORIES =
      Set.of(
          "TYPICAL",
          "BOUNDARY_LOW",
          "BOUNDARY_HIGH",
          "DEGENERATE",
          "DUPLICATE",
          "SORTED",
          "EXTREME");

  private GeneratorOutputParser() {}

  static List<GeneratedCase> parse(String stdout) {
    String text = stdout == null ? "" : stdout.replace("\r\n", "\n");
    if (text.isBlank()) {
      throw TestcaseGenerationRejectedException.script("INVALID_FORMAT");
    }
    List<GeneratedCase> cases = new ArrayList<>();
    String label = null;
    StringBuilder body = null;
    for (String line : text.split("\n", -1)) {
      if (isHeader(line)) {
        close(cases, label, body);
        label = labelOf(line);
        body = new StringBuilder();
      } else if (body == null) {
        if (!line.isBlank()) {
          return single(text);
        }
      } else {
        body.append(line).append('\n');
      }
    }
    close(cases, label, body);
    return cases;
  }

  /** No header anywhere means one case; a header after other text is a malformed output. */
  private static List<GeneratedCase> single(String text) {
    if (text.lines().anyMatch(GeneratorOutputParser::isHeader)) {
      throw TestcaseGenerationRejectedException.script("INVALID_FORMAT");
    }
    return List.of(new GeneratedCase(null, normalize(text)));
  }

  /** The marker alone, or the marker followed by a space or tab and a label. */
  private static boolean isHeader(String line) {
    return line.startsWith(MARK)
        && (line.length() == MARK.length()
            || line.charAt(MARK.length()) == ' '
            || line.charAt(MARK.length()) == '	');
  }

  private static String labelOf(String line) {
    String label = line.substring(MARK.length()).strip();
    return label.isEmpty() ? null : label;
  }

  private static void close(List<GeneratedCase> cases, String label, StringBuilder body) {
    if (body == null) {
      return;
    }
    String input = normalize(body.toString());
    if (input.isBlank()) {
      throw TestcaseGenerationRejectedException.script("INVALID_FORMAT");
    }
    String category = label == null ? null : label.strip().toUpperCase(java.util.Locale.ROOT);
    cases.add(
        new GeneratedCase(
            category != null && CATEGORIES.contains(category) ? category : null, input));
  }

  /** One trailing newline, nothing else changed. */
  static String normalize(String input) {
    return input.stripTrailing() + "\n";
  }
}
