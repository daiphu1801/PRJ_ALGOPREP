package com.algoprep.judge.application.query;

import com.algoprep.judge.application.ports.in.GetAcceptedSubmissionSnapshotUseCase;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.exception.SubmissionNotAcceptedException;
import com.algoprep.judge.domain.exception.SubmissionNotFoundException;
import com.algoprep.judge.domain.model.AcceptedSubmissionSnapshot;
import com.algoprep.judge.domain.ports.out.SubmissionSnapshotRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetAcceptedSubmissionSnapshotQueryHandler implements GetAcceptedSubmissionSnapshotUseCase {
  private final SubmissionSnapshotRepository repository;

  GetAcceptedSubmissionSnapshotQueryHandler(SubmissionSnapshotRepository repository) {
    this.repository = repository;
  }

  @Override
  public AcceptedSubmissionSnapshot getAccepted(UUID submissionId) {
    Objects.requireNonNull(submissionId);
    try {
      var row = repository.find(submissionId).orElseThrow(SubmissionNotFoundException::new);
      if (!"ACCEPTED".equals(row.status())) {
        throw new SubmissionNotAcceptedException();
      }
      return Objects.requireNonNull(row.snapshot());
    } catch (SubmissionNotFoundException | SubmissionNotAcceptedException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new JudgeReadUnavailableException();
    }
  }
}
