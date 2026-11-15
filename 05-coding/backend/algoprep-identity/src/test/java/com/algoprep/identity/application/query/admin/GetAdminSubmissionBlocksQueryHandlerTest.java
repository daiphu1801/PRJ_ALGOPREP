package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.out.JudgeStatsPort;
import com.algoprep.identity.application.ports.out.ProblemBankStatsPort;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Difficulty;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelRef;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelScope;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.ProblemCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.PublishedProblem;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.TopProblem;
import com.algoprep.identity.domain.model.StatSummary;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** DD api F1 section 0.8.5: how identity scopes the judge counts and what a failure costs. */
class GetAdminSubmissionBlocksQueryHandlerTest {
  private static final UUID EASY = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
  private static final UUID HARD = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

  private final JudgeStatsPort judge = mock(JudgeStatsPort.class);
  private final ProblemBankStatsPort problems = mock(ProblemBankStatsPort.class);
  private final GetAdminSubmissionBlocksQueryHandler handler =
      new GetAdminSubmissionBlocksQueryHandler(judge, problems);
  private final UUID first = UUID.randomUUID();
  private final UUID second = UUID.randomUUID();

  @Test
  void theSummaryIsPassedThroughUntouched() {
    var summary = StatSummary.of(10, 5, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L));
    when(judge.summary()).thenReturn(summary);

    assertThat(handler.submissionsSummary()).isEqualTo(summary);
  }

  @Test
  void theJudgeIsScopedByLevelInCatalogueOrderAndALevelWithNoProblemIsStillAsked() {
    when(problems.levelCatalog())
        .thenReturn(
            List.of(new LevelRef(EASY, "easy", "Easy"), new LevelRef(HARD, "hard", "Hard")));
    when(problems.publishedProblems()).thenReturn(List.of(new PublishedProblem(first, "A", EASY)));
    when(judge.byDifficulty(any()))
        .thenReturn(List.of(new LevelCount(EASY, 7, 3), new LevelCount(HARD, 0, 0)));

    Difficulty result = handler.byDifficulty();

    verify(judge)
        .byDifficulty(List.of(new LevelScope(EASY, Set.of(first)), new LevelScope(HARD, Set.of())));
    assertThat(result.groups()).extracting("levelCode").containsExactly("easy", "hard");
    assertThat(result.groups().getFirst().submissions()).isEqualTo(7);
    assertThat(result.groups().getFirst().accepted()).isEqualTo(3);
    assertThat(result.legend()).containsExactly("totalSubmissions", "acceptedSubmissions");
  }

  @Test
  void aLevelTheJudgeDidNotAnswerForShowsZeroNotAGap() {
    when(problems.levelCatalog()).thenReturn(List.of(new LevelRef(EASY, "easy", "Easy")));
    when(problems.publishedProblems()).thenReturn(List.of());
    when(judge.byDifficulty(any())).thenReturn(List.of());

    assertThat(handler.byDifficulty().groups()).hasSize(1);
    assertThat(handler.byDifficulty().groups().getFirst().submissions()).isZero();
  }

  @Test
  void theTopListIsScopedToPublishedProblemsAndTitledFromTheCatalogue() {
    when(problems.publishedProblems())
        .thenReturn(
            List.of(
                new PublishedProblem(first, "Two Sum", EASY),
                new PublishedProblem(second, "LRU", HARD)));
    when(judge.topProblems(Set.of(first, second)))
        .thenReturn(List.of(new ProblemCount(second, 9), new ProblemCount(first, 4)));

    assertThat(handler.topProblems().items())
        .containsExactly(new TopProblem(second, "LRU", 9), new TopProblem(first, "Two Sum", 4));
  }

  @Test
  void aProblemTheJudgeReturnsButTheCatalogueDoesNotKnowIsLeftOut() {
    when(problems.publishedProblems())
        .thenReturn(List.of(new PublishedProblem(first, "Two Sum", EASY)));
    when(judge.topProblems(Set.of(first)))
        .thenReturn(List.of(new ProblemCount(second, 9), new ProblemCount(first, 4)));

    assertThat(handler.topProblems().items()).containsExactly(new TopProblem(first, "Two Sum", 4));
  }

  @Test
  void aFailingSourceIsA503ForThatBlockWithTheDashboardCodeAndNoCause() {
    when(judge.byDay()).thenThrow(new IllegalStateException("db down"));
    when(problems.levelCatalog()).thenThrow(new IllegalStateException("down"));

    assertThatThrownBy(handler::byDay)
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class,
            e -> {
              assertThat(e.errorCode()).isEqualTo("IDT-506");
              assertThat(e.getCause()).isNull();
            });
    assertThatThrownBy(handler::byDifficulty).isInstanceOf(ServiceUnavailableException.class);
  }
}
