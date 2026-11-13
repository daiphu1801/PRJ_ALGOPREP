package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.command.GeneratedTestcaseWriter.Kept;
import com.algoprep.problembank.application.command.GeneratedTestcaseWriter.RunInfo;
import com.algoprep.problembank.application.command.GeneratorOutputParser.GeneratedCase;
import com.algoprep.problembank.application.dto.ReferenceRunReport;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import com.algoprep.problembank.application.dto.TestcaseGeneration.Dropped;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ReferenceStatus;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ScriptStatus;
import com.algoprep.problembank.application.dto.TestcaseGeneration.Warning;
import com.algoprep.problembank.application.ports.in.GenerateTestcasesUseCase;
import com.algoprep.problembank.application.ports.in.RunReferenceSolutionUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseGenerationRejectedException;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * DD F2 section 49, BR-16. Deliberately not transactional: the AI call and the sandbox runs take
 * minutes and hold no connection; only {@link GeneratedTestcaseWriter} opens a transaction, at the
 * very end. The AI sees the statement, the data constraints and two hand-written samples, never the
 * sample solution. A generation that adds nothing, or fails anywhere, does not use up a turn.
 */
@Service
class GenerateTestcasesCommandHandler implements GenerateTestcasesUseCase {
  private final ProblemAuthoringReadRepository repository;
  private final RunReferenceSolutionUseCase reference;
  private final TestcaseObjectStorePort store;
  private final GenerationPorts ports;
  private final GeneratedTestcaseWriter writer;

  GenerateTestcasesCommandHandler(
      ProblemAuthoringReadRepository repository,
      RunReferenceSolutionUseCase reference,
      TestcaseObjectStorePort store,
      GenerationPorts ports,
      GeneratedTestcaseWriter writer) {
    this.repository = repository;
    this.reference = reference;
    this.store = store;
    this.ports = ports;
    this.writer = writer;
  }

  @Override
  public TestcaseGeneration.Result generate(TestcaseGeneration.Command command) {
    var problem =
        repository
            .find(command.problemId(), true)
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    authorize(command, problem);
    if (problem.referenceSolution() == null) {
      throw new DomainRuleViolationException("PRB-040", "The problem has no sample solution yet");
    }
    var samples = manualSamples(problem);
    var reasons = new ArrayList<String>();
    if (problem.constraintsMd() == null || problem.constraintsMd().isBlank()) {
      reasons.add("NO_CONSTRAINTS");
    }
    if (samples.size() < 2) {
      reasons.add("NOT_ENOUGH_MANUAL_SAMPLES");
    }
    if (!reasons.isEmpty()) {
      throw TestcaseGenerationRejectedException.prerequisites(reasons);
    }
    int limit = repository.aiGenerationLimit();
    int used = problem.aiGenerationsUsed();
    if (used >= limit) {
      throw TestcaseGenerationRejectedException.quota(used, limit);
    }
    String token = ports.lock().acquire(problem.id());
    if (token == null) {
      throw new ResourceConflictException("PRB-023", "Another generation is running");
    }
    try {
      return run(command, problem, samples.subList(0, 2), new Quota(used, limit));
    } finally {
      ports.lock().release(problem.id(), token);
    }
  }

  private record Quota(int used, int limit) {}

