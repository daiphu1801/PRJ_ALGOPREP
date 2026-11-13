package com.algoprep.problembank.application.policy;

import com.algoprep.problembank.application.dto.ProblemAuthoringView.PublishChecklist;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * BR-07, the five publish conditions (DD logic F2 BR-07, DD validation F2 table 11.1). One policy
 * for the authoring read, which cannot run the sandbox, and for the status change, which does.
 */
@Component
public final class PublishChecklistPolicy {
  private static final String PUBLISH = "algoprep.problem-bank.publish.";
  public static final String REFERENCE_PASSES = "REFERENCE_SOLUTION_PASSES_ALL";

  private final Environment environment;

  public PublishChecklistPolicy(Environment environment) {
    this.environment = environment;
  }

  /**
   * @param sandboxPass result of running the reference solution, or null when it was not run (the
   *     read path): that item is then neither met nor missing
   */
  public PublishChecklist evaluate(AuthoringProblemRecord p, Boolean sandboxPass) {
    int approved = (int) p.testcases().stream().filter(AuthoringTestcase::approved).count();
    int samples =
        (int)
            p.testcases().stream()
                .filter(t -> t.approved() && "SAMPLE".equals(t.visibility()))
                .count();
    int examples = p.workedExamples().size();
    boolean spec = specDeclared(p);
    int minTestcases = minApprovedTestcases();
    int minSamples = minApprovedSamples();
    int minExamples = threshold("min-worked-examples", 2);
    var items =
        List.of(
            new PublishChecklist.Item(
                "MIN_APPROVED_TESTCASES", approved >= minTestcases, minTestcases, approved),
            new PublishChecklist.Item(
                "MIN_APPROVED_SAMPLES", samples >= minSamples, minSamples, samples),
            new PublishChecklist.Item(
                "MIN_WORKED_EXAMPLES", examples >= minExamples, minExamples, examples),
            new PublishChecklist.Item(REFERENCE_PASSES, sandboxPass, null, null),
            new PublishChecklist.Item("SPEC_DECLARED", spec, null, null));
    return new PublishChecklist(items, approved, samples, examples, spec, sandboxPass);
  }

  /** True when the sandbox has anything to run: a reference solution and an approved testcase. */
  public static boolean runnable(AuthoringProblemRecord p) {
    return p.referenceSolution() != null && p.testcases().stream().anyMatch(t -> t.approved());
  }

  /**
   * Both format texts are filled and, unless the signature uses a type outside the schema, the
   * signature is declared. The strategy rules are table checks.
   */
  private static boolean specDeclared(AuthoringProblemRecord p) {
    StoredProblemSpec stored = p.spec();
    if (stored == null) {
      return false;
    }
    var data = stored.data();
    boolean formats = !blank(data.stdinFormatMd()) && !blank(data.stdoutFormatMd());
    boolean signature =
        data.functionName() != null
            && data.returnTypeJson() != null
            && data.parametersJson() != null;
    return formats && (signature || !p.functionWrapperSupported());
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public int minApprovedTestcases() {
    return threshold("min-approved-testcases", 8);
  }

  public int minApprovedSamples() {
    return threshold("min-approved-samples", 2);
  }

  private int threshold(String key, int fallback) {
    return environment.getProperty(PUBLISH + key, Integer.class, fallback);
  }
}
