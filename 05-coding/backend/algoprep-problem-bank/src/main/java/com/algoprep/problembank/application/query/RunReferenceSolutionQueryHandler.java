package com.algoprep.problembank.application.query;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.ReferenceRunReport;
import com.algoprep.problembank.application.dto.ReferenceRunReport.Row;
import com.algoprep.problembank.application.dto.ReferenceRunReport.Summary;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.in.RunReferenceSolutionUseCase;
import com.algoprep.problembank.application.ports.out.ReferenceRunQuotaPort;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import com.algoprep.problembank.domain.exception.ReferenceRunRateLimitedException;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * DD F2 section 48, BR-15. Deliberately not transactional: the sandbox can take up to two minutes
 * and nothing is written, so no database connection is held while it runs. The report is built from
 * verdicts only; the actual output of a testcase never leaves the judge.
 */
@Service
class RunReferenceSolutionQueryHandler implements RunReferenceSolutionUseCase {
  static final int MAX_SOURCE_BYTES = 65_536;
  private static final Set<String> LANGUAGES = Set.of("JAVA", "CPP", "PYTHON");

  private final ProblemAuthoringReadRepository repository;
  private final ReferenceRunRequests requests;
  private final SampleSolutionRunnerPort runner;
  private final ReferenceRunQuotaPort quota;
  private final ClockPort clock;

  RunReferenceSolutionQueryHandler(
      ProblemAuthoringReadRepository repository,
      ReferenceRunRequests requests,
      SampleSolutionRunnerPort runner,
      ReferenceRunQuotaPort quota,
      ClockPort clock) {
    this.repository = repository;
    this.requests = requests;
    this.runner = runner;
    this.quota = quota;
    this.clock = clock;
  }

  @Override
  public ReferenceRunReport.Report run(ReferenceRunReport.Command command) {
    var problem =
        repository
            .find(command.problemId(), true)
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    authorize(command, problem);
    requireOverrideShape(command);
    var cases = select(command, problem);
    var code = code(command, problem);
    if (cases.isEmpty()) {
      return new ReferenceRunReport.Report(null, new Summary(0, 0, false), List.of(), clock.now());
    }
    var result = execute(command, problem, code, cases);
    return report(cases, result);
  }

  private ReferenceSolutionRun.Result execute(
      ReferenceRunReport.Command command,
      AuthoringProblemRecord problem,
      ProblemContentData.Solution code,
      List<AuthoringTestcase> cases) {
    UUID actor = command.scope().actorId();
    UUID requestId = UUID.randomUUID();
    long retry = quota.reserve(actor, requestId);
    if (retry > 0) {
      throw new ReferenceRunRateLimitedException(retry);
    }
    try {
      var result = runner.run(requests.build(problem, code.language(), code.sourceCode(), cases));
      if (!result.compileError() && result.results().size() != cases.size()) {
        throw new IllegalStateException("Sandbox answered a different number of cases");
      }
      return result;
    } catch (ServiceUnavailableException storage) {
      quota.refund(actor, requestId);
      throw storage;
    } catch (RuntimeException sandbox) {
      quota.refund(actor, requestId);
      throw new ServiceUnavailableException("PRB-050", "Sample solution run unavailable", null);
    }
  }

  private ReferenceRunReport.Report report(
      List<AuthoringTestcase> cases, ReferenceSolutionRun.Result result) {
    var rows = new ArrayList<Row>();
    for (int i = 0; i < cases.size(); i++) {
      var t = cases.get(i);
      if (result.compileError()) {
        rows.add(
            new Row(t.id(), t.position(), t.visibility(), "FAIL", "COMPILE_ERROR", null, null));
        continue;
      }
      var one = result.results().get(i);
      rows.add(
          new Row(
              t.id(),
              t.position(),
              t.visibility(),
              one.passed() ? "PASS" : "FAIL",
              one.passed() ? null : failure(one.verdict()),
              narrow(one.runtimeMs()),
              narrow(one.memoryKb())));
    }
    int passed = (int) rows.stream().filter(r -> "PASS".equals(r.verdict())).count();
    return new ReferenceRunReport.Report(
        result.compileError() ? compileText(result.compileMessage()) : null,
        new Summary(passed, rows.size(), passed == rows.size()),
        rows,
        clock.now());
  }

  private static String compileText(String message) {
    return message == null || message.isBlank() ? "Compilation failed" : message;
  }

  private static Integer narrow(Long value) {
    return value == null ? null : (int) Math.min(value, Integer.MAX_VALUE);
  }

  /** The judge's verdict word as the wire enum of section 48.3. */
  static String failure(String verdict) {
    return switch (verdict) {
      case "WA" -> "WRONG_ANSWER";
      case "TLE" -> "TIME_LIMIT_EXCEEDED";
      case "MLE" -> "MEMORY_LIMIT_EXCEEDED";
      case "OLE" -> "OUTPUT_LIMIT_EXCEEDED";
      case "COMPILE_ERROR" -> "COMPILE_ERROR";
      default -> "RUNTIME_ERROR";
    };
  }

  /** Another author's draft is 404, their published problem 403 (same rule as testcase writes). */
  private static void authorize(
      ReferenceRunReport.Command command, AuthoringProblemRecord problem) {
    var scope = command.scope();
    if (scope.admin() || problem.authorId().equals(scope.actorId())) {
      return;
    }
    if (!"PUBLISHED".equals(problem.status())) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
    throw new PermissionDeniedException("PRB-003", "Only the author can run this problem");
  }

  private static void requireOverrideShape(ReferenceRunReport.Command command) {
    boolean hasLanguage = command.language() != null;
    boolean hasSource = command.sourceCode() != null;
    if (hasLanguage != hasSource
        || (hasLanguage && !LANGUAGES.contains(command.language()))
        || (hasSource
            && (command.sourceCode().isBlank()
                || command.sourceCode().getBytes(StandardCharsets.UTF_8).length
                    > MAX_SOURCE_BYTES))) {
      throw new InvalidRequestException("PRB-001", "Invalid sample solution override");
    }
  }

  /** One named testcase (draft or not), or every approved one in grading order. */
  private static List<AuthoringTestcase> select(
      ReferenceRunReport.Command command, AuthoringProblemRecord problem) {
    if (command.testcaseId() != null) {
      return List.of(
          problem.testcases().stream()
              .filter(t -> t.id().equals(command.testcaseId()))
              .findFirst()
              .orElseThrow(() -> new ResourceNotFoundException("PRB-011", "Testcase not found")));
    }
    return problem.testcases().stream()
        .filter(AuthoringTestcase::approved)
        .sorted(
            Comparator.comparingInt(AuthoringTestcase::position)
                .thenComparing(AuthoringTestcase::id))
        .toList();
  }

  private static ProblemContentData.Solution code(
      ReferenceRunReport.Command command, AuthoringProblemRecord problem) {
    if (command.language() != null) {
      return new ProblemContentData.Solution(command.language(), command.sourceCode());
    }
    var stored = problem.referenceSolution();
    if (stored == null) {
      throw new DomainRuleViolationException("PRB-040", "The problem has no sample solution yet");
    }
    return new ProblemContentData.Solution(stored.language(), stored.sourceCode());
  }
}
