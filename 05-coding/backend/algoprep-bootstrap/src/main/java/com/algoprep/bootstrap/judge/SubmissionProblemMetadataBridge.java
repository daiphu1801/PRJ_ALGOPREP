package com.algoprep.bootstrap.judge;

import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.problembank.application.ports.in.ReadSubmissionProblemSummariesUseCase;
import com.algoprep.problembank.application.ports.in.ReadSubmissionSampleIdsUseCase;
import com.algoprep.problembank.application.ports.in.SearchSubmissionProblemsUseCase;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SubmissionProblemMetadataBridge implements ProblemSummaryReadPort {
  private final ReadSubmissionProblemSummariesUseCase summaries;
  private final SearchSubmissionProblemsUseCase search;
  private final ReadSubmissionSampleIdsUseCase samples;

  SubmissionProblemMetadataBridge(
      ReadSubmissionProblemSummariesUseCase summaries,
      SearchSubmissionProblemsUseCase search,
      ReadSubmissionSampleIdsUseCase samples) {
    this.summaries = summaries;
    this.search = search;
    this.samples = samples;
  }

  @Override
  public List<Problem> summaries(Set<UUID> ids) {
    try {
      return summaries.summaries(ids).stream()
          .map(
              p ->
                  new Problem(
                      p.problemId(),
                      p.code(),
                      p.title(),
                      p.slug(),
                      p.levelDisplayName(),
                      p.withdrawn()))
          .toList();
    } catch (RuntimeException failure) {
      throw unavailable();
    }
  }

  @Override
  public List<UUID> search(String keyword, int limit) {
    try {
      return search.search(keyword, limit);
    } catch (RuntimeException failure) {
      throw unavailable();
    }
  }

  @Override
  public Set<UUID> currentSamples(UUID problemId, Set<UUID> ids) {
    try {
      return samples.samples(problemId, ids);
    } catch (RuntimeException failure) {
      throw unavailable();
    }
  }

  private static IllegalStateException unavailable() {
    return new IllegalStateException("Submission problem metadata unavailable");
  }
}
