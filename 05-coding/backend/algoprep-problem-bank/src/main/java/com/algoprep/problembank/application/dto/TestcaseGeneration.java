package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contracts of the AI testcase generation (DD F2 section 49, BR-16): the HTTP result, plus the
 * types of the two outbound ports. Script text, testcase inputs and outputs never reach toString.
 */
public final class TestcaseGeneration {
  private TestcaseGeneration() {}

  public record Command(AuthoringScope scope, UUID problemId) {}

  public record Dropped(String reason, int count) {}

  public record Warning(String code, String message) {}

  public record Quota(int used, int limit) {}

  /** Wire names of section 49.3. */
  public record Result(
      int added,
      List<TestcaseView> testcases,
      List<Dropped> dropped,
      List<Warning> warnings,
      Quota aiGeneration,
      Instant problemUpdatedAt) {}

  /** What the AI may see: statement, data constraints and two hand-written samples. */
  public record Sample(String input, String expectedOutput) {
    @Override
    public String toString() {
      return "TestcaseGeneration.Sample[redacted]";
    }
  }

  public record ScriptRequest(
      UUID callId,
      UUID userId,
      UUID problemId,
      String statementMarkdown,
      String constraintsMarkdown,
      List<Sample> samples) {
    public ScriptRequest {
      samples = List.copyOf(samples);
    }

    @Override
    public String toString() {
      return "TestcaseGeneration.ScriptRequest[" + problemId + "]";
    }
  }

  public record Scripts(
      String generatorScript,
      String validatorScript,
      List<String> categoryLabels,
      String modelId,
      String promptVersion) {
    public Scripts {
      categoryLabels = List.copyOf(categoryLabels);
    }

    @Override
    public String toString() {
      return "TestcaseGeneration.Scripts[redacted]";
    }
  }

  public enum ScriptStatus {
    OK,
    TIMEOUT,
    OUTPUT_TOO_LARGE,
    SCRIPT_ERROR
  }

  public record GeneratorRun(ScriptStatus status, String stdout) {
    @Override
    public String toString() {
      return "TestcaseGeneration.GeneratorRun[" + status + "]";
    }
  }

  public enum ReferenceStatus {
    OK,
    TIMEOUT,
    FAILED
  }

  /** One reference-solution run on one generated input; {@code output} only when OK. */
  public record ReferenceOutput(ReferenceStatus status, String output) {
    @Override
    public String toString() {
      return "TestcaseGeneration.ReferenceOutput[" + status + "]";
    }
  }

  /** Sections 55 to 57: the cap on successful generations per problem. */
  public record Settings(int maxPerProblem) {}

  public record UpdateSettings(UUID actorId, int maxPerProblem) {}
}
