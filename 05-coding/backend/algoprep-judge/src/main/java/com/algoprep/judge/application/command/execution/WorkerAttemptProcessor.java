package com.algoprep.judge.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.GradingTestcaseSnapshot;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.judge.domain.model.WorkerJob;
import com.algoprep.judge.domain.model.WorkerSettings;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;
import java.util.Comparator;
import java.util.HashSet;
import org.springframework.stereotype.Component;

@Component
class WorkerAttemptProcessor {
  private final WorkerProgramLoader loader;
  private final ProblemGradingSpecPort problems;
  private final WorkerPersistence persistence;
  private final WorkerExecution execution;

  WorkerAttemptProcessor(
      WorkerProgramLoader loader,
      ProblemGradingSpecPort problems,
      WorkerPersistence persistence,
      WorkerExecution execution) {
    this.loader = loader;
    this.problems = problems;
    this.persistence = persistence;
    this.execution = execution;
  }

  void process(WorkerJob job, WorkerSettings settings, Lease lease) {
    // ponytail: the default engine cannot change network namespace per request; enabled networking
    // is deferred.
    if (settings.networkEnabled()) {
      throw new JudgeInfrastructureException();
    }
    var spec = loader.spec(job);
    var snapshot = snapshot(job);
    for (int attempt = 0; attempt < 2; attempt++) {
      if (!persistence.compiling(job.id(), spec.versions().specVersion(), lease)) {
        return;
      }
      try {
        execute(job, settings, spec, snapshot, lease);
        return;
      } catch (DomainRuleViolationException e) {
        if (!e.errorCode().equals("HRN-002") || attempt == 1) {
          throw e;
        }
        spec = loader.spec(job);
      }
    }
  }

  private void execute(
      WorkerJob job,
      WorkerSettings settings,
      GradingSpecification spec,
      GradingTestcaseSnapshot snapshot,
      Lease lease) {
    var prepared = loader.prepare(job, spec);
    execution.run(
        new WorkerExecutionRequest(
            job, settings, prepared, loader.runLimits(spec, settings), snapshot, lease));
  }

  private GradingTestcaseSnapshot snapshot(WorkerJob job) {
    var snapshot =
        WorkerRetry.read(
            () -> problems.loadTestcaseSnapshot(job.problemId(), job.testcaseVersion()));
    if (!snapshot.problemId().equals(job.problemId())
        || snapshot.version() != job.testcaseVersion()
        || snapshot.testcases().isEmpty()) {
      throw invalid();
    }
    var ids = new HashSet<java.util.UUID>();
    var positions = new HashSet<Integer>();
    for (var row : snapshot.testcases()) {
      if (row.position() < 1 || !ids.add(row.testcaseId()) || !positions.add(row.position())) {
        throw invalid();
      }
    }
    var sorted =
        snapshot.testcases().stream()
            .sorted(
                Comparator.comparingInt(
                    com.algoprep.judge.application.dto.GradingTestcase::position))
            .toList();
    return new GradingTestcaseSnapshot(snapshot.problemId(), snapshot.version(), sorted);
  }

  private ProblemGradingReadException invalid() {
    return new ProblemGradingReadException(ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
  }
}
