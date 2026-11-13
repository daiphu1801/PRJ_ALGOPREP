package com.algoprep.judge.application.command;

import com.algoprep.judge.application.ports.in.BeginSubmissionCompilationUseCase;
import com.algoprep.judge.application.ports.in.BeginSubmissionJudgingUseCase;
import com.algoprep.judge.application.ports.in.FinalizeSubmissionUseCase;
import com.algoprep.judge.application.ports.in.IssueSubmissionRunTokenUseCase;
import com.algoprep.judge.application.ports.in.RecordSubmissionTestcaseUseCase;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class WorkerPersistence {
  private final BeginSubmissionCompilationUseCase compilation;
  private final BeginSubmissionJudgingUseCase judging;
  private final IssueSubmissionRunTokenUseCase tokens;
  private final RecordSubmissionTestcaseUseCase results;
  private final FinalizeSubmissionUseCase finalizer;

  WorkerPersistence(
      BeginSubmissionCompilationUseCase compilation,
      BeginSubmissionJudgingUseCase judging,
      IssueSubmissionRunTokenUseCase tokens,
      RecordSubmissionTestcaseUseCase results,
      FinalizeSubmissionUseCase finalizer) {
    this.compilation = compilation;
    this.judging = judging;
    this.tokens = tokens;
    this.results = results;
    this.finalizer = finalizer;
  }

  boolean compiling(UUID id, int spec, Lease lease) {
    lease.check();
    return compilation.begin(id, spec).isPresent();
  }

  boolean judging(UUID id, int count, Lease lease) {
    lease.check();
    return judging.beginJudging(id, count).isPresent();
  }

  Optional<Token> token(UUID id, UUID testcase, Lease lease) {
    lease.check();
    return tokens.issue(id, testcase);
  }

  boolean record(TestcaseWrite result, Lease lease) {
    lease.check();
    return results.record(result);
  }

  void finish(UUID id, FinishKind kind, String detail, Lease lease) {
    lease.check();
    finalizer.finish(
        new FinishCommand(
            id,
            kind,
            kind == FinishKind.SYSTEM_ERROR ? detail : null,
            kind == FinishKind.COMPILE_ERROR ? detail : null));
  }
}
