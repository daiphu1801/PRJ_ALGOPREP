package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.LearnerListLimit;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.AcceptedItem;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.AcceptedProblem;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.InProgressItem;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.ProgressProblem;
import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import com.algoprep.judge.application.ports.in.ListMyAcceptedSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListMyInProgressProblemsUseCase;
import com.algoprep.judge.application.ports.out.CurrentMetricsStudentPort;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.model.LearnerSubmissionCandidate;
import com.algoprep.judge.domain.ports.out.LearnerSubmissionListRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class GetLearnerSubmissionListsQueryHandler
    implements ListMyInProgressProblemsUseCase, ListMyAcceptedSubmissionsUseCase {
  private static final int BATCH_SIZE = 32;
  private final CurrentMetricsStudentPort principal;
  private final LearnerSubmissionListRepository repository;
  private final ProblemSummaryReadPort problems;

  GetLearnerSubmissionListsQueryHandler(
      CurrentMetricsStudentPort principal,
      LearnerSubmissionListRepository repository,
      ProblemSummaryReadPort problems) {
    this.principal = principal;
    this.repository = repository;
    this.problems = problems;
  }

  @Override
  public List<InProgressItem> inProgress(LearnerListLimit limit) {
    UUID student = principal.requireStudent();
    try {
      List<InProgressItem> result = new ArrayList<>();
      LearnerSubmissionCandidate.Cursor after = null;
      while (result.size() < limit.value()) {
        var rows = repository.inProgress(student, after, BATCH_SIZE);
        if (rows.isEmpty()) {
          break;
        }
        var metadata = metadata(rows);
        for (var row : rows) {
          var problem = metadata.items().get(row.problemId());
          if (metadata.unavailable() || (problem != null && !problem.withdrawn())) {
            result.add(progress(row, problem));
            if (result.size() == limit.value()) {
              break;
            }
          }
        }
        if (rows.size() < BATCH_SIZE) {
          break;
        }
        after = rows.getLast().cursor();
      }
      return List.copyOf(result);
    } catch (RuntimeException failure) {
      throw new JudgeReadUnavailableException();
    }
  }

  @Override
  public List<AcceptedItem> accepted(LearnerListLimit limit) {
    UUID student = principal.requireStudent();
    try {
      var rows = repository.accepted(student, limit.value());
      var metadata = metadata(rows);
      return rows.stream()
          .map(
              row -> {
                var p = metadata.items().get(row.problemId());
                var problem =
                    p == null
                        ? null
                        : new AcceptedProblem(p.code(), p.title(), p.slug(), p.levelDisplayName());
                return new AcceptedItem(
                    row.submissionId(),
                    row.problemId(),
                    problem,
                    row.language(),
                    row.runtimeMs(),
                    row.memoryKb(),
                    row.submittedAt());
              })
          .toList();
    } catch (RuntimeException failure) {
      throw new JudgeReadUnavailableException();
    }
  }

  private Metadata metadata(List<LearnerSubmissionCandidate> rows) {
    if (rows.isEmpty()) {
      return new Metadata(false, Map.of());
    }
    Set<UUID> ids =
        rows.stream().map(LearnerSubmissionCandidate::problemId).collect(Collectors.toSet());
    try {
      return new Metadata(
          false,
          problems.summaries(ids).stream()
              .collect(Collectors.toMap(Problem::problemId, Function.identity())));
    } catch (RuntimeException failure) {
      return new Metadata(true, Map.of());
    }
  }

  private static InProgressItem progress(LearnerSubmissionCandidate row, Problem p) {
    boolean counts =
        row.totalCount() != null
            && !row.status().equals("COMPILE_ERROR")
            && !row.status().equals("SYSTEM_ERROR");
    var problem =
        p == null
            ? null
            : new ProgressProblem(
                p.code(), p.title(), p.slug(), p.levelDisplayName(), p.withdrawn());
    return new InProgressItem(
        row.problemId(),
        problem,
        row.submissionId(),
        row.language(),
        row.status(),
        counts ? row.passedCount() : null,
        counts ? row.totalCount() : null,
        row.submittedAt());
  }

  private record Metadata(boolean unavailable, Map<UUID, Problem> items) {}
}
