package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.SubmissionReads.Detail;
import com.algoprep.judge.application.dto.SubmissionReads.Progress;
import com.algoprep.judge.application.ports.in.GetOwnedSubmissionDetailUseCase;
import com.algoprep.judge.application.ports.in.GetSubmissionProgressUseCase;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class ReadOwnedSubmissionQueryHandler
    implements GetOwnedSubmissionDetailUseCase, GetSubmissionProgressUseCase {
  private final SubmissionReadRepository repository;
  private final SubmissionReadProjection projection;

  ReadOwnedSubmissionQueryHandler(
      SubmissionReadRepository repository, SubmissionReadProjection projection) {
    this.repository = repository;
    this.projection = projection;
  }

  // Calls F2 over a REQUIRES_NEW read; it must not run inside a transaction that holds a
  // connection,
  // otherwise every request pins one connection while waiting for a second and the pool deadlocks.
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Override
  public Detail get(UUID owner, UUID id) {
    return projection.detail(
        repository.owned(id, owner).orElseThrow(() -> SubmissionReadQueries.missing("JDG-110")),
        true);
  }

  @Override
  public Progress getProgress(UUID owner, UUID id) {
    return projection.progress(
        repository.owned(id, owner).orElseThrow(() -> SubmissionReadQueries.missing("JDG-110")));
  }
}
