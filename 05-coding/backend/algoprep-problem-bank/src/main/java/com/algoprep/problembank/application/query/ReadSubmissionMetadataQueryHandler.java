package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.ports.in.ReadSubmissionProblemSummariesUseCase;
import com.algoprep.problembank.application.ports.in.ReadSubmissionSampleIdsUseCase;
import com.algoprep.problembank.application.ports.in.SearchSubmissionProblemsUseCase;
import com.algoprep.problembank.domain.model.SubmissionProblemSummary;
import com.algoprep.problembank.domain.ports.out.SubmissionMetadataRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(
    readOnly = true,
    propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
class ReadSubmissionMetadataQueryHandler
    implements ReadSubmissionProblemSummariesUseCase,
        SearchSubmissionProblemsUseCase,
        ReadSubmissionSampleIdsUseCase {
  private final SubmissionMetadataRepository repository;

  ReadSubmissionMetadataQueryHandler(SubmissionMetadataRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SubmissionProblemSummary> summaries(Set<UUID> ids) {
    return ids.isEmpty() ? List.of() : repository.summaries(Set.copyOf(ids));
  }

  @Override
  public List<UUID> search(String keyword, int limit) {
    return repository.search(keyword, Math.clamp(limit, 1, 500));
  }

  @Override
  public Set<UUID> samples(UUID problemId, Set<UUID> testcaseIds) {
    return testcaseIds.isEmpty()
        ? Set.of()
        : repository.samples(problemId, Set.copyOf(testcaseIds));
  }
}
