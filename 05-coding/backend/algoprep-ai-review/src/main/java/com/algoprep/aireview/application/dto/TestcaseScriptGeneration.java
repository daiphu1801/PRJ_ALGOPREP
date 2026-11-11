package com.algoprep.aireview.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * P3 contract (DD F5 section 32.2). The input type has no field for a reference solution, an author
 * brief or a hidden testcase, so none of them can reach the prompt.
 */
public final class TestcaseScriptGeneration {
  private TestcaseScriptGeneration() {}

  public record Sample(String input, String expectedOutput) {
    public Sample {
      Objects.requireNonNull(input, "input");
      Objects.requireNonNull(expectedOutput, "expectedOutput");
    }
  }

  /**
   * {@code callId} is the caller-made ledger idempotency key; {@code userId} pressed the button.
   */
  public record Input(
      UUID callId,
      UUID userId,
      UUID problemId,
      String statementMarkdown,
      String constraintsMarkdown,
      List<Sample> samples) {
    public Input {
      Objects.requireNonNull(callId, "callId");
      Objects.requireNonNull(userId, "userId");
      Objects.requireNonNull(problemId, "problemId");
      requireText(statementMarkdown, "statementMarkdown");
      requireText(constraintsMarkdown, "constraintsMarkdown");
      samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
      if (samples.size() != 2) {
        throw new IllegalArgumentException("exactly two samples are required");
      }
    }

    private static void requireText(String value, String name) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " must not be blank");
      }
    }

    @Override
    public String toString() {
      return "TestcaseScriptGeneration.Input[call=" + callId + ", problem=" + problemId + "]";
    }
  }

  public record Output(
      String generatorScript,
      String validatorScript,
      List<String> categoryLabels,
      String modelId,
      String promptVersion,
      int tokensUsed) {
    public Output {
      categoryLabels = List.copyOf(categoryLabels);
    }

    @Override
    public String toString() {
      return "TestcaseScriptGeneration.Output[redacted]";
    }
  }
}
