package com.algoprep.harness.domain.compilation;

import com.algoprep.harness.domain.model.LineMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Redacts host details after classification, without echoing source or caret context. */
final class CompileMessageSanitizer {
  private static final Pattern PATH = Pattern.compile("(?:[A-Za-z]:[\\\\/]|/)[^\\s\"'<>]++");
  private static final Pattern LINE = Pattern.compile("(?i)\\bline\\s++(\\d++)\\b");

  private CompileMessageSanitizer() {}

  static Optional<String> clean(String message, LineMap map, boolean standard) {
    if (message.contains("apH")
        || message.contains("ApH")
        || message.contains("@@APH")
        || isVersion(message)) {
      return Optional.empty();
    }
    String value = PATH.matcher(message).replaceAll("<path>");
    value = files(value);
    Matcher matcher = LINE.matcher(value);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(
          result, Matcher.quoteReplacement(reference(matcher.group(1), map, standard)));
    }
    matcher.appendTail(result);
    String cleaned = result.toString().replaceAll("[\\p{Cntrl}&&[^\\t]]", "").strip();
    return cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned);
  }

  private static String reference(String value, LineMap map, boolean standard) {
    try {
      long line = Long.parseLong(value);
      if (line < 1 || (!standard && (line > Integer.MAX_VALUE || !map.isUserLine((int) line)))) {
        return "<location>";
      }
      long mapped = standard ? Math.min(line, map.userLineCount()) : line - map.prefixLineCount();
      return "line " + mapped;
    } catch (NumberFormatException invalid) {
      return "<location>";
    }
  }

  private static String files(String value) {
    StringBuilder result = new StringBuilder();
    int start = 0;
    for (int end = 0; end <= value.length(); end++) {
      if (end == value.length()
          || Character.isWhitespace(value.charAt(end))
          || "\"',;()[]{}".indexOf(value.charAt(end)) >= 0) {
        String token = value.substring(start, end);
        int colon = token.indexOf(':');
        String name = colon < 0 ? token : token.substring(0, colon);
        result.append(isFile(name) ? "<file>" : token);
        if (end < value.length()) {
          result.append(value.charAt(end));
        }
        start = end + 1;
      }
    }
    return result.toString();
  }

  private static boolean isFile(String value) {
    return value.endsWith(".java")
        || value.endsWith(".cpp")
        || value.endsWith(".cc")
        || value.endsWith(".cxx")
        || value.endsWith(".hpp")
        || value.endsWith(".h")
        || value.endsWith(".py");
  }

  private static boolean isVersion(String value) {
    String lower = value.toLowerCase(Locale.ROOT);
    boolean explicit =
        lower.contains("version")
            && (lower.contains("compiler")
                || lower.contains("javac")
                || lower.contains("g++")
                || lower.contains("gcc")
                || lower.contains("python")
                || lower.contains("openjdk"));
    if (explicit) {
      return true;
    }
    for (String compiler : List.of("javac", "gcc", "g++", "python", "openjdk")) {
      int start = lower.indexOf(compiler + " ");
      if (start >= 0) {
        String version = lower.substring(start + compiler.length()).stripLeading();
        if (!version.isEmpty() && version.charAt(0) >= '0' && version.charAt(0) <= '9') {
          return true;
        }
      }
    }
    return false;
  }
}
