package com.algoprep.problembank.domain.model.problem;

import com.algoprep.common.text.EdgeTrim;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/** BR-03 problem code, including deleted rows in uniqueness; unchanged after content edits. */
public final class ProblemCode {
  private ProblemCode() {}

  /**
   * Bounded collision fallback: retain the full identity, including for long or symbolic titles.
   */
  public static String withIdentitySuffix(String title, UUID id) {
    String base = from(title, 1);
    String suffix = "-" + id.toString().toUpperCase(Locale.ROOT);
    return EdgeTrim.edges(base.substring(0, Math.min(base.length(), 64 - suffix.length())), "-")
        + suffix;
  }

  public static String from(String title, int attempt) {
    String base =
        Normalizer.normalize(title, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "-");
    base = EdgeTrim.edges(base, "-");
    if (base.isEmpty()) {
      base = "PROBLEM";
    }
    String suffix = attempt == 1 ? "" : "-" + attempt;
    return base.substring(0, Math.min(base.length(), 64 - suffix.length())).replaceAll("-$", "")
        + suffix;
  }
}
