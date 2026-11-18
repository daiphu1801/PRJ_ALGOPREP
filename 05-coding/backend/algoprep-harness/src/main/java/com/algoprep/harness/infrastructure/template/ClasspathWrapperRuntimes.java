package com.algoprep.harness.infrastructure.template;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.Language;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Loads fixed jar resources; no external paths or user-controlled templates. */
public final class ClasspathWrapperRuntimes {
  private ClasspathWrapperRuntimes() {}

  public static Map<Language, String> load() {
    return Map.of(
        Language.JAVA,
        read("java/runtime.txt"),
        Language.CPP,
        read("cpp/runtime.txt"),
        Language.PYTHON,
        read("python/runtime.py"));
  }

  private static String read(String path) {
    try (var input =
        ClasspathWrapperRuntimes.class.getResourceAsStream("/harness-templates/" + path)) {
      if (input != null) {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (IOException failure) {
      throw invalid();
    }
    throw invalid();
  }

  private static DomainRuleViolationException invalid() {
    return new DomainRuleViolationException("HRN-030", "Invalid wrapper runtime resource");
  }
}
