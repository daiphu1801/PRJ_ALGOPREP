package com.algoprep.aireview.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.ports.out.ProblemBankStatsPort;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.ProblemSummary;
import com.algoprep.aireview.domain.model.RecentSolutionReview;
import com.algoprep.aireview.domain.ports.out.SolutionReviewRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolutionReviewReadHandlersTest {
  private final ProblemBankStatsPort problems = mock(ProblemBankStatsPort.class);
  private final SolutionReviewRepository repository = mock(SolutionReviewRepository.class);
  private final SolutionReviewReadHandlers handlers =
      new SolutionReviewReadHandlers(repository, problems);
  private final UUID user = UUID.randomUUID();

  @Test
  void readsAreScopedToTheCallerId() {
    UUID problem = UUID.randomUUID();
    var row = new RecentSolutionReview(UUID.randomUUID(), UUID.randomUUID(), problem, "{}", null);
    when(repository.distinctProblemIds(user)).thenReturn(List.of(problem));
    when(repository.latestForProblem(user, problem)).thenReturn(Optional.of(row));
    when(repository.recent(user, 3)).thenReturn(List.of(row));
    assertThat(handlers.list(user)).containsExactly(problem);
    assertThat(handlers.get(user, problem)).contains(row);
    assertThat(handlers.recent(user, 3)).containsExactly(row);
  }

  @Test
  void limitOutsideOneToTenIsRejectedBeforeTheQuery() {
    assertThatThrownBy(() -> handlers.recent(user, 0))
        .isInstanceOfSatisfying(
            AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-V02"));
    assertThatThrownBy(() -> handlers.recent(user, 11))
        .isInstanceOf(AiInvalidRequestException.class);
    verifyNoInteractions(repository);
    handlers.recent(user, 10);
    handlers.recent(user, 1);
    verify(repository).recent(user, 10);
    verify(repository).recent(user, 1);
  }

  private RecentSolutionReview row(UUID problem) {
    return new RecentSolutionReview(UUID.randomUUID(), UUID.randomUUID(), problem, "{}", null);
  }

  @Test
  void recentTitlesComeFromOneCallCoveringAllProblems() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID unknown = UUID.randomUUID();
    when(repository.recent(user, 3)).thenReturn(List.of(row(a), row(b), row(a), row(unknown)));
    when(problems.problemSummaries(Set.of(a, b, unknown)))
        .thenReturn(
            List.of(
                new ProblemSummary(a, "Two Sum", "EASY", "Easy"),
                new ProblemSummary(b, "Withdrawn One", "HARD", "Hard")));
    var result = handlers.recent(user, 3);
    assertThat(result)
        .extracting(RecentSolutionReview::problemTitle)
        .containsExactly("Two Sum", "Withdrawn One", "Two Sum", null);
    verify(problems).problemSummaries(Set.of(a, b, unknown));
  }

  @Test
  void aFailingPortOnlyCostsTheTitles() {
    UUID a = UUID.randomUUID();
    when(repository.recent(user, 3)).thenReturn(List.of(row(a)));
    when(problems.problemSummaries(Set.of(a))).thenThrow(new IllegalStateException("down"));
    var result = handlers.recent(user, 3);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).problemTitle()).isNull();
    assertThat(result.get(0).problemId()).isEqualTo(a);
  }

  @Test
  void noRowsMeansNoPortCall() {
    when(repository.recent(user, 3)).thenReturn(List.of());
    assertThat(handlers.recent(user, 3)).isEmpty();
    verifyNoInteractions(problems);
  }
}
