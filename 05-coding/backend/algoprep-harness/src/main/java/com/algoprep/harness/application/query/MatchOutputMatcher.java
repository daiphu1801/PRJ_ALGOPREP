package com.algoprep.harness.application.query;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.harness.application.dto.MatchOutputCommand;
import com.algoprep.harness.application.dto.MatchResult;
import com.algoprep.harness.application.dto.MatchSpec;
import com.algoprep.harness.application.dto.MatchVerdict;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.MatchOutputUseCase;
import com.algoprep.harness.domain.matching.OutputMatching;
import java.util.List;
import org.springframework.stereotype.Service;

/** Stateless P4 bean; no transaction, logging or I/O. */
@Service
public final class MatchOutputMatcher implements MatchOutputUseCase {
  @Override
  public MatchResult match(MatchOutputCommand command) {
    validate(command);
    MatchSpec spec = command.matchSpec();
    OutputParts parts = split(command);
    boolean matches = matches(parts, command.expectedOutput(), spec);
    return new MatchResult(
        matches ? MatchVerdict.MATCH : MatchVerdict.NO_MATCH, parts.actual(), parts.console());
  }

  private static boolean matches(OutputParts parts, String expected, MatchSpec spec) {
    try {
      return OutputMatching.matches(
          parts.found() ? parts.actual() : null,
          normalize(expected),
          spec.matchingStrategy(),
          spec.returnType(),
          spec.epsilonValue());
    } catch (IllegalArgumentException | ArithmeticException internal) {
      throw new DomainRuleViolationException("HRN-031", "Output matching failed");
    }
  }

  private static void validate(MatchOutputCommand command) {
    if (command == null
        || command.model() == null
        || command.matchSpec() == null
        || command.actualStdout() == null
        || command.expectedOutput() == null) {
      throw invalidConfig();
    }
    MatchSpec spec = command.matchSpec();
    if (spec.matchingStrategy() == null
        || (spec.matchingStrategy() == MatchingStrategy.EPSILON
            && (spec.epsilonValue() == null || spec.epsilonValue().signum() <= 0))) {
      throw invalidConfig();
    }
    if (command.model() == SubmissionModel.FUNCTION_WRAPPER
        && (spec.returnType() == null
            || command.resultSentinel() == null
            || command.resultSentinel().isEmpty()
            || command.resultSentinel().contains("\n")
            || command.resultSentinel().contains("\r"))) {
      throw invalidConfig();
    }
    if (spec.returnType() != null
        && !FunctionSpecEvaluator.evaluate(
                new FunctionSpecDraft(
                    "solve",
                    spec.returnType(),
                    List.of(),
                    null,
                    spec.matchingStrategy(),
                    spec.epsilonValue()))
            .wrapperSupported()) {
      throw invalidConfig();
    }
  }

  private static DomainRuleViolationException invalidConfig() {
    return new DomainRuleViolationException("HRN-051", "Invalid output matching configuration");
  }

  private static OutputParts split(MatchOutputCommand command) {
    String stdout = newlines(command.actualStdout());
    if (command.model() == SubmissionModel.STANDARD_IO) {
      return new OutputParts(true, normalize(stdout), "");
    }
    int start = 0;
    OutputParts result = new OutputParts(false, "", stdout);
    while (start <= stdout.length()) {
      int end = stdout.indexOf('\n', start);
      if (end < 0) {
        end = stdout.length();
      }
      if (stdout.substring(start, end).equals(command.resultSentinel())) {
        result =
            new OutputParts(
                true,
                normalize(stdout.substring(Math.min(end + 1, stdout.length()))),
                stdout.substring(0, start));
      }
      if (end == stdout.length()) {
        break;
      }
      start = end + 1;
    }
    return result;
  }

  private static String normalize(String value) {
    return newlines(value).replaceFirst("\n+$", "");
  }

  private static String newlines(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }

  private record OutputParts(boolean found, String actual, String console) {}
}
