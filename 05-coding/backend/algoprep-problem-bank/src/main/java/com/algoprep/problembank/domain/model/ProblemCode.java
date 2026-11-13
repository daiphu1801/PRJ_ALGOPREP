package com.algoprep.problembank.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/** BR-03 problem code, including deleted rows in uniqueness; unchanged after content edits. */
public final class ProblemCode {
  private ProblemCode() {}

  public static String from(String title, int attempt) {
    String base =
        Normalizer.normalize(title, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "-")
            .replaceAll("^-|-$", "");
    if (base.isEmpty()) {
      base = "PROBLEM";
    }
    String suffix = attempt == 1 ? "" : "-" + attempt;
    return base.substring(0, Math.min(base.length(), 64 - suffix.length())).replaceAll("-$", "")
        + suffix;
  }
}
