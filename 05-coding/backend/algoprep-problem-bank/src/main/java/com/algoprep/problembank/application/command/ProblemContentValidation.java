package com.algoprep.problembank.application.command;

import com.algoprep.problembank.domain.exception.ProblemContentInvalidException;
import com.algoprep.problembank.domain.model.ProblemContentData;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Technical ceilings use environment-backed application properties; Markdown/source stay verbatim.
 */
@Component
class ProblemContentValidation {
  private final Environment environment;

  ProblemContentValidation(Environment environment) {
    this.environment = environment;
  }

  ProblemContentData normalize(ProblemContentData data) {
    if (data == null) {
      throw invalid("content", "required");
    }
    text(data.title(), "title", 255, true);
    text(data.statementMd(), "statementMd", cap("statement-max-chars", 100000), true);
    text(data.constraintsMd(), "constraintsMd", cap("constraints-max-chars", 20000), false);
    limits(data.limits());
    if (data.topicIds().stream().anyMatch(java.util.Objects::isNull)) {
      throw invalid("topicIds", "exists");
    }
    var tags = new LinkedHashMap<String, String>();
    for (String name : data.tags()) {
      text(name, "tags", 50, true);
      tags.putIfAbsent(name.strip().toLowerCase(Locale.ROOT), name.strip());
    }
    if (tags.size() > cap("max-tags", 20)) {
      throw invalid("tags", "maxItems");
    }
    examples(data);
    var solution = solution(data.referenceSolution());
    return new ProblemContentData(
        data.title().strip(),
        data.statementMd(),
        data.constraintsMd() == null ? "" : data.constraintsMd(),
        data.levelId(),
        data.limits(),
        new ArrayList<>(new LinkedHashSet<>(data.topicIds())),
        new ArrayList<>(tags.values()),
        data.workedExamples(),
        solution);
  }

  private void limits(ProblemContentData.Limits limits) {
    if (limits == null) {
      throw invalid("limits", "required");
    }
    positive(limits.timeLimitMs(), "limits.timeLimitMs");
    positive(limits.memoryLimitMb(), "limits.memoryLimitMb");
    positive(limits.maxOutputSizeKb(), "limits.maxOutputSizeKb");
    if (limits.maxSubmissionsPerHour() != null) {
      positive(limits.maxSubmissionsPerHour(), "limits.maxSubmissionsPerHour");
    }
  }

  private void examples(ProblemContentData data) {
    Set<java.util.UUID> seen = new java.util.HashSet<>();
    for (var example : data.workedExamples()) {
      if (example == null) {
        throw invalid("workedExamples", "required");
      }
      bytes(example.input(), "workedExamples.input", cap("example-max-bytes", 8192), true);
      bytes(example.output(), "workedExamples.output", cap("example-max-bytes", 8192), true);
      text(
          example.explanationMd(),
          "workedExamples.explanationMd",
          cap("explanation-max-chars", 4000),
          false);
      if (example.id() != null && !seen.add(example.id())) {
        throw invalid("workedExamples.id", "unique");
      }
    }
  }

  private ProblemContentData.Solution solution(ProblemContentData.Solution solution) {
    if (solution == null || solution.sourceCode() == null || solution.sourceCode().isBlank()) {
      return null;
    }
    if (!Set.of("JAVA", "CPP", "PYTHON")
        .contains(solution.language() == null ? "" : solution.language())) {
      throw invalid("referenceSolution.language", "enum");
    }
    bytes(
        solution.sourceCode(),
        "referenceSolution.sourceCode",
        cap("source-max-bytes", 65536),
        true);
    return solution;
  }

  private void text(String value, String field, int limit, boolean required) {
    if (value == null || value.isBlank()) {
      if (required) {
        throw invalid(field, "required");
      }
    }
    String measured =
        "title".equals(field) || "tags".equals(field) ? value == null ? "" : value.strip() : value;
    if (measured != null && measured.codePointCount(0, measured.length()) > limit) {
      throw invalid(field, "maxLength");
    }
    if (value != null && value.indexOf(0) >= 0) {
      throw invalid(field, "text");
    }
  }

  private void bytes(String value, String field, int limit, boolean required) {
    text(value, field, Integer.MAX_VALUE, required);
    if (value != null && value.getBytes(StandardCharsets.UTF_8).length > limit) {
      throw invalid(field, "maxBytes");
    }
  }

  private void positive(int value, String field) {
    if (value <= 0) {
      throw invalid(field, "positiveInt");
    }
  }

  private int cap(String key, int fallback) {
    return environment.getProperty(
        "algoprep.problem-bank.authoring." + key, Integer.class, fallback);
  }

  static ProblemContentInvalidException invalid(String field, String rule) {
    return new ProblemContentInvalidException(field, rule);
  }
}
