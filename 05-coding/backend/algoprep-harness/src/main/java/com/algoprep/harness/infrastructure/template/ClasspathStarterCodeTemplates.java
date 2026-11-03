package com.algoprep.harness.infrastructure.template;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.starter.StarterCodeRenderer;
import com.algoprep.harness.domain.starter.StarterTemplates;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;

/** Loads trusted starter assets once; the resulting renderer performs no I/O. */
public final class ClasspathStarterCodeTemplates {
  private ClasspathStarterCodeTemplates() {}

  public static StarterCodeRenderer create() {
    EnumMap<Language, StarterTemplates> templates = new EnumMap<>(Language.class);
    for (Language language : Language.values()) {
      String base = "/harness-templates/" + language.name().toLowerCase(Locale.ROOT) + "/starter-";
      templates.put(
          language,
          new StarterTemplates(read(base + "wrapper.txt"), read(base + "standard-io.txt")));
    }
    return new StarterCodeRenderer(templates);
  }

  private static String read(String path) {
    try (var stream = ClasspathStarterCodeTemplates.class.getResourceAsStream(path)) {
      if (stream == null) {
        throw new DomainRuleViolationException("HRN-030", "Missing starter template");
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
          .replace("\r\n", "\n")
          .replace('\r', '\n');
    } catch (IOException failure) {
      throw new DomainRuleViolationException("HRN-030", "Starter template loading failed");
    }
  }
}
