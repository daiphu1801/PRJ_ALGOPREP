package com.algoprep.harness.domain.compilation;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.domain.model.CompileDiagnostic;
import com.algoprep.harness.domain.model.CompileReportStatus;
import com.algoprep.harness.domain.model.LineMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Pure classification and report construction from DD F3 logic section 6. */
public final class CompileMapping {
  private CompileMapping() {}

  public static Result map(Language language, boolean standard, String output, LineMap map) {
    List<CompileDiagnostic> users = new ArrayList<>();
    boolean callSite = false;
    boolean parentUser = false;
    for (CompilerDiagnostic item : CompilerOutputParser.parse(language, output)) {
      Optional<String> message = CompileMessageSanitizer.clean(item.message(), map, standard);
      boolean ownFile = basename(item.file()).equals(map.sourceFileName());
      boolean user = ownFile && (standard || map.isUserLine(item.line()));
      if (item.isError()) {
        parentUser = user && message.isPresent();
        if (parentUser) {
          users.add(diagnostic(item, message.orElseThrow(), map, standard));
        } else if (ownFile && !user && message.isPresent() && map.isCallSite(item.line())) {
          callSite = true;
        }
      } else if (item.severity().equals("note")) {
        if (parentUser && user && message.isPresent()) {
          users.add(diagnostic(item, "note: " + message.orElseThrow(), map, standard));
        }
      } else {
        parentUser = false;
      }
    }
    if (!users.isEmpty()) {
      return new Result(CompileReportStatus.USER_ERRORS, reduce(users), null);
    }
    if (callSite) {
      return fixed(CompileReportStatus.USER_ERRORS, "harness.compile.signatureMismatch");
    }
    if (standard && language == Language.CPP && noMain(output)) {
      return fixed(CompileReportStatus.USER_ERRORS, "harness.compile.noMain");
    }
    return fixed(CompileReportStatus.HARNESS_INTERNAL, "harness.compile.internal");
  }

  private static CompileDiagnostic diagnostic(
      CompilerDiagnostic item, String message, LineMap map, boolean standard) {
    int line =
        standard ? Math.min(item.line(), map.userLineCount()) : item.line() - map.prefixLineCount();
    return new CompileDiagnostic(line, item.column(), message);
  }

  private static List<CompileDiagnostic> reduce(List<CompileDiagnostic> values) {
    Map<Key, CompileDiagnostic> unique = new LinkedHashMap<>();
    for (CompileDiagnostic value : values) {
      unique.putIfAbsent(new Key(value.line(), value.message()), value);
    }
    return unique.values().stream()
        .sorted(Comparator.comparingInt(CompileDiagnostic::line))
        .limit(10)
        .toList();
  }

  private static String basename(String value) {
    String path = value.replace('\\', '/');
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private static boolean noMain(String output) {
    return output.contains("undefined reference to 'main'")
        || output.contains("undefined reference to `main'")
        || output.contains("undefined reference to ‘main’");
  }

  private static Result fixed(CompileReportStatus status, String key) {
    return new Result(status, List.of(), key);
  }

  public record Result(
      CompileReportStatus status, List<CompileDiagnostic> diagnostics, String fixedMessageKey) {
    public Result {
      diagnostics = List.copyOf(diagnostics);
    }
  }

  private record Key(int line, String message) {}
}
