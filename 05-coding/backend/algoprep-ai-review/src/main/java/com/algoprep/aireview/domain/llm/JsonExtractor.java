package com.algoprep.aireview.domain.llm;

import java.util.Optional;

/**
 * Pulls the first balanced JSON object out of free-model output that may wrap it in code fences or
 * surround it with prose. It only locates text; it does not validate against a schema and never
 * invents content.
 */
public final class JsonExtractor {
  private JsonExtractor() {}

  /**
   * The first balanced {@code {...}} block (string literals respected), or empty if none/truncated.
   */
  public static Optional<String> firstJsonObject(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    int start = raw.indexOf('{');
    while (start >= 0) {
      int end = matchingBrace(raw, start);
      if (end >= 0) {
        return Optional.of(raw.substring(start, end + 1));
      }
      if (looksLikeObjectStart(raw, start)) {
        // A real object that never closes is truncated output; its inner objects are not the
        // answer.
        return Optional.empty();
      }
      // A stray "{" in prose: try the next opening brace.
      start = raw.indexOf('{', start + 1);
    }
    return Optional.empty();
  }

  private static boolean looksLikeObjectStart(String s, int brace) {
    int i = brace + 1;
    while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
      i++;
    }
    return i < s.length() && (s.charAt(i) == '"' || s.charAt(i) == '}');
  }

  private static int matchingBrace(String s, int start) {
    int depth = 0;
    boolean inString = false;
    for (int i = start; i < s.length(); i++) {
      char c = s.charAt(i);
      if (inString) {
        if (c == '\\') {
          i++;
        } else if (c == '"') {
          inString = false;
        }
      } else if (c == '"') {
        inString = true;
      } else if (c == '{') {
        depth++;
      } else if (c == '}' && --depth == 0) {
        return i;
      }
    }
    return -1;
  }
}