  private TestcaseGeneration.Result run(
      TestcaseGeneration.Command command,
      AuthoringProblemRecord problem,
      List<AuthoringTestcase> samples,
      Quota quota) {
    var report =
        reference.run(
            new ReferenceRunReport.Command(command.scope(), problem.id(), null, null, null));
    if (!report.summary().allPass()) {
      throw TestcaseGenerationRejectedException.prerequisites(List.of("REFERENCE_NOT_PASSING"));
    }
    var scripts = askAi(command, problem, samples);
    long seed = seed(problem.id(), quota.used() + 1);
    var generated = sandbox(() -> ports.sandbox().runGenerator(scripts.generatorScript(), seed));
    if (generated.status() != ScriptStatus.OK) {
      throw TestcaseGenerationRejectedException.script(generated.status().name());
    }
    var drops = new LinkedHashMap<String, Integer>();
    var fresh = withoutDuplicates(problem, GeneratorOutputParser.parse(generated.stdout()), drops);
    var valid = validated(scripts.validatorScript(), fresh, drops);
    var warnings = new ArrayList<Warning>();
    var kept = outputs(problem, valid, drops, warnings);
    var dropped =
        drops.entrySet().stream().map(e -> new Dropped(e.getKey(), e.getValue())).toList();
    if (kept.isEmpty()) {
      return new TestcaseGeneration.Result(
          0,
          List.of(),
          dropped,
          warnings,
          new TestcaseGeneration.Quota(quota.used(), quota.limit()),
          problem.updatedAt());
    }
    var saved =
        writer.persist(
            command.scope(),
            problem.id(),
            kept,
            new RunInfo(
                command.scope().actorId(), scripts, seed, dropped, quota.used(), quota.limit()));
    return new TestcaseGeneration.Result(
        kept.size(),
        saved.testcases(),
        dropped,
        warnings,
        new TestcaseGeneration.Quota(quota.used() + 1, quota.limit()),
        saved.touch().updatedAt());
  }

  private TestcaseGeneration.Scripts askAi(
      TestcaseGeneration.Command command,
      AuthoringProblemRecord problem,
      List<AuthoringTestcase> samples) {
    try {
      return ports
          .ai()
          .generate(
              new TestcaseGeneration.ScriptRequest(
                  UUID.randomUUID(),
                  command.scope().actorId(),
                  problem.id(),
                  problem.statementMd(),
                  problem.constraintsMd(),
                  samples.stream()
                      .map(
                          s ->
                              new TestcaseGeneration.Sample(
                                  text(s.inputStorage(), s.inputInline(), s.inputObjectKey()),
                                  text(
                                      s.expectedStorage(),
                                      s.expectedInline(),
                                      s.expectedObjectKey())))
                      .toList()));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException("PRB-051", "AI path unavailable", failure);
    }
  }

  /** Step 6: an input equal to an existing one, or to an earlier generated one, is dropped. */
  private List<GeneratedCase> withoutDuplicates(
      AuthoringProblemRecord problem, List<GeneratedCase> cases, Map<String, Integer> drops) {
    Set<String> seen = new HashSet<>();
    for (var existing : problem.testcases()) {
      seen.add(
          GeneratorOutputParser.normalize(
              text(existing.inputStorage(), existing.inputInline(), existing.inputObjectKey())));
    }
    var fresh = new ArrayList<GeneratedCase>();
    for (var one : cases) {
      if (seen.add(one.input())) {
        fresh.add(one);
      } else {
        drops.merge("DUPLICATE_INPUT", 1, Integer::sum);
      }
    }
    return fresh;
  }

  /** Step 7: the validator script, one run per input. */
  private List<GeneratedCase> validated(
      String validator, List<GeneratedCase> cases, Map<String, Integer> drops) {
    if (cases.isEmpty()) {
      return cases;
    }
    var flags =
        sandbox(
            () ->
                ports
                    .sandbox()
                    .validate(validator, cases.stream().map(GeneratedCase::input).toList()));
    if (flags.size() != cases.size()) {
      throw new ServiceUnavailableException("PRB-050", "Sandbox answered a wrong count", null);
    }
    var valid = new ArrayList<GeneratedCase>();
    for (int i = 0; i < cases.size(); i++) {
      if (Boolean.TRUE.equals(flags.get(i))) {
        valid.add(cases.get(i));
      } else {
        drops.merge("INVALID_CONSTRAINT", 1, Integer::sum);
      }
    }
    return valid;
  }

