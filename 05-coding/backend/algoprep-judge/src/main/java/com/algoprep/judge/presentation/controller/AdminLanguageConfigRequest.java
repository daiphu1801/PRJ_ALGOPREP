package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.domain.exception.AdminConfigException;
import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Defaults;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Generator;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Language;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Sandbox;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Raw JSON types are checked before conversion; numeric strings and fractional ints fail. */
final class AdminLanguageConfigRequest {
  private AdminLanguageConfigRequest() {}

  static AdminLanguageConfig parse(JsonNode body) {
    if (body == null || !body.isObject()) {
      throw invalid("JDG-V10");
    }
    var rows = body.path("languages");
    if (!rows.isArray() || rows.size() != 3) {
      throw invalid("JDG-V10");
    }
    var languages = new ArrayList<Language>();
    var seen = new HashSet<String>();
    for (var row : rows) {
      String language = row.path("language").asText("");
      if (!Set.of("JAVA", "CPP", "PYTHON").contains(language) || !seen.add(language)) {
        throw invalid("JDG-V10");
      }
      languages.add(
          new Language(
              language,
              "",
              decimal(row, "timeLimitMultiplier"),
              decimal(row, "memoryLimitMultiplier"),
              bool(row, "enabled"),
              0,
              version(row)));
    }
    var defaults = body.path("gradingDefaults");
    var sandbox = body.path("sandbox");
    var generator = body.path("generatorSandbox");
    return new AdminLanguageConfig(
        languages,
        new Defaults(
            integer(defaults, "timeLimitMs", Integer.MAX_VALUE, "JDG-V11"),
            integer(defaults, "memoryLimitMb", Integer.MAX_VALUE, "JDG-V11"),
            integer(defaults, "maxOutputSizeKb", Integer.MAX_VALUE, "JDG-V11"),
            integer(defaults, "compileTimeoutSeconds", Integer.MAX_VALUE, "JDG-V11"),
            version(defaults)),
        new Sandbox(
            bool(sandbox, "networkAccessEnabled"),
            bool(sandbox, "childProcessLimitEnabled"),
            bool(sandbox, "returnStderrToStudent"),
            version(sandbox)),
        new Generator(
            integer(generator, "maxRuntimeSeconds", 300, "JDG-V12"),
            integer(generator, "maxOutputMb", 1024, "JDG-V12"),
            version(generator)));
  }

  private static BigDecimal decimal(JsonNode row, String name) {
    var value = row.path(name);
    if (!value.isNumber()) {
      throw invalid("JDG-V10");
    }
    BigDecimal number = value.decimalValue();
    if (number.scale() > 2
        || number.compareTo(new BigDecimal("0.10")) < 0
        || number.compareTo(new BigDecimal("9.99")) > 0) {
      throw invalid("JDG-V10");
    }
    return number;
  }

  private static int integer(JsonNode row, String name, int max, String code) {
    var value = row.path(name);
    if (!value.isIntegralNumber()
        || !value.canConvertToInt()
        || value.intValue() < 1
        || value.intValue() > max) {
      throw invalid(code);
    }
    return value.intValue();
  }

  private static boolean bool(JsonNode row, String name) {
    if (!row.path(name).isBoolean()) {
      throw invalid("JDG-V10");
    }
    return row.path(name).booleanValue();
  }

  private static Instant version(JsonNode row) {
    try {
      return Instant.parse(row.path("updatedAt").asText(""));
    } catch (DateTimeParseException failure) {
      throw invalid("JDG-V10");
    }
  }

  private static AdminConfigException invalid(String code) {
    return new AdminConfigException(400, code);
  }
}
