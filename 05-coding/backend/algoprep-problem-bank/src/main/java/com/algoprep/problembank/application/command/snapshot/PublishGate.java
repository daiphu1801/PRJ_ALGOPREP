package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.PublishChecklist;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.policy.PublishChecklistPolicy;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.StoredGradingTestcase;
import com.algoprep.problembank.domain.ports.out.SnapshotPublicationRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BR-07 for a publish: the four counting conditions plus the reference solution run on every
 * approved testcase. The run only happens when there is something to run; an unavailable sandbox is
 * 503 PRB-050 and the caller changes nothing.
 */
@Component
class PublishGate {
  private static final Logger LOG = LoggerFactory.getLogger(PublishGate.class);
  private final PublishChecklistPolicy policy;
  private final SnapshotPublicationRepository approved;
  private final TestcaseObjectStorePort store;
  private final JudgeConfigReadPort config;
  private final SampleSolutionRunnerPort runner;

  PublishGate(
      PublishChecklistPolicy policy,
      SnapshotPublicationRepository approved,
      TestcaseObjectStorePort store,
      JudgeConfigReadPort config,
      SampleSolutionRunnerPort runner) {
    this.policy = policy;
    this.approved = approved;
    this.store = store;
    this.config = config;
    this.runner = runner;
  }

  /** Counting conditions only, as on a read; the sandbox condition stays undecided. */
  PublishChecklist counting(AuthoringProblemRecord problem) {
    return policy.evaluate(problem, null);
  }

  /** All five conditions; runs the sandbox when a reference solution and approved cases exist. */
  PublishChecklist evaluate(AuthoringProblemRecord problem) {
    boolean pass = PublishChecklistPolicy.runnable(problem) && runReference(problem);
    return policy.evaluate(problem, pass);
  }

  private boolean runReference(AuthoringProblemRecord problem) {
    var cases = new ArrayList<ReferenceSolutionRun.Case>();
    for (StoredGradingTestcase row : approved.findApproved(problem.id())) {
      cases.add(
          new ReferenceSolutionRun.Case(
              row.descriptor().testcaseId(), text(row.input()), text(row.expectedOutput())));
    }
    var limits = problem.limits();
    var request =
        new ReferenceSolutionRun.Request(
            problem.id(),
            problem.referenceSolution().language(),
            problem.functionWrapperSupported(),
            problem.referenceSolution().sourceCode(),
            problem.spec() == null ? 1 : problem.spec().specVersion(),
            new ReferenceSolutionRun.Limits(
                limits.timeLimitMs(),
                limits.memoryLimitMb(),
                limits.maxOutputSizeKb(),
                config.getGradingDefaults().compileTimeoutSeconds()),
            cases);
    try {
      return runner.run(request).allPassed();
    } catch (RuntimeException unavailable) {
      // The API contract stays one code; the log keeps the root cause apart from audit PRB-050.
      LOG.warn("Reference solution run failed for problem {}", problem.id(), unavailable);
      throw new ServiceUnavailableException("PRB-050", "Sample solution run unavailable", null);
    }
  }

  private String text(StoredGradingTestcase.Content value) {
    if ("INLINE".equals(value.storage())) {
      return value.inline();
    }
    return new String(store.get(value.objectKey()), StandardCharsets.UTF_8);
  }
}
