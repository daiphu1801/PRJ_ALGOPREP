package com.algoprep.judge.application.command;

import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.Outcome;
import com.algoprep.judge.domain.model.WorkerRecords.Result;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

final class SubmissionFinalizationPolicy {
  private SubmissionFinalizationPolicy() {}

  static Outcome summarize(State state, FinishCommand command, List<Result> rows) {
    if (command.kind() == FinishKind.SYSTEM_ERROR) {
      if ("QUEUE_UNAVAILABLE".equals(command.failureReason())
          && !state.status().equals("PENDING")) {
        throw new IllegalStateException("Queue failure can only finalize pending submissions");
      }
      return new Outcome(
          "SYSTEM_ERROR",
          state.passed(),
          state.total(),
          null,
          state.runtime(),
          state.memory(),
          command.failureReason(),
          null,
          null);
    }
    if (command.kind() == FinishKind.COMPILE_ERROR) {
      if (!java.util.Set.of("COMPILING", "JUDGING").contains(state.status())) {
        throw new IllegalStateException("Compilation has not started");
      }
      return new Outcome(
          "COMPILE_ERROR",
          0,
          null,
          null,
          null,
          null,
          null,
          snippet(command.compileMessage(), 10000),
          null);
    }
    return completed(state, rows);
  }

  private static Outcome completed(State state, List<Result> rows) {
    validate(state, rows);
    int passed = (int) rows.stream().filter(r -> r.verdict().equals("AC")).count();
    var first = rows.stream().filter(r -> !r.verdict().equals("AC")).findFirst();
    String status = first.map(r -> terminal(r.verdict())).orElse("ACCEPTED");
    var score =
        BigDecimal.valueOf(passed).divide(BigDecimal.valueOf(rows.size()), 3, RoundingMode.HALF_UP);
    Integer runtime =
        rows.stream()
            .map(Result::runtime)
            .filter(java.util.Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(null);
    Integer memory =
        rows.stream()
            .map(Result::memory)
            .filter(java.util.Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(null);
    return new Outcome(
        status,
        passed,
        state.total(),
        score,
        runtime,
        memory,
        null,
        null,
        first.map(Result::order).orElse(null));
  }

  private static String terminal(String verdict) {
    return switch (verdict) {
      case "WA" -> "WRONG_ANSWER";
      case "TLE", "MLE" -> "TIME_LIMIT_EXCEEDED";
      case "RE" -> "RUNTIME_ERROR";
      default -> throw new IllegalStateException("Unknown testcase verdict");
    };
  }

  private static void validate(State state, List<Result> rows) {
    Integer total = state.total();
    if (!state.status().equals("JUDGING") || total == null || total < 1 || rows.size() != total) {
      throw new IllegalStateException("Incomplete grading");
    }
    for (int i = 0; i < rows.size(); i++) {
      if (rows.get(i).order() != i + 1) {
        throw new IllegalStateException("Invalid result order");
      }
    }
  }

  static String snippet(String value, int max) {
    return value == null ? null : value.substring(0, Math.min(value.length(), max));
  }
}
