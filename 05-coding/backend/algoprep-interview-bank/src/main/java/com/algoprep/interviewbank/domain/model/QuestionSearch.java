package com.algoprep.interviewbank.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Search text pipeline shared by the learner list (DD api F6 section 2) and the management list
 * (section 19): lower-case first, split on anything that is not a letter or digit, one prefix term
 * per token. Accent folding happens in SQL through {@code f_unaccent}, on both document and query.
 */
public final class QuestionSearch {
  private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");

  private QuestionSearch() {}

  /** Lower-case tokens as {@code token:*} joined by {@code &}; null when nothing is left. */
  public static String tsQuery(String q) {
    if (q == null) {
      return null;
    }
    String joined =
        NON_ALNUM
            .splitAsStream(q.strip().toLowerCase(Locale.ROOT))
            .filter(token -> !token.isEmpty())
            .map(token -> token + ":*")
            .collect(Collectors.joining(" & "));
    return joined.isEmpty() ? null : joined;
  }

  /**
   * Escapes the LIKE metacharacters backslash, percent and underscore (escape char is backslash).
   */
  public static String escapeLike(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
