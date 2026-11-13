package com.algoprep.judge.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.dto.TestcaseProgress;
import com.algoprep.judge.application.ports.in.IssueSubmissionRunTokenUseCase;
import com.algoprep.judge.application.ports.in.RecordSubmissionTestcaseUseCase;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import com.algoprep.judge.domain.ports.out.SubmissionWorkerRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class WorkerTestcaseCommandHandler
    implements IssueSubmissionRunTokenUseCase, RecordSubmissionTestcaseUseCase {
  private final SubmissionWorkerRepository repository;
  private final SubmissionProgressPort progress;
  private final WorkerTransactions transactions;
  private final IdGeneratorPort ids;

  WorkerTestcaseCommandHandler(
      SubmissionWorkerRepository repository,
      SubmissionProgressPort progress,
      WorkerTransactions transactions,
      IdGeneratorPort ids) {
    this.repository = repository;
    this.progress = progress;
    this.transactions = transactions;
    this.ids = ids;
  }

  @Override
  public Optional<Token> issue(UUID id, UUID testcaseId) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(testcaseId);
    return transactions.execute(
        () -> {
          if (repository.heartbeat(id).isEmpty() || repository.resultExists(id, testcaseId)) {
            return Optional.empty();
          }
          var token = new Token(ids.newId(), UUID.randomUUID());
          return repository.issue(id, testcaseId, token) ? Optional.of(token) : Optional.empty();
        });
  }

  @Override
  public boolean record(TestcaseWrite write) {
    Objects.requireNonNull(write);
    return transactions.execute(() -> persist(write));
  }

  private boolean persist(TestcaseWrite write) {
    var active = repository.heartbeat(write.submissionId());
    if (active.isEmpty() || repository.resultExists(write.submissionId(), write.testcaseId())) {
      return false;
    }
    var state = active.orElseThrow();
    var rows = repository.results(state.id());
    if (state.total() == null
        || write.order() != rows.size() + 1
        || write.order() > state.total()) {
      throw new IllegalStateException("Invalid testcase order");
    }
    repository.save(write, state, ids.newId());
    int passed =
        (int) rows.stream().filter(r -> r.verdict().equals("AC")).count()
            + (write.verdict().equals("AC") ? 1 : 0);
    progress.afterCommit(
        state.owner().userId(),
        new TestcaseProgress(
            state.id(),
            state.updatedAt(),
            write.order(),
            write.verdict(),
            write.order(),
            passed,
            state.total()));
    return true;
  }
}
