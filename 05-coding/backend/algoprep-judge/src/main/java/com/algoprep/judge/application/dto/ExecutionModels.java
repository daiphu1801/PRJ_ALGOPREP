package com.algoprep.judge.application.dto;

import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeNode;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Component contracts. No persistence, HTTP submission or problem-bank simulation. */
public final class ExecutionModels {
  private ExecutionModels() {}

  public enum Model {
    FUNCTION_WRAPPER,
    STANDARD_IO
  }

  public enum Status {
    OK,
    TLE,
    MLE,
    RE,
    INPUT_ERROR,
    SIGNATURE_ERROR
  }

  public enum Verdict {
    AC,
    WA,
    TLE,
    MLE,
    RE,
    COMPILE_ERROR,
    SYSTEM_ERROR
  }

  public record SourceMap(
      int prefixLineCount,
      int userLineCount,
      Integer callSiteStartLine,
      Integer callSiteEndLine,
      String sourceFileName) {}

  public record Program(
      Language language,
      Model model,
      Map<String, Object> body,
      String artifact,
      SourceMap sourceMap,
      String signatureText,
      String resultSentinel) {
    public Program {
      body = Collections.unmodifiableMap(new LinkedHashMap<>(body));
    }
  }

  public record Limits(long timeMs, long memoryBytes, long outputBytes, int processes) {
    public Limits {
      if (timeMs <= 0
          || timeMs > 300000
          || memoryBytes <= 0
          || outputBytes <= 0
          || outputBytes > Integer.MAX_VALUE - 65536
          || processes < 0
          || processes > 60) {
        throw new IllegalArgumentException("Invalid sandbox limits");
      }
    }
  }

  public record CompileRequest(Program program, Limits limits) {}

  public record Compilation(boolean success, String compilerOutput) {}

  public record RunRequest(String stdin, Limits limits) {}

  public record RunResult(
      Status status, String stdout, String stderr, long runtimeMs, long memoryKb) {}

  public record Matching(MatchingStrategy strategy, BigDecimal epsilon, TypeNode returnType) {}

  public record Testcase(String stdin, String expectedOutput, boolean sample, Limits limits) {}

  public record EvaluationRequest(
      Program program,
      Limits compileLimits,
      Matching matching,
      List<Testcase> testcases,
      boolean returnStderr) {
    public EvaluationRequest {
      testcases = List.copyOf(testcases);
    }
  }

  public record Match(boolean matches, String actualOutput, String userConsole) {}

  public record CompileReport(boolean internal, String message) {}

  public record TestcaseResult(
      int order,
      Verdict verdict,
      long runtimeMs,
      long memoryKb,
      String actualOutputSnippet,
      String userConsole,
      String stderrSnippet) {}

  public record EvaluationResult(
      Verdict verdict, String failureReason, String compileMessage, List<TestcaseResult> results) {
    public EvaluationResult {
      results = List.copyOf(results);
    }
  }
}