  /** Step 8: the real output comes from the sample solution, never from the AI. */
  private List<Kept> outputs(
      AuthoringProblemRecord problem,
      List<GeneratedCase> cases,
      Map<String, Integer> drops,
      List<Warning> warnings) {
    if (cases.isEmpty()) {
      return List.of();
    }
    var limits = problem.limits();
    var request = referenceRequest(problem, cases);
    var results = sandbox(() -> ports.sandbox().runReference(request));
    if (results.size() != cases.size()) {
      throw new ServiceUnavailableException("PRB-050", "Sandbox answered a wrong count", null);
    }
    var kept = new ArrayList<Kept>();
    GeneratedCase largest =
        cases.stream().max(Comparator.comparingInt(c -> c.input().length())).get();
    boolean largestLost = false;
    for (int i = 0; i < cases.size(); i++) {
      var one = results.get(i);
      var input = cases.get(i);
      String output = one.status() == ReferenceStatus.OK ? one.output() : null;
      if (output == null || output.isBlank() || tooBig(output, limits.maxOutputSizeKb())) {
        drops.merge(
            one.status() == ReferenceStatus.TIMEOUT ? "TIMEOUT" : "REFERENCE_FAILED",
            1,
            Integer::sum);
        largestLost |= input == largest;
      } else {
        kept.add(
            new Kept(input.category(), input.input(), GeneratorOutputParser.normalize(output)));
      }
    }
    if (largestLost) {
      warnings.add(
          new Warning(
              "LARGEST_CASE_TIMEOUT",
              "The largest generated case failed or timed out on the sample solution; check the"
                  + " time limit or the sample solution speed."));
    }
    return kept;
  }

  private ReferenceSolutionRun.Request referenceRequest(
      AuthoringProblemRecord problem, List<GeneratedCase> cases) {
    var solution = problem.referenceSolution();
    var limits = problem.limits();
    return new ReferenceSolutionRun.Request(
        problem.id(),
        solution.language(),
        problem.functionWrapperSupported(),
        solution.sourceCode(),
        problem.spec() == null ? 1 : problem.spec().specVersion(),
        new ReferenceSolutionRun.Limits(
            limits.timeLimitMs(),
            limits.memoryLimitMb(),
            limits.maxOutputSizeKb(),
            ports.config().getGradingDefaults().compileTimeoutSeconds()),
        cases.stream()
            .map(c -> new ReferenceSolutionRun.Case(UUID.randomUUID(), c.input(), ""))
            .toList());
  }

  private static boolean tooBig(String output, int maxKb) {
    return output.getBytes(StandardCharsets.UTF_8).length > maxKb * 1024L
        || TestcaseContentStore.tooLarge(output);
  }

  /** A sandbox that cannot answer is 503 PRB-050 and nothing was written. */
  private static <T> T sandbox(Supplier<T> call) {
    try {
      return call.get();
    } catch (AlgoPrepException known) {
      throw known;
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException("PRB-050", "Sandbox unavailable", failure);
    }
  }

  /** Fixed per problem and attempt number, so the same attempt always draws the same data. */
  static long seed(UUID problemId, int attempt) {
    return (problemId.getMostSignificantBits() ^ problemId.getLeastSignificantBits()) * 31L
        + attempt;
  }

  /** The two samples the AI may see: hand-written, approved, SAMPLE, in position order. */
  private static List<AuthoringTestcase> manualSamples(AuthoringProblemRecord problem) {
    return problem.testcases().stream()
        .filter(t -> t.approved() && "SAMPLE".equals(t.visibility()) && "MANUAL".equals(t.origin()))
        .sorted(Comparator.comparingInt(AuthoringTestcase::position))
        .toList();
  }

  /** Another author's draft is 404, their published problem 403 (same rule as testcase writes). */
  private static void authorize(
      TestcaseGeneration.Command command, AuthoringProblemRecord problem) {
    var scope = command.scope();
    if (scope.admin() || problem.authorId().equals(scope.actorId())) {
      return;
    }
    if (!"PUBLISHED".equals(problem.status())) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
    throw new PermissionDeniedException("PRB-003", "Only the author can generate testcases");
  }

  private String text(String storage, String inline, String objectKey) {
    if ("INLINE".equals(storage)) {
      return inline;
    }
    return new String(store.get(objectKey), StandardCharsets.UTF_8);
  }
}
