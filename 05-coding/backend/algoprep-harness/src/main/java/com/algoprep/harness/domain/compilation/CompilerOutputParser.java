package com.algoprep.harness.domain.compilation;

import com.algoprep.common.functionspec.Language;
import java.util.ArrayList;
import java.util.List;

/** Header parsing without backtracking over untrusted compiler output. */
final class CompilerOutputParser {
  private CompilerOutputParser() {}

  static List<CompilerDiagnostic> parse(Language language, String output) {
    List<CompilerDiagnostic> result = new ArrayList<>();
    PythonLocation python = null;
    for (String line : output.split("\n", -1)) {
      if (language == Language.PYTHON) {
        PythonLocation location = pythonLocation(line);
        if (location != null) {
          python = location;
        } else if (python != null && isPythonError(line)) {
          result.add(new CompilerDiagnostic(python.file(), python.line(), null, "error", line));
          python = null;
        }
      } else {
        CompilerDiagnostic diagnostic = nativeHeader(language, line);
        if (diagnostic != null) {
          result.add(diagnostic);
        }
      }
    }
    return List.copyOf(result);
  }

  private static CompilerDiagnostic nativeHeader(Language language, String text) {
    for (String severity : List.of("fatal error", "error", "warning", "note")) {
      String marker = ": " + severity + ": ";
      int end = text.indexOf(marker);
      if (end < 0) {
        continue;
      }
      String location = text.substring(0, end);
      int last = location.lastIndexOf(':');
      Integer number = positive(location.substring(last + 1));
      if (last < 0 || number == null) {
        continue;
      }
      if (language == Language.JAVA && (severity.equals("error") || severity.equals("warning"))) {
        String file = location.substring(0, last);
        if (file.endsWith(".java") && file.chars().noneMatch(Character::isWhitespace)) {
          return new CompilerDiagnostic(
              file, number, null, severity, text.substring(end + marker.length()));
        }
      } else if (language == Language.CPP) {
        int previous = location.lastIndexOf(':', last - 1);
        Integer sourceLine = previous < 0 ? null : positive(location.substring(previous + 1, last));
        if (sourceLine != null && previous > 0) {
          return new CompilerDiagnostic(
              location.substring(0, previous),
              sourceLine,
              number,
              severity,
              text.substring(end + marker.length()));
        }
      }
    }
    return null;
  }

  private static PythonLocation pythonLocation(String text) {
    String line = text.stripLeading();
    if (!line.startsWith("File \"")) {
      return null;
    }
    int quote = line.indexOf('"', 6);
    if (quote < 0 || !line.startsWith(", line ", quote + 1)) {
      return null;
    }
    String number = line.substring(quote + 8);
    int comma = number.indexOf(',');
    Integer sourceLine = positive(comma < 0 ? number : number.substring(0, comma));
    return sourceLine == null ? null : new PythonLocation(line.substring(6, quote), sourceLine);
  }

  private static boolean isPythonError(String line) {
    int colon = line.indexOf(": ");
    if (colon < 0) {
      return false;
    }
    String name = line.substring(0, colon);
    return name.endsWith("Error") && name.chars().allMatch(c -> Character.isLetter(c) || c == '_');
  }

  private static Integer positive(String value) {
    if (value.isEmpty()
        || value.length() > 10
        || !value.chars().allMatch(c -> c >= '0' && c <= '9')) {
      return null;
    }
    try {
      int number = Integer.parseInt(value);
      return number > 0 ? number : null;
    } catch (NumberFormatException invalid) {
      return null;
    }
  }

  private record PythonLocation(String file, int line) {}
}
