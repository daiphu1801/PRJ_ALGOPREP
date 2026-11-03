package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.model.AdminSubmissionStats.LevelProblems;
import com.algoprep.judge.domain.model.AdminSubmissionStats.Totals;
import com.algoprep.judge.domain.ports.out.AdminSubmissionStatsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAdminSubmissionStatsQueryHandlerTest {
  private final AdminSubmissionStatsRepository repository =
      mock(AdminSubmissionStatsRepository.class);
  private final GetAdminSubmissionStatsQueryHandler handler =
      new GetAdminSubmissionStatsQueryHandler(repository);
  private final Instant now = Instant.parse("2026-11-04T00:00:00Z");

  @Test
  void brokenVerdictBlockDoesNotExecuteOrInvalidateAnotherAggregation() {
    when(repository.now()).thenReturn(now);
    when(repository.verdicts(now)).thenThrow(new IllegalStateException("private SQL password"));
    assertThatThrownBy(handler::verdicts)
        .hasMessage("Admin submission statistics are temporarily unavailable")
        .hasNoCause();
    verify(repository, never()).totals(any());
    when(repository.daily(any(), any())).thenReturn(List.of());
    when(repository.totals(now)).thenReturn(new Totals(4, 4, 0));
    assertThat(handler.summary().value()).isEqualTo(4);
    assertThat(handler.summary().sparklineSeries()).hasSize(7);
  }

  @Test
  void languageDayAndMonthEachUseTheirOwnWindowAndReturnCompleteShapes() {
    when(repository.now()).thenReturn(now);
    when(repository.languages(any(), any())).thenReturn(List.of());
    when(repository.daily(any(), any())).thenReturn(List.of());
    when(repository.months(any(), any())).thenReturn(List.of());
    when(repository.verdicts(now)).thenReturn(Map.of());
    assertThat(handler.byLanguage().series()).hasSize(3);
    assertThat(handler.byDay().columns()).hasSize(7);
    assertThat(handler.byMonth().points()).hasSize(6);
    assertThat(handler.verdicts().slices()).hasSize(5);
    verify(repository).languages(Instant.parse("2026-10-15T17:00:00Z"), now);
    verify(repository).daily(Instant.parse("2026-10-28T17:00:00Z"), now);
    verify(repository).months(Instant.parse("2026-05-31T17:00:00Z"), now);
    verify(repository, never()).totals(any());
  }

  @Test
  void difficultyAndTopReceiveOnlyTheSuppliedPublishedProblemScope() {
    when(repository.now()).thenReturn(now);
    UUID problem = UUID.randomUUID();
    UUID level = UUID.randomUUID();
    when(repository.problems(Set.of(problem), now)).thenReturn(List.of());
    assertThat(
            handler
                .byDifficulty(List.of(new LevelProblems(level, Set.of(problem))))
                .groups()
                .getFirst()
                .submissions())
        .isZero();
    assertThat(handler.topProblems(Set.of(problem)).items()).isEmpty();
    org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2))
        .problems(Set.of(problem), now);
    verify(repository, never()).totals(any());
  }
}
