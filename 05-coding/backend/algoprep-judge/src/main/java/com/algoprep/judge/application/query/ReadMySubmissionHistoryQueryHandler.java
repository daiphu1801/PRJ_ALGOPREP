package com.algoprep.judge.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.History;
import com.algoprep.judge.application.dto.SubmissionReads.Page;
import com.algoprep.judge.application.dto.SubmissionReads.Summary;
import com.algoprep.judge.application.ports.in.ListMyProblemSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListMySubmissionHistoryUseCase;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.judge.domain.model.HistoryWindow;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class ReadMySubmissionHistoryQueryHandler
    implements ListMyProblemSubmissionsUseCase, ListMySubmissionHistoryUseCase {
  private final SubmissionReadRepository repository;
  private final SubmissionReadProjection projection;
  private final ProblemSummaryReadPort problems;

  ReadMySubmissionHistoryQueryHandler(
      SubmissionReadRepository repository,
      SubmissionReadProjection projection,
      ProblemSummaryReadPort problems) {
    this.repository = repository;
    this.projection = projection;
    this.problems = problems;
  }

  @Override
  public List<Summary> recent(UUID owner, UUID problem, int limit) {
    if (limit < 1 || limit > 10) {
      throw new InvalidRequestException("JDG-V14", "Invalid submission limit");
    }
    return repository.recent(owner, problem, limit).stream()
        .map(SubmissionReadProjection::summary)
        .toList();
  }

  @Override
  public Page<History> history(UUID owner, Filters f) {
    String keyword = SubmissionReadQueries.keyword(f.q());
    Set<UUID> ids = keyword.isEmpty() ? null : Set.copyOf(problems.search(keyword, 500));
    String verdict =
        Set.of("ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "COMPILE_ERROR", "RUNTIME_ERROR")
                .contains(f.verdict() == null ? "" : f.verdict())
            ? f.verdict()
            : null;
    String language =
        Set.of("JAVA", "CPP", "PYTHON").contains(f.language() == null ? "" : f.language())
            ? f.language()
            : null;
    long count = repository.count(owner, verdict, language, ids);
    int page = SubmissionReadQueries.page(f.page(), count);
    var rows =
        repository.history(owner, verdict, language, new HistoryWindow(ids, (page - 1) * 20, 20));
    var summaries =
        projection.summaries(rows.stream().map(s -> s.problemId()).collect(Collectors.toSet()));
    return new Page<>(
        rows.stream()
            .map(s -> SubmissionReadProjection.history(s, summaries.get(s.problemId())))
            .toList(),
        page,
        20,
        count);
  }
}
