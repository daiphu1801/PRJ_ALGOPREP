package com.algoprep.harness.infrastructure.template;

import com.algoprep.common.exception.DomainRuleViolationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Loads the trusted Python guard once for a future P2 plugin; never executes it in the JVM. */
public final class ClasspathPythonSignatureGuard {
  private ClasspathPythonSignatureGuard() {}

  public static String load() {
    try (var input =
        ClasspathPythonSignatureGuard.class.getResourceAsStream(
            "/harness-templates/python/signature-guard.py")) {
      if (input == null) {
        throw invalidGuard();
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw invalidGuard();
    }
  }

  private static DomainRuleViolationException invalidGuard() {
    return new DomainRuleViolationException("HRN-030", "Python signature guard is unavailable");
  }
}
