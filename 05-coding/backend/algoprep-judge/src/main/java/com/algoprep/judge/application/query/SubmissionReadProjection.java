package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.SubmissionReads.Detail;
import com.algoprep.judge.application.dto.SubmissionReads.History;
import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import com.algoprep.judge.application.dto.SubmissionReads.Progress;
import com.algoprep.judge.application.dto.SubmissionReads.ProgressCase;
import com.algoprep.judge.application.dto.SubmissionReads.Summary;
import com.algoprep.judge.application.dto.SubmissionReads.Testcase;
import com.algoprep.judge.application.dto.SubmissionReads.TestcaseDetail;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.judge.domain.model.SubmissionReadState;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class SubmissionReadProjection {
  private final SubmissionReadRepository repository;
  private final ProblemSummaryReadPort problems;

  SubmissionReadProjection(SubmissionReadRepository repository, ProblemSummaryReadPort problems) {
    this.repository = repository;
    this.problems = problems;
  }

  Detail detail(SubmissionReadState s, boolean beats) {
    var sample = beats && s.status().equals("ACCEPTED") ? repository.beats(s) : null;
    Integer percent =
        sample == null || sample.total() < 10
            ? null
            : BigDecimal.valueOf(sample.slower())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sample.total()), 0, RoundingMode.HALF_UP)
                .intValueExact();
    return new Detail(
        s.id(),
        s.status(),
        s.language(),
        s.model(),
        s.submittedAt(),
        s.finishedAt(),
        summaries(Set.of(s.problemId())).get(s.problemId()),
        s.source(),
        s.status().equals("COMPILE_ERROR") ? s.compileError() : null,
        s.scored() ? s.passed() : null,
        s.scored() ? s.total() : null,
        s.scored() ? s.score() : null,
        s.runtime(),
        s.memory(),
        percent,
        sample == null ? null : sample.total(),
        testcaseDetail(s));
  }

  private TestcaseDetail testcaseDetail(SubmissionReadState s) {
    if (s.expired()) {
      return new TestcaseDetail(false, "RETENTION_EXPIRED", List.of());
    }
    var cases = repository.cases(s.id());
    if (s.status().equals("COMPILE_ERROR")
        || (s.status().equals("SYSTEM_ERROR") && cases.isEmpty())) {
      return new TestcaseDetail(false, "NOT_APPLICABLE", List.of());
    }
    Set<UUID> samples;
    try {
      samples =
          problems.currentSamples(
              s.problemId(),
              cases.stream().map(SubmissionReadState.Case::id).collect(Collectors.toSet()));
    } catch (RuntimeException failure) {
      samples = Set.of();
    }
    boolean stderr = repository.stderrEnabled();
    Set<UUID> current = samples;
    return new TestcaseDetail(
        true, null, cases.stream().map(c -> testcase(c, s, current, stderr)).toList());
  }

  private static Testcase testcase(
      SubmissionReadState.Case c, SubmissionReadState s, Set<UUID> samples, boolean stderr) {
    boolean visible = c.sample() && samples.contains(c.id());
    boolean showStderr =
        visible && stderr && s.model().equals("STANDARD_IO") && !c.verdict().equals("AC");
    return new Testcase(
        c.order(),
        visible ? "SAMPLE" : "HIDDEN",
        visible ? c.id() : null,
        c.verdict(),
        c.runtime(),
        c.memory(),
        visible ? c.actual() : null,
        showStderr ? c.stderr() : null);
  }

  Progress progress(SubmissionReadState s) {
    var cases = s.expired() ? List.<SubmissionReadState.Case>of() : repository.cases(s.id());
    boolean terminal = s.terminal() || s.expired();
    int seq =
        s.terminal()
            ? 3 + (s.total() == null ? 0 : s.total())
            : switch (s.status()) {
              case "PENDING" -> 0;
              case "COMPILING" -> 1;
              default -> 2 + cases.size();
            };
    Integer failing =
        !s.scored()
            ? null
            : cases.stream()
                .filter(c -> !c.verdict().equals("AC"))
                .map(SubmissionReadState.Case::order)
                .findFirst()
                .orElse(null);
    return new Progress(
        s.id(),
        s.status(),
        seq,
        terminal,
        s.total(),
        cases.size(),
        (int) cases.stream().filter(c -> c.verdict().equals("AC")).count(),
        cases.stream().map(c -> new ProgressCase(c.order(), c.verdict())).toList(),
        s.scored() ? s.score() : null,
        failing);
  }

  Map<UUID, Problem> summaries(Set<UUID> ids) {
    try {
      return problems.summaries(ids).stream()
          .collect(Collectors.toMap(Problem::problemId, Function.identity()));
    } catch (RuntimeException failure) {
      return Map.of();
    }
  }

  static Summary summary(SubmissionReadState s) {
    boolean counts =
        s.total() != null
            && !s.status().equals("COMPILE_ERROR")
            && !s.status().equals("SYSTEM_ERROR");
    return new Summary(
        s.id(),
        s.language(),
        s.status(),
        counts ? s.passed() : null,
        counts ? s.total() : null,
        s.scored() ? s.score() : null,
        s.terminal() && !s.status().equals("COMPILE_ERROR") ? s.runtime() : null,
        s.terminal() && !s.status().equals("COMPILE_ERROR") ? s.memory() : null,
        s.submittedAt());
  }

  static History history(SubmissionReadState s, Problem p) {
    return new History(
        s.id(),
        s.submittedAt(),
        p,
        s.language(),
        s.status(),
        s.scored() ? s.passed() : null,
        s.scored() ? s.total() : null,
        s.terminal() ? s.runtime() : null);
  }
}
