package com.algoprep.harness.application.prepare;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.domain.model.LineMap;
import com.algoprep.harness.domain.model.RenderedProgram;
import com.algoprep.harness.domain.model.WrapperSource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Pure source assembly, DD F3 logic 4.2-4.5. Does not load specs or execute source. */
public final class ProgramSourceAssembler {
  private static final int MAX_SOURCE_BYTES = 65536;
  private static final Map<Language, String> STANDARD_FILES =
      Map.of(Language.JAVA, "Main.java", Language.CPP, "main.cpp", Language.PYTHON, "main.py");
  private static final Map<Language, String> WRAPPER_FILES =
      Map.of(Language.JAVA, "Solution.java", Language.CPP, "main.cpp", Language.PYTHON, "main.py");

  public RenderedProgram standardIo(Language language, String source) {
    String user = normalize(source);
    String file = fileName(language, STANDARD_FILES);
    return new RenderedProgram(file, user, new LineMap(0, lines(user), null, null, file));
  }

  public RenderedProgram functionWrapper(
      Language language, String source, WrapperSource framework) {
    String user = normalize(source);
    String file = fileName(language, WRAPPER_FILES);
    validateFramework(framework);
    int prefixLines = lines(framework.prefix());
    int userLines = lines(user);
    int callStart = prefixLines + userLines + lines(framework.beforeCall()) + 1;
    int callEnd = callStart + lines(framework.callSite()) - 1;
    String text =
        new StringBuilder()
            .append(framework.prefix())
            .append(user)
            .append(framework.beforeCall())
            .append(framework.callSite())
            .append(framework.afterCall())
            .toString();
    return new RenderedProgram(
        file, text, new LineMap(prefixLines, userLines, callStart, callEnd, file));
  }

  private static String normalize(String source) {
    if (source == null) {
      throw error("HRN-020", "Source code is blank");
    }
    String withoutBom = source.startsWith("\uFEFF") ? source.substring(1) : source;
    if (withoutBom.isBlank()) {
      throw error("HRN-020", "Source code is blank");
    }
    if (source.getBytes(StandardCharsets.UTF_8).length > MAX_SOURCE_BYTES) {
      throw error("HRN-021", "Source code exceeds the byte limit");
    }
    if (source.indexOf('\0') >= 0) {
      throw error("HRN-022", "Source code contains NUL");
    }
    String normalized = withoutBom.replace("\r\n", "\n").replace('\r', '\n');
    return normalized.endsWith("\n") ? normalized : normalized + "\n";
  }

  private static String fileName(Language language, Map<Language, String> files) {
    if (language == null) {
      throw error("HRN-010", "Missing source language");
    }
    return files.get(language);
  }

  private static void validateFramework(WrapperSource framework) {
    if (framework == null
        || !validFragment(framework.prefix())
        || !validFragment(framework.beforeCall())
        || !validFragment(framework.callSite())
        || !validFragment(framework.afterCall())
        || framework.callSite().isBlank()) {
      throw error("HRN-030", "Invalid rendered framework fragments");
    }
  }

  private static boolean validFragment(String value) {
    return value != null
        && (value.isEmpty() || value.endsWith("\n"))
        && value.indexOf('\r') < 0
        && value.indexOf('\0') < 0;
  }

  private static int lines(String value) {
    return Math.toIntExact(value.chars().filter(character -> character == '\n').count());
  }

  private static DomainRuleViolationException error(String code, String message) {
    return new DomainRuleViolationException(code, message);
  }
}
